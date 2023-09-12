// SPDX-License-Identifier: MIT

package forward.bootstrap.metadata.expression;

import forward.bootstrap.metadata.ClassMeta;
import forward.bootstrap.metadata.TypeMeta;

public record AccessMeta(ClassMeta classMeta, TypeMeta typeMeta) {
    public boolean isClassMeta() {
        return classMeta != null;
    }

    public boolean isTypeMeta() {
        return typeMeta != null;
    }
}
