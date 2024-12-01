// SPDX-License-Identifier: MIT

package sylect.bootstrap.metadata.expression;

import sylect.bootstrap.metadata.ClassMeta;

public record CallTargetMeta(ClassMeta classMeta, boolean isConstructor, boolean isNewObject, boolean isSpecial) {
}
