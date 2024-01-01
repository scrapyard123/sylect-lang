// SPDX-License-Identifier: MIT

package forward.bootstrap;

import forward.ForwardBaseListener;
import forward.ForwardParser;
import forward.ForwardParser.AssignmentStatementContext;
import forward.ForwardParser.ClassDefinitionContext;
import forward.ForwardParser.ConditionalStatementContext;
import forward.ForwardParser.ElseBranchContext;
import forward.ForwardParser.ExpressionStatementContext;
import forward.ForwardParser.FieldDefinitionContext;
import forward.ForwardParser.LoopStatementContext;
import forward.ForwardParser.MethodDefinitionContext;
import forward.ForwardParser.ProgramContext;
import forward.ForwardParser.ReturnStatementContext;
import forward.ForwardParser.VariableDefinitionContext;
import forward.bootstrap.metadata.ClassMeta;
import forward.bootstrap.metadata.FieldMeta;
import forward.bootstrap.metadata.LocalMeta;
import forward.bootstrap.metadata.MethodMeta;
import forward.bootstrap.metadata.TypeMeta;
import forward.bootstrap.metadata.TypeMeta.Kind;
import forward.bootstrap.util.Pair;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Stack;
import java.util.function.Function;

public class BytecodeTargetListener extends ForwardBaseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BytecodeTargetListener.class);

    private final int target;

    private final ScopeManager scopeManager;
    private final ClassWriter cw;

    private final Stack<Pair<Label, Label>> conditionalBlocks;
    private final Stack<Pair<Label, Label>> loopBlocks;

    private MethodMeta methodMeta;
    private MethodVisitor mv;
    private Label methodStart;
    private Label methodEnd;

    public BytecodeTargetListener(int target, ScopeManager scopeManager) {
        this.target = target;
        this.scopeManager = Objects.requireNonNull(scopeManager, "scopeManager");

        this.cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
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
        // TODO: Actual interface list
        var interfaces = (String[]) null;
        LOGGER.debug("class definition: {}", classMeta);

        cw.visit(getVersion(),
                Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                ClassMeta.javaClassFromClassName(classMeta.name()),
                null,
                ClassMeta.javaClassFromClassName(classMeta.baseClassName()),
                interfaces);
        visitAnnotationDefinition(ctx.annotationDefinition(), desc -> cw.visitAnnotation(desc, true));
    }

    @Override
    public void enterFieldDefinition(FieldDefinitionContext ctx) {
        var fieldMeta = FieldMeta.fromContext(scopeManager, ctx);
        LOGGER.debug("field definition: {}", fieldMeta);

        cw.visitField(
                Opcodes.ACC_PUBLIC + (fieldMeta.isStatic() ? Opcodes.ACC_STATIC : 0),
                fieldMeta.name(),
                fieldMeta.asDescriptor(),
                null,
                null);
    }

    @Override
    public void enterMethodDefinition(MethodDefinitionContext ctx) {
        methodMeta = scopeManager.enterMethod(ctx);
        LOGGER.debug("method definition start: {}", methodMeta);

        mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC + (methodMeta.isStatic() ? Opcodes.ACC_STATIC : 0),
                methodMeta.name(),
                methodMeta.asDescriptor(),
                null,
                null);
        visitAnnotationDefinition(ctx.annotationDefinition(), desc -> mv.visitAnnotation(desc, true));

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
    public void enterVariableDefinition(VariableDefinitionContext ctx) {
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

        if (localMeta == null) {
            // TODO: Support field assignment
            throw new CompilationException("field assignment is not supported: " + name);
        }
        LOGGER.debug("assignment statement: {} to {}", ctx.expression().getText(), localMeta);

        assignLocalVariable(localMeta, ctx.expression());
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

        mv.visitLabel(loopStart);
        loopBlocks.push(new Pair<>(loopStart, otherCode));

        var expressionType = new ExpressionCompiler(scopeManager, mv).compile(ctx.expression());
        if (expressionType.kind() != Kind.INTEGER) {
            throw new CompilationException("expected integer return type: " + ctx.expression().getText());
        }

        mv.visitJumpInsn(Opcodes.IFEQ, otherCode);
    }

    @Override
    public void exitLoopStatement(LoopStatementContext ctx) {
        LOGGER.debug("loop statement end: {}", ctx.expression().getText());

        var loopBlock = loopBlocks.pop();
        mv.visitJumpInsn(Opcodes.GOTO, loopBlock.left());
        mv.visitLabel(loopBlock.right());
    }

    @Override
    public void enterReturnStatement(ReturnStatementContext ctx) {
        var expressionType = ctx.expression() == null ? new TypeMeta(Kind.VOID, false, null)
                : new ExpressionCompiler(scopeManager, mv).compile(ctx.expression());
        LOGGER.debug("return statement: {} from {}", expressionType, methodMeta.returnType());

        if (!methodMeta.returnType().equals(expressionType)) {
            throw new CompilationException("cannot return " + expressionType + " as " + methodMeta.returnType());
        }

        switch (expressionType.kind()) {
            case VOID -> mv.visitInsn(Opcodes.RETURN);
            case INTEGER -> mv.visitInsn(Opcodes.IRETURN);
            case LONG -> mv.visitInsn(Opcodes.LRETURN);
            case FLOAT -> mv.visitInsn(Opcodes.FRETURN);
            case DOUBLE -> mv.visitInsn(Opcodes.DRETURN);
            case CLASS -> mv.visitInsn(Opcodes.ARETURN);
            default -> throw new CompilationException("unsupported return type: " + expressionType);
        }
    }

    @Override
    public void exitMethodDefinition(MethodDefinitionContext ctx) {
        LOGGER.debug("method definition end: {}", methodMeta);

        // Guard to protect from the lack of return statement
        // TODO: Devise some sort of analysis to determine code paths without return
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("No return statement");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException",
                "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);

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

    private void visitAnnotationDefinition(
            ForwardParser.AnnotationDefinitionContext ctx,
            Function<String, AnnotationVisitor> visitorGenerator) {
        if (ctx == null) {
            return;
        }

        for (var type : ctx.type()) {
            var typeMeta = TypeMeta.fromContext(scopeManager, type);
            var visitor = visitorGenerator.apply(typeMeta.asDescriptor());
            visitor.visitEnd();
        }
    }

    private void visitLocalVariable(LocalMeta localMeta) {
        mv.visitLocalVariable(
                localMeta.name(),
                localMeta.type().asDescriptor(), null,
                methodStart, methodEnd,
                localMeta.offset());
    }

    private void assignLocalVariable(LocalMeta localMeta, ForwardParser.ExpressionContext ctx) {
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

    private int getVersion() {
        return switch (target) {
            case 5 -> Opcodes.V1_5;
            case 6 -> Opcodes.V1_6;
            case 7 -> Opcodes.V1_7;
            case 8 -> Opcodes.V1_8;
            case 9 -> Opcodes.V9;
            case 10 -> Opcodes.V10;
            case 11 -> Opcodes.V11;
            case 12 -> Opcodes.V12;
            case 13 -> Opcodes.V13;
            case 14 -> Opcodes.V14;
            case 15 -> Opcodes.V15;
            case 16 -> Opcodes.V16;
            case 17 -> Opcodes.V17;
            case 18 -> Opcodes.V18;
            case 19 -> Opcodes.V19;
            default -> throw new CompilationException("unsupported target: " + target);
        };
    }
}
