// SPDX-License-Identifier: MIT

package sylect.bootstrap.metadata;

import java.lang.reflect.Parameter;

public record ParameterMeta(String name, TypeMeta type) {

    public static ParameterMeta fromJavaParameter(Parameter parameter) {
        return new ParameterMeta(
                parameter.getName(),
                TypeMeta.fromJavaType(parameter.getType()));
    }
}
