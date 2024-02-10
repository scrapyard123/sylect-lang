// SPDX-License-Identifier: MIT

package forward.bootstrap.metadata.expression;

import forward.ForwardParser.UnaryOpContext;
import forward.bootstrap.CompilationException;

// TODO: Is it needed at all?
public enum UnaryOperatorMeta {
    MINUS, TYPE_CONVERSION;

    public static UnaryOperatorMeta fromContext(UnaryOpContext ctx) {
        if (ctx.type() != null) {
            return TYPE_CONVERSION;
        }

        var operator = ctx.getText();
        return switch (operator) {
            case "-" -> MINUS;
            default -> throw new CompilationException("unknown unary operator: " + operator);
        };
    }
}
