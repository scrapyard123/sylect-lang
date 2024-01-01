// SPDX-License-Identifier: MIT

package forward.bootstrap.metadata;

import forward.ForwardParser.MethodDefinitionContext;
import forward.bootstrap.CompilationException;
import forward.bootstrap.ScopeManager;

import java.util.List;
import java.util.stream.Collectors;

public record MethodMeta(
        String name,
        boolean isStatic, boolean isAbstract,
        TypeMeta returnType,
        List<ParameterMeta> parameters
) {
    public static MethodMeta fromContext(ScopeManager scopeManager, MethodDefinitionContext ctx) {
        var name = ctx.IDENTIFIER().getText();
        if ("constructor".equals(name)) {
            name = "<init>";
        }

        var isStatic = ctx.getText().startsWith("static");
        var isAbstract = ctx.codeBlock() == null;
        if (isStatic && isAbstract) {
            throw new CompilationException("Static method cannot be abstract");
        }

        var returnType = TypeMeta.fromContext(scopeManager, ctx.type());
        var parameters = ctx.parameter().stream()
                .map(parameter -> new ParameterMeta(parameter.IDENTIFIER().getText(),
                        TypeMeta.fromContext(scopeManager, parameter.type())))
                .collect(Collectors.toList());
        return new MethodMeta(name, isStatic, isAbstract, returnType, parameters);
    }

    public String asDescriptor() {
        return "(" + parameters.stream()
                .map(ParameterMeta::type)
                .map(TypeMeta::asDescriptor)
                .collect(Collectors.joining()) + ")" + returnType.asDescriptor();
    }
}
