// SPDX-License-Identifier: MIT

package sylect.bootstrap;

import sylect.SylectBaseListener;
import sylect.SylectParser;
import sylect.SylectParser.AssignmentStatementContext;
import sylect.SylectParser.ClassDefinitionContext;
import sylect.SylectParser.ConditionalStatementContext;
import sylect.SylectParser.ElseBranchContext;
import sylect.SylectParser.ExpressionStatementContext;
import sylect.SylectParser.FieldDefinitionContext;
import sylect.SylectParser.LoopStatementContext;
import sylect.SylectParser.MethodDefinitionContext;
import sylect.SylectParser.ProgramContext;
import sylect.SylectParser.ReturnStatementContext;
import sylect.bootstrap.metadata.ClassMeta;
import sylect.bootstrap.metadata.FieldMeta;
import sylect.bootstrap.metadata.LocalMeta;
import sylect.bootstrap.metadata.MethodMeta;
import sylect.bootstrap.metadata.TypeMeta;
import sylect.bootstrap.metadata.TypeMeta.Kind;
import sylect.bootstrap.util.ClassUtils;
import sylect.bootstrap.util.LoopContext;
import sylect.bootstrap.util.Pair;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Stack;

public class BytecodeTargetListener extends SylectBaseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BytecodeTargetListener.class);

    private final int target;
    private final ScopeManager scopeManager;

    private final ClassWriter cw;
    private final AnnotationCompiler annotationCompiler;

    private MethodMeta methodMeta;
    private MethodVisitor mv;
    private Label methodStart;
    private Label methodEnd;

    private final Stack<Pair<Label, Label>> conditionalBlocks;
    private final Stack<LoopContext> loopBlocks;

    public BytecodeTargetListener(int target, ScopeManager scopeManager) {
        this.target = target;
        this.scopeManager = Objects.requireNonNull(scopeManager, "scopeManager");

        this.cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        this.annotationCompiler = new AnnotationCompiler(scopeManager);

        this.conditionalBlocks = new Stack<>();
        this.loopBlocks = new Stack<>();
    }

    @Override
    public void enterProgram(ProgramContext ctx) {
        LOGGER.debug("program start");
        scopeManager.enterSource(ctx);
    }

    @Override
    public void enterClassDefinition(ClassDefinitionContext ctx) {
        var classMeta = scopeManager.enterClass(ctx);
        LOGGER.debug("class definition: {}", classMeta);

        cw.visit(ClassUtils.getVersion(target),
                Opcodes.ACC_PUBLIC +
                        (classMeta.iface() ? Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT : Opcodes.ACC_SUPER),
                ClassMeta.javaClassFromClassName(classMeta.name()),
                null,
                ClassMeta.javaClassFromClassName(classMeta.baseClassName()),
                classMeta.interfaces().stream()
                        .map(ClassMeta::javaClassFromClassName)
                        .toArray(String[]::new));
        annotationCompiler.visitAnnotationBlock(ctx.annotationBlock(), desc -> cw.visitAnnotation(desc, true));
    }

    @Override
    public void enterFieldDefinition(FieldDefinitionContext ctx) {
        var fieldMeta = FieldMeta.fromContext(scopeManager, ctx);
        LOGGER.debug("field definition: {}", fieldMeta);

        var fv = cw.visitField(
                Opcodes.ACC_PROTECTED + (fieldMeta.isStatic() ? Opcodes.ACC_STATIC : 0),
                fieldMeta.name(),
                fieldMeta.asDescriptor(),
                null,
                null);
        annotationCompiler.visitAnnotationBlock(ctx.annotationBlock(), desc -> fv.visitAnnotation(desc, true));
    }

    @Override
    public void enterMethodDefinition(MethodDefinitionContext ctx) {
        methodMeta = scopeManager.enterMethod(ctx);
        LOGGER.debug("method definition start: {}", methodMeta);

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC +
                        (methodMeta.isStatic() ? Opcodes.ACC_STATIC : 0) +
                        (methodMeta.isAbstract() ? Opcodes.ACC_ABSTRACT : 0),
                methodMeta.name(),
                methodMeta.asDescriptor(),
                null,
                null);

        annotationCompiler.visitAnnotationBlock(ctx.annotationBlock(), desc -> mv.visitAnnotation(desc, true));
        for (int i = 0; i < ctx.parameter().size(); i++) {
            int index = i; // Lambda needs a final variable
            annotationCompiler.visitAnnotationBlock(
                    ctx.parameter(i).annotationBlock(),
                    desc -> mv.visitParameterAnnotation(index, desc, true));
        }

        methodStart = new Label();
        methodEnd = new Label();
        mv.visitLabel(methodStart);

        scopeManager.forEachLocal(this::visitLocalVariable);

        // TODO: Proper constructor calls
        if ("<init>".equals(methodMeta.name())) {
            var classMeta = scopeManager.getClassMeta();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    ClassMeta.javaClassFromClassName(classMeta.baseClassName()),
                    "<init>", "()V", false);
        }
    }

    @Override
    public void enterVariableDefinitionStatement(SylectParser.VariableDefinitionStatementContext ctx) {
        for (int i = 0; i < ctx.IDENTIFIER().size(); i++) {
            var localMeta = scopeManager.addLocal(
                    ctx.IDENTIFIER().get(i).getText(),
                    TypeMeta.fromContext(scopeManager, ctx.type(i)));
            visitLocalVariable(localMeta);
            LOGGER.debug("variable definition: {}", localMeta);

            if (ctx.expression(i) != null) {
                assignLocalVariable(localMeta, ctx.expression(i));
            }
        }
    }

    @Override
    public void enterAssignmentStatement(AssignmentStatementContext ctx) {
        var name = ctx.IDENTIFIER().getText();

        var localMeta = scopeManager.getLocal(name);
        if (localMeta != null) {
            LOGGER.debug("assignment statement: {} to {}", ctx.expression().getText(), localMeta);
            assignLocalVariable(localMeta, ctx.expression());
            return;
        }

        var fieldMeta = scopeManager.getField(name);
        if (fieldMeta != null) {
            LOGGER.debug("assignment statement: {} to {}", ctx.expression().getText(), fieldMeta);
            assignField(fieldMeta, ctx.expression());
            return;
        }

        throw new CompilationException("unknown local/field to assign: " + name);
    }

    @Override
    public void enterExpressionStatement(ExpressionStatementContext ctx) {
        LOGGER.debug("expression statement: {}", ctx.expression().getText());

        var expressionType = new ExpressionCompiler(scopeManager, mv).compile(ctx.expression());
        switch (expressionType.getLocalSize()) {
            case 1 -> mv.visitInsn(Opcodes.POP);
            case 2 -> mv.visitInsn(Opcodes.POP2);
        }
    }

    @Override
    public void enterConditionalStatement(ConditionalStatementContext ctx) {
        LOGGER.debug("conditional statement start: {}", ctx.expression().getText());

        var expressionType = new ExpressionCompiler(scopeManager, mv).compile(ctx.expression());
        if (expressionType.kind() != Kind.INTEGER) {
            throw new CompilationException("expected integer return type: " + ctx.expression().getText());
        }

        var elseBranch = new Label();
        var otherCode = new Label();

        if (ctx.elseBranch() == null) {
            mv.visitJumpInsn(Opcodes.IFEQ, otherCode);
            conditionalBlocks.push(new Pair<>(null, otherCode));
        } else {
            mv.visitJumpInsn(Opcodes.IFEQ, elseBranch);
            conditionalBlocks.push(new Pair<>(elseBranch, otherCode));
        }
    }

    @Override
    public void enterElseBranch(ElseBranchContext ctx) {
        LOGGER.debug("conditional statement: else");

        var conditionalBlock = conditionalBlocks.peek();
        mv.visitJumpInsn(Opcodes.GOTO, conditionalBlock.right());
        mv.visitLabel(conditionalBlock.left());
    }

    @Override
    public void exitConditionalStatement(ConditionalStatementContext ctx) {
        LOGGER.debug("conditional statement end: {}", ctx.expression().getText());

        var conditionalBlock = conditionalBlocks.pop();
        mv.visitLabel(conditionalBlock.right());
    }

    @Override
    public void enterLoopStatement(LoopStatementContext ctx) {
        LOGGER.debug("loop statement start: {}", ctx.expression().getText());

        var loopStart = new Label();
        var otherCode = new Label();
        var eachBlock = ctx.eachBlock() == null ? otherCode : new Label();

        mv.visitLabel(loopStart);
        loopBlocks.push(new LoopContext(loopStart, eachBlock, otherCode));

        var expressionType = new ExpressionCompiler(scopeManager, mv).compile(ctx.expression());
        if (expressionType.kind() != Kind.INTEGER) {
            throw new CompilationException("expected integer return type: " + ctx.expression().getText());
        }

        mv.visitJumpInsn(Opcodes.IFEQ, otherCode);
    }

    @Override
    public void enterBreakContinueStatement(SylectParser.BreakContinueStatementContext ctx) {
        LOGGER.debug("break/continue statement: {}", ctx.getText());

        if (loopBlocks.empty()) {
            throw new CompilationException("break/continue should be inside loop");
        }
        var currentLoop = loopBlocks.peek();

        if ("break".equals(ctx.getText())) {
            mv.visitJumpInsn(Opcodes.GOTO, currentLoop.otherCode());
        } else {
            mv.visitJumpInsn(Opcodes.GOTO, currentLoop.eachBlock());
        }
    }

    @Override
    public void enterEachBlock(SylectParser.EachBlockContext ctx) {
        LOGGER.debug("loop statement each: {}", ctx.codeBlock().getText());
        mv.visitLabel(loopBlocks.peek().eachBlock());
    }

    @Override
    public void exitLoopStatement(LoopStatementContext ctx) {
        LOGGER.debug("loop statement end: {}", ctx.expression().getText());

        var loopBlock = loopBlocks.pop();
        mv.visitJumpInsn(Opcodes.GOTO, loopBlock.loopStart());
        mv.visitLabel(loopBlock.otherCode());
    }

    @Override
    public void enterReturnStatement(ReturnStatementContext ctx) {
        var expressionType = ctx.expression() == null ? new TypeMeta(Kind.VOID, false, null)
                : new ExpressionCompiler(scopeManager, mv).compile(ctx.expression());
        LOGGER.debug("return statement: {} from {}", expressionType, methodMeta.returnType());

        if (!methodMeta.returnType().equals(expressionType)) {
            throw new CompilationException("cannot return " + expressionType + " as " + methodMeta.returnType());
        }

        if (expressionType.isArray()) {
            mv.visitInsn(Opcodes.ARETURN);
            return;
        }

        switch (expressionType.kind()) {
            case VOID -> mv.visitInsn(Opcodes.RETURN);

            case INTEGER -> mv.visitInsn(Opcodes.IRETURN);
            case LONG -> mv.visitInsn(Opcodes.LRETURN);
            case FLOAT -> mv.visitInsn(Opcodes.FRETURN);
            case DOUBLE -> mv.visitInsn(Opcodes.DRETURN);
            case CLASS -> mv.visitInsn(Opcodes.ARETURN);

            case BOOLEAN, BYTE, CHAR, SHORT -> mv.visitInsn(Opcodes.IRETURN);

            default -> throw new CompilationException("unsupported return type: " + expressionType);
        }
    }

    @Override
    public void exitMethodDefinition(MethodDefinitionContext ctx) {
        LOGGER.debug("method definition end: {}", methodMeta);

        // Guard to protect from lack of return statement
        if (!methodMeta.isAbstract()) {
            if (methodMeta.returnType().kind() == Kind.VOID) {
                mv.visitInsn(Opcodes.RETURN);
            } else {
                // TODO: Analyze code to determine whether there are code paths without return
                mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn("No return statement");
                mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL, "java/lang/RuntimeException",
                        "<init>", "(Ljava/lang/String;)V", false);
                mv.visitInsn(Opcodes.ATHROW);
            }
        }

        mv.visitLabel(methodEnd);

        // Both parameters are calculated with ASM
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    @Override
    public void exitProgram(ProgramContext ctx) {
        LOGGER.debug("program end");
        cw.visitEnd();
    }

    public byte[] getBytecode() {
        return cw.toByteArray();
    }

    private void visitLocalVariable(LocalMeta localMeta) {
        mv.visitLocalVariable(
                localMeta.name(),
                localMeta.type().asDescriptor(), null,
                methodStart, methodEnd,
                localMeta.offset());
    }

    private void assignLocalVariable(LocalMeta localMeta, SylectParser.ExpressionContext ctx) {
        var expressionType = new ExpressionCompiler(scopeManager, mv).compile(ctx);
        if (!expressionType.equals(localMeta.type())) {
            throw new CompilationException("cannot assign " + expressionType + " to " + localMeta.type());
        }

        switch (expressionType.kind()) {
            case INTEGER -> mv.visitIntInsn(Opcodes.ISTORE, localMeta.offset());
            case LONG -> mv.visitIntInsn(Opcodes.LSTORE, localMeta.offset());
            case FLOAT -> mv.visitIntInsn(Opcodes.FSTORE, localMeta.offset());
            case DOUBLE -> mv.visitIntInsn(Opcodes.DSTORE, localMeta.offset());
            case CLASS -> mv.visitIntInsn(Opcodes.ASTORE, localMeta.offset());
            default -> throw new CompilationException("unsupported assignment type: " + expressionType);
        }
    }

    private void assignField(FieldMeta fieldMeta, SylectParser.ExpressionContext ctx) {
        if (methodMeta.isStatic() && !fieldMeta.isStatic()) {
            throw new CompilationException("could not assign non-static field in static method: " + fieldMeta.name());
        }

        if (!fieldMeta.isStatic()) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
        }

        var expressionType = new ExpressionCompiler(scopeManager, mv).compile(ctx);
        if (!expressionType.equals(fieldMeta.type())) {
            throw new CompilationException("cannot assign " + expressionType + " to " + fieldMeta.type());
        }

        var owner = ClassMeta.javaClassFromClassName(scopeManager.getClassMeta().name());
        mv.visitFieldInsn(
                fieldMeta.isStatic() ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD,
                owner, fieldMeta.name(), fieldMeta.asDescriptor());
    }
}
