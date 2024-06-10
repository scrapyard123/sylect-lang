// SPDX-License-Identifier: MIT

package sylect.bootstrap.metadata.expression;

import sylect.bootstrap.metadata.ClassMeta;
import sylect.bootstrap.metadata.TypeMeta;

/**
 * @param classMeta {@link ClassMeta} represents class that we are working with
 * @param typeMeta {@link TypeMeta} represents value that we are working with
 */
public record AccessMeta(ClassMeta classMeta, TypeMeta typeMeta) {
    public boolean isClassMeta() {
        return classMeta != null;
    }

    public boolean isTypeMeta() {
        return typeMeta != null;
    }
}
