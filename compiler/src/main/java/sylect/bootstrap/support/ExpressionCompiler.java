// SPDX-License-Identifier: MIT

package sylect.bootstrap.support;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import sylect.CompilationException;
import sylect.SylectParser;
import sylect.bootstrap.ScopeManager;
import sylect.bootstrap.metadata.TypeMeta;

import java.util.Objects;

public class ExpressionCompiler {
    private static final TypeMeta BOOLEAN_PSEUDO_TYPE = new TypeMeta(TypeMeta.Kind.INTEGER, false, null);

    private final ScopeManager scopeManager;
    private final MethodVisitor mv;

    public ExpressionCompiler(ScopeManager scopeManager, MethodVisitor mv) {
        this.scopeManager = Objects.requireNonNull(scopeManager);
        this.mv = Objects.requireNonNull(mv);
    }

    public TypeMeta compile(SylectParser.ExpressionContext ctx) {
        // If there's only one term - compile and return it as-is
        if (ctx.andExpression().size() == 1) {
            return compileAndExpression(ctx.andExpression(0));
        }

        var trueLabel = new Label();
        var otherCodeLabel = new Label();

        for (var andExpression : ctx.andExpression()) {
            var typeMeta = compileAndExpression(andExpression);
            if (!BOOLEAN_PSEUDO_TYPE.equals(typeMeta)) {
                throw new CompilationException("boolean expression term should evaluate to integer");
            }

            // Short-circuit to end if not zero (like in C)
            mv.visitJumpInsn(Opcodes.IFNE, trueLabel);
        }

        // If expression is still false
        mv.visitLdcInsn(0);
        mv.visitJumpInsn(Opcodes.GOTO, otherCodeLabel);

        mv.visitLabel(trueLabel);
        mv.visitLdcInsn(1);

        mv.visitLabel(otherCodeLabel);

        return BOOLEAN_PSEUDO_TYPE;
    }

    private TypeMeta compileAndExpression(SylectParser.AndExpressionContext ctx) {
        // If there's only one term - compile and return it as-is
        if (ctx.mathExpression().size() == 1) {
            return new MathExpressionCompiler(scopeManager, mv).compile(ctx.mathExpression(0));
        }

        var falseLabel = new Label();
        var otherCodeLabel = new Label();

        for (var mathExpression : ctx.mathExpression()) {
            var typeMeta = new MathExpressionCompiler(scopeManager, mv).compile(mathExpression);
            if (!BOOLEAN_PSEUDO_TYPE.equals(typeMeta)) {
                throw new CompilationException("boolean expression term should evaluate to integer");
            }

            // Short-circuit to end
            mv.visitJumpInsn(Opcodes.IFEQ, falseLabel);
        }

        // If expression is still true
        mv.visitLdcInsn(1);
        mv.visitJumpInsn(Opcodes.GOTO, otherCodeLabel);

        mv.visitLabel(falseLabel);
        mv.visitLdcInsn(0);

        mv.visitLabel(otherCodeLabel);

        return BOOLEAN_PSEUDO_TYPE;
    }
}
