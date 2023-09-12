// SPDX-License-Identifier: MIT

package forward.bootstrap.metadata.expression;

import forward.ForwardParser.UnaryOpContext;
import forward.bootstrap.CompilationException;

public enum UnaryOperatorMeta {
    MINUS;

    public static UnaryOperatorMeta fromContext(UnaryOpContext ctx) {
        var operator = ctx.getText();
        return switch (operator) {
            case "-" -> MINUS;
            default -> throw new CompilationException("unknown unary operator: " + operator);
        };
    }
}
