// SPDX-License-Identifier: MIT

package sylect.bootstrap.support;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import sylect.CompilationException;
import sylect.SylectParser;
import sylect.SylectParser.MathExpressionContext;
import sylect.SylectParser.MathTermContext;
import sylect.bootstrap.ScopeManager;
import sylect.bootstrap.metadata.TypeMeta;
import sylect.bootstrap.metadata.TypeMeta.Kind;
import sylect.bootstrap.metadata.expression.OperatorMeta;
import sylect.bootstrap.metadata.expression.UnaryOperatorMeta;
import sylect.bootstrap.util.ClassUtils;

import java.util.Objects;
import java.util.Stack;

public class MathExpressionCompiler {
    private final ScopeManager scopeManager;
    private final MethodVisitor mv;

    private final Stack<TypeMeta> operands;
    private final Stack<OperatorMeta> operators;

    public MathExpressionCompiler(ScopeManager scopeManager, MethodVisitor mv) {
        this.scopeManager = Objects.requireNonNull(scopeManager);
        this.mv = Objects.requireNonNull(mv);

        this.operands = new Stack<>();
        this.operators = new Stack<>();
    }

    public TypeMeta compile(MathExpressionContext ctx) {
        // Shunting yard algorithm
        var termContexts = ctx.mathTerm();
        var operatorContexts = ctx.operator();

        for (int i = 0; i < termContexts.size(); i++) {
            operands.add(compileTerm(termContexts.get(i)));
            if (i >= operatorContexts.size()) {
                continue;
            }

            var operatorMeta = OperatorMeta.fromContext(operatorContexts.get(i));
            while (!operators.isEmpty() && operators.peek().comparePrecedence(operatorMeta) >= 0) {
                var topOperator = operators.pop();
                compileOperator(topOperator);
            }
            operators.push(operatorMeta);
        }
        while (!operators.isEmpty()) {
            compileOperator(operators.pop());
        }

        if (operands.size() == 1) {
            return operands.pop();
        } else {
            throw new CompilationException("failed to compile expression: " + ctx.getText());
        }
    }

    private TypeMeta compileTerm(MathTermContext ctx) {
        var operandType = (TypeMeta) null;

        if (ctx.LITERAL() != null) {
            operandType = ClassUtils.visitLiteral(ctx.LITERAL(), mv::visitLdcInsn);
        }

        if (ctx.objectExpression() != null) {
            operandType = new ObjectExpressionCompiler(scopeManager, mv).compile(ctx.objectExpression());
        }

        if (ctx.expression() != null) {
            operandType = new ExpressionCompiler(scopeManager, mv).compile(ctx.expression());
        }

        if (ctx.unaryOperator() != null) {
            for (var unaryOperatorContext : ctx.unaryOperator()) {
                operandType = compileUnaryOperator(unaryOperatorContext, operandType);
            }
        }

        if (operandType == null) {
            throw new CompilationException("failed to compile term: " + ctx.getText());
        } else {
            return operandType;
        }
    }

    private TypeMeta compileUnaryOperator(SylectParser.UnaryOperatorContext ctx, TypeMeta operandType) {
        var unaryOp = UnaryOperatorMeta.fromContext(ctx);
        return switch (unaryOp) {
            case MINUS -> {
                switch (operandType.kind()) {
                    case INTEGER -> mv.visitInsn(Opcodes.INEG);
                    case LONG -> mv.visitInsn(Opcodes.LNEG);
                    case FLOAT -> mv.visitInsn(Opcodes.FNEG);
                    case DOUBLE -> mv.visitInsn(Opcodes.DNEG);
                    default -> throw new CompilationException("could not negate: " + operandType);
                }
                yield operandType;
            }

            case NOT -> {
                if (operandType.kind() != Kind.INTEGER) {
                    throw new CompilationException("NOT operator works only on integers: " + operandType);
                }

                var whenZero = new Label();
                var otherCode = new Label();

                // If stack top is zero - place 1, otherwise place 0
                mv.visitJumpInsn(Opcodes.IFEQ, whenZero);
                mv.visitLdcInsn(0);
                mv.visitJumpInsn(Opcodes.GOTO, otherCode);

                mv.visitLabel(whenZero);
                mv.visitLdcInsn(1);

                mv.visitLabel(otherCode);

                yield operandType; // always integer
            }

            case TYPE_CONVERSION -> {
                var targetType = TypeMeta.fromContext(scopeManager, ctx.type());

                if (targetType.isBlackBoxType() || operandType.isBlackBoxType()) {
                    compileBlackBoxTypeConversion(targetType, operandType);
                } else {
                    switch (operandType.kind()) {
                        case INTEGER -> compileTypeConversion(
                                targetType, Opcodes.NOP, Opcodes.I2L, Opcodes.I2F, Opcodes.I2D);
                        case LONG -> compileTypeConversion(
                                targetType, Opcodes.L2I, Opcodes.NOP, Opcodes.L2F, Opcodes.L2D);
                        case FLOAT -> compileTypeConversion(
                                targetType, Opcodes.F2I, Opcodes.F2L, Opcodes.NOP, Opcodes.F2D);
                        case DOUBLE -> compileTypeConversion(
                                targetType, Opcodes.D2I, Opcodes.D2L, Opcodes.D2F, Opcodes.NOP);

                        case CLASS -> mv.visitTypeInsn(Opcodes.CHECKCAST, targetType.className());

                        default -> throw new CompilationException(
                                "could not convert " + operandType + " to " + targetType);
                    }
                }

                yield targetType;
            }
        };
    }

