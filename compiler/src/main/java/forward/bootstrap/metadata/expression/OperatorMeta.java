// SPDX-License-Identifier: MIT

package forward.bootstrap.metadata.expression;

import forward.ForwardParser.OperatorContext;
import forward.bootstrap.CompilationException;

public enum OperatorMeta {
    MULTIPLY(7), DIVIDE(7), REM(7),
    PLUS(6), MINUS(6),
    SHIFT_LEFT(5), SHIFT_RIGHT(5), LOGICAL_SHIFT_RIGHT(5),
    LESSER(4), GREATER(4), LESSER_EQUAL(4), GREATER_EQUAL(4),
    EQUALS(3), NOT_EQUALS(3),
    BITWISE_AND(2),
    BITWISE_XOR(1),
    BITWISE_OR(0);

    private final int precedence;

    private OperatorMeta(int precedence) {
        this.precedence = precedence;
    }

    public static OperatorMeta fromContext(OperatorContext ctx) {
        var operator = ctx.getText();
        return switch (operator) {
            case "*" -> MULTIPLY;
            case "/" -> DIVIDE;
            case "%" -> REM;

            case "+" -> PLUS;
            case "-" -> MINUS;

            case "<<" -> SHIFT_LEFT;
            case ">>" -> SHIFT_RIGHT;
            case ">>>" -> LOGICAL_SHIFT_RIGHT;

            case "<" -> LESSER;
            case ">" -> GREATER;
            case "<=" -> LESSER_EQUAL;
            case ">=" -> GREATER_EQUAL;

            case "==" -> EQUALS;
            case "!=" -> NOT_EQUALS;

            case "&" -> BITWISE_AND;
            case "^" -> BITWISE_XOR;
            case "|" -> BITWISE_OR;

            default -> throw new CompilationException("unknown operator: " + operator);
        };
    }

    public int comparePrecedence(OperatorMeta that) {
        return precedence - that.precedence;
    }
}
