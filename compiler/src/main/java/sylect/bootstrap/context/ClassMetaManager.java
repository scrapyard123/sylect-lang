// SPDX-License-Identifier: MIT

package sylect.bootstrap.context;

import sylect.CompilationException;
import sylect.bootstrap.metadata.ClassMeta;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassMetaManager {

    private final ClassLoader classLoader;
    private final Map<String, ClassMeta> classMetaMap = new ConcurrentHashMap<>();

    public ClassMetaManager(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ClassMeta resolveClass(String identifier) {
        return classMetaMap.computeIfAbsent(identifier, id -> {
            try {
                return ClassMeta.fromJavaClass(classLoader.loadClass(ClassMeta.sylectClassNameToJavaClassName(id)));
            } catch (ClassNotFoundException e) {
                throw new CompilationException("unknown class: " + id);
            }
        });
    }

    public void addToSourceSet(ClassMeta classMeta) {
        classMetaMap.put(classMeta.name(), classMeta);
    }
}