    private void compileTypeConversion(TypeMeta targetType, int toInt, int toLong, int toFloat, int toDouble) {
        switch (targetType.kind()) {
            case INTEGER -> mv.visitInsn(toInt);
            case LONG -> mv.visitInsn(toLong);
            case FLOAT -> mv.visitInsn(toFloat);
            case DOUBLE -> mv.visitInsn(toDouble);
            default -> throw new CompilationException("could not convert to: " + targetType);
        }
    }

    private void compileBlackBoxTypeConversion(TypeMeta targetType, TypeMeta operandType) {
        // We can convert between object array types
        if (operandType.isArray() && targetType.isArray() &&
                Kind.CLASS.equals(targetType.arrayElementType().kind()) &&
                Kind.CLASS.equals(operandType.arrayElementType().kind())) {
            mv.visitTypeInsn(Opcodes.CHECKCAST, targetType.asDescriptor());
            return;
        }

        // We can convert other black box types to integer by doing nothing
        if (operandType.isBlackBoxType()) {
            if (Kind.INTEGER.equals(targetType.kind())) {
                return;
            } else {
                throw new CompilationException("could not convert " + operandType + " to non-integer type");
            }
        }

        // We can convert integer to other black box types
        if (Kind.INTEGER.equals(operandType.kind()) && targetType.isBlackBoxType()) {
            switch (targetType.kind()) {
                case BOOLEAN -> { /* Just do nothing */ }
                case BYTE -> mv.visitInsn(Opcodes.I2B);
                case CHAR -> mv.visitInsn(Opcodes.I2C);
                case SHORT -> mv.visitInsn(Opcodes.I2S);
                default -> throw new CompilationException("integer could not be converted to: " + targetType);
            }
            return;
        }

        throw new CompilationException("could not convert " + operandType + " to: " + targetType);
    }

    private void compileOperator(OperatorMeta operatorMeta) {
        var right = operands.pop();
        var left = operands.pop();

        switch (operatorMeta) {
            case MULTIPLY -> fullOperator(left, right, Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL);
            case DIVIDE -> fullOperator(left, right, Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV);
            case REM -> fullOperator(left, right, Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM);

            case PLUS -> fullOperator(left, right, Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD);
            case MINUS -> fullOperator(left, right, Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB);

            case SHIFT_LEFT -> shiftOperator(left, right, Opcodes.ISHL, Opcodes.LSHL);
            case SHIFT_RIGHT -> shiftOperator(left, right, Opcodes.ISHR, Opcodes.LSHR);
            case LOGICAL_SHIFT_RIGHT -> shiftOperator(left, right, Opcodes.IUSHR, Opcodes.LUSHR);

            case LESSER, GREATER, LESSER_EQUAL, GREATER_EQUAL -> comparisonOperator(left, right, operatorMeta);
            case EQUALS, NOT_EQUALS -> comparisonOperator(left, right, operatorMeta);

            case BITWISE_AND -> intLongOperator(left, right, Opcodes.IAND, Opcodes.LAND);
            case BITWISE_XOR -> intLongOperator(left, right, Opcodes.IXOR, Opcodes.LXOR);
            case BITWISE_OR -> intLongOperator(left, right, Opcodes.IOR, Opcodes.LOR);

            default -> throw new CompilationException("unsupported operator: " + operatorMeta);
        }
    }

