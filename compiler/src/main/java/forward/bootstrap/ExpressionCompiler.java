// SPDX-License-Identifier: MIT

package forward.bootstrap;

import forward.ForwardParser.ExpressionContext;
import forward.ForwardParser.TermContext;
import forward.bootstrap.metadata.TypeMeta;
import forward.bootstrap.metadata.TypeMeta.Kind;
import forward.bootstrap.metadata.expression.OperatorMeta;
import forward.bootstrap.metadata.expression.UnaryOperatorMeta;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Objects;
import java.util.Stack;

public class ExpressionCompiler {
    private final ScopeManager scopeManager;
    private final MethodVisitor mv;

    private final Stack<TypeMeta> operands;
    private final Stack<OperatorMeta> operators;

    public ExpressionCompiler(ScopeManager scopeManager, MethodVisitor mv) {
        this.scopeManager = Objects.requireNonNull(scopeManager);
        this.mv = Objects.requireNonNull(mv);

        this.operands = new Stack<>();
        this.operators = new Stack<>();
    }

    public TypeMeta compile(ExpressionContext ctx) {
        // Shunting yard algorithm
        var termContexts = ctx.term();
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

    private TypeMeta compileTerm(TermContext ctx) {
        var operandType = (TypeMeta) null;

        if (ctx.LITERAL() != null) {
            var kind = (TypeMeta.Kind) null;
            var className = (String) null;

            var literal = ctx.LITERAL().getText();
            if (literal.startsWith("\"") && literal.endsWith("\"")) {
                kind = Kind.CLASS;
                className = "java.lang.String";
                literal = literal.substring(1, literal.length() - 1);
            } else if (literal.endsWith("F")) {
                kind = Kind.FLOAT;
                literal = literal.substring(0, literal.length() - 1);
            } else if (literal.endsWith("L")) {
                kind = Kind.LONG;
                literal = literal.substring(0, literal.length() - 1);
            } else if (literal.contains(".")) {
                kind = Kind.DOUBLE;
            } else {
                kind = Kind.INTEGER;
            }

            switch (kind) {
                case INTEGER -> mv.visitLdcInsn(Integer.parseInt(literal));
                case LONG -> mv.visitLdcInsn(Long.parseLong(literal));
                case FLOAT -> mv.visitLdcInsn(Float.parseFloat(literal));
                case DOUBLE -> mv.visitLdcInsn(Double.parseDouble(literal));
                case CLASS -> mv.visitLdcInsn(literal);
                default -> throw new CompilationException("unsupported literal type: " + kind);
            }

            operandType = new TypeMeta(kind, false, className);
        }

        if (ctx.accessExpression() != null) {
            operandType = new AccessExpressionCompiler(scopeManager, mv).compile(ctx.accessExpression());
        }

        if (ctx.expression() != null) {
            operandType = new ExpressionCompiler(scopeManager, mv).compile(ctx.expression());
        }

        if (ctx.unaryOp() != null) {
            for (var unaryOpContext : ctx.unaryOp()) {
                var unaryOp = UnaryOperatorMeta.fromContext(unaryOpContext);
                switch (unaryOp) {
                    case MINUS -> {
                        switch (operandType.kind()) {
                            case INTEGER -> mv.visitInsn(Opcodes.INEG);
                            case LONG -> mv.visitInsn(Opcodes.LNEG);
                            case FLOAT -> mv.visitInsn(Opcodes.FNEG);
                            case DOUBLE -> mv.visitInsn(Opcodes.DNEG);
                            default -> throw new CompilationException("could not negate: " + operandType);
                        }
                    }
                    default -> throw new CompilationException("unsupported unary operator: " + unaryOp);
                }
            }
        }

        if (operandType == null) {
            throw new CompilationException("failed to compile term: " + ctx.getText());
        } else {
            return operandType;
        }
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
