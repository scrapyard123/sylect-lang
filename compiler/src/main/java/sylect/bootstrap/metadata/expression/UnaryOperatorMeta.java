// SPDX-License-Identifier: MIT

package sylect.bootstrap.metadata.expression;

import sylect.SylectParser.UnaryOpContext;
import sylect.CompilationException;

public enum UnaryOperatorMeta {
    MINUS, NOT, TYPE_CONVERSION;

    public static UnaryOperatorMeta fromContext(UnaryOpContext ctx) {
        if (ctx.type() != null) {
            return TYPE_CONVERSION;
        }

        var operator = ctx.getText();
        return switch (operator) {
            case "-" -> MINUS;
            case "!" -> NOT;
            default -> throw new CompilationException("unknown unary operator: " + operator);
        };
    }
}