    private void fullOperator(TypeMeta left, TypeMeta right, int intOp, int longOp, int floatOp, int doubleOp) {
        if (!left.equals(right)) {
            throw new CompilationException("type mismatch: " + left + " (op) " + right);
        }

        switch (left.kind()) {
            case INTEGER -> mv.visitInsn(intOp);
            case LONG -> mv.visitInsn(longOp);
            case FLOAT -> mv.visitInsn(floatOp);
            case DOUBLE -> mv.visitInsn(doubleOp);
            default -> throw new CompilationException("unsupported operand type: " + left);
        }
        operands.push(left);
    }

    private void shiftOperator(TypeMeta left, TypeMeta right, int intOp, int longOp) {
        if (right.kind() != Kind.INTEGER) {
            throw new CompilationException("operand should be integer: " + right);
        }

        switch (left.kind()) {
            case INTEGER -> mv.visitInsn(intOp);
            case LONG -> mv.visitInsn(longOp);
            default -> throw new CompilationException("unsupported operand type: " + left);
        }
        operands.push(left);
    }

    private void intLongOperator(TypeMeta left, TypeMeta right, int intOp, int longOp) {
        if (!left.equals(right)) {
            throw new CompilationException("type mismatch: " + left + " (op) " + right);
        }

        switch (left.kind()) {
            case INTEGER -> mv.visitInsn(intOp);
            case LONG -> mv.visitInsn(longOp);
            default -> throw new CompilationException("unsupported operand type: " + left);
        }
        operands.push(left);
    }

    private void comparisonOperator(TypeMeta left, TypeMeta right, OperatorMeta operatorMeta) {
        if (!left.equals(right)) {
            throw new CompilationException("type mismatch: " + left + " (op) " + right);
        }

        switch (left.kind()) {
            case INTEGER -> {
                switch (operatorMeta) {
                    case LESSER -> compileIntegerComparison(Opcodes.IF_ICMPLT);
                    case GREATER -> compileIntegerComparison(Opcodes.IF_ICMPGT);
                    case LESSER_EQUAL -> compileIntegerComparison(Opcodes.IF_ICMPLE);
                    case GREATER_EQUAL -> compileIntegerComparison(Opcodes.IF_ICMPGE);

                    case EQUALS -> compileIntegerComparison(Opcodes.IF_ICMPEQ);
                    case NOT_EQUALS -> compileIntegerComparison(Opcodes.IF_ICMPNE);

                    default -> throw new CompilationException("unknown comparison operator: " + operatorMeta);
                }
            }
            case LONG -> compileTwoStepComparison(operatorMeta, Opcodes.LCMP);

            // TODO: Proper NaN treatment
            case FLOAT -> compileTwoStepComparison(operatorMeta, Opcodes.FCMPG);
            case DOUBLE -> compileTwoStepComparison(operatorMeta, Opcodes.DCMPG);

            default -> throw new CompilationException("unsupported operand type: " + left);
        }
        operands.push(new TypeMeta(Kind.INTEGER, false, null));
    }

    private void compileTwoStepComparison(OperatorMeta operatorMeta, int firstStepOp) {
        switch (operatorMeta) {
            case LESSER -> compileTwoStepComparison(firstStepOp, Opcodes.IFLT);
            case GREATER -> compileTwoStepComparison(firstStepOp, Opcodes.IFGT);
            case LESSER_EQUAL -> compileTwoStepComparison(firstStepOp, Opcodes.IFLE);
            case GREATER_EQUAL -> compileTwoStepComparison(firstStepOp, Opcodes.IFGE);

            case EQUALS -> compileTwoStepComparison(firstStepOp, Opcodes.IFEQ);
            case NOT_EQUALS -> compileTwoStepComparison(firstStepOp, Opcodes.IFNE);

            default -> throw new CompilationException("unknown comparison operator: " + operatorMeta);
        }
    }

    private void compileTwoStepComparison(int firstStepOp, int secondStepOp) {
        mv.visitInsn(firstStepOp);
        compileIntegerComparison(secondStepOp);
    }

    private void compileIntegerComparison(int op) {
        var target = new Label();
        var otherCode = new Label();

        mv.visitJumpInsn(op, target);
        mv.visitLdcInsn(0);
        mv.visitJumpInsn(Opcodes.GOTO, otherCode);
        mv.visitLabel(target);
        mv.visitLdcInsn(1);
        mv.visitLabel(otherCode);
    }
}
