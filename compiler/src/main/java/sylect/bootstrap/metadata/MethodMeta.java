// SPDX-License-Identifier: MIT

package sylect.bootstrap.metadata;

import sylect.SylectParser.MethodDefinitionContext;
import sylect.CompilationException;
import sylect.bootstrap.context.ImportManager;

import java.util.List;
import java.util.stream.Collectors;

public record MethodMeta(
        String name,
        boolean isStatic, boolean isNative, boolean isAbstract,
        TypeMeta returnType,
        List<ParameterMeta> parameters
) {

    public static MethodMeta fromContext(ImportManager importManager, MethodDefinitionContext ctx) {
        var name = ctx.IDENTIFIER().getText();
        if ("constructor".equals(name)) {
            name = "<init>";
        }

        var isStatic = ctx.methodModifiers().getText().contains("static");
        var isNative = ctx.methodModifiers().getText().contains("native");

        // Native methods don't have code blocks too
        var isAbstract = ctx.codeBlock() == null && !isNative;
        if (isStatic && isAbstract) {
            throw new CompilationException("Static method cannot be abstract");
        }

        var returnType = TypeMeta.fromContext(importManager, ctx.type());
        var parameters = ctx.parameter().stream()
                .map(parameter -> new ParameterMeta(parameter.IDENTIFIER().getText(),
                        TypeMeta.fromContext(importManager, parameter.type())))
                .collect(Collectors.toList());
        return new MethodMeta(name, isStatic, isNative, isAbstract, returnType, parameters);
    }

    public String asDescriptor() {
        return "(" + parameters.stream()
                .map(ParameterMeta::type)
                .map(TypeMeta::asDescriptor)
                .collect(Collectors.joining()) + ")" + returnType.asDescriptor();
    }
}
