// SPDX-License-Identifier: MIT

package sylect.bootstrap.context;

import sylect.CompilationException;
import sylect.SylectParser.ClassDefinitionContext;
import sylect.SylectParser.MethodDefinitionContext;
import sylect.bootstrap.metadata.ClassMeta;
import sylect.bootstrap.metadata.FieldMeta;
import sylect.bootstrap.metadata.LocalMeta;
import sylect.bootstrap.metadata.MethodMeta;
import sylect.bootstrap.metadata.ParameterMeta;
import sylect.bootstrap.metadata.TypeMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ScopeManager {

    private final ClassMetaManager classMetaManager;
    private final ImportManager importManager;

    private final Map<String, LocalMeta> locals = new HashMap<>();

    private ClassMeta classMeta;

    private boolean staticMethod = false;
    private int currentOffset = 0;

    public ScopeManager(ClassMetaManager classMetaManager, ImportManager importManager) {
        this.classMetaManager = classMetaManager;
        this.importManager = importManager;
    }

    public ClassMeta enterClass(ClassDefinitionContext ctx) {
        var className = ctx.IDENTIFIER().getText();
        classMeta = classMetaManager.resolveClass(className);

        if (classMeta == null) {
            throw new CompilationException("could not find class in source set: " + className);
        } else {
            return classMeta;
        }
    }

    public ClassMeta getClassMeta() {
        return classMeta;
    }

    public MethodMeta enterMethod(MethodDefinitionContext ctx) {
        currentOffset = 0;
        locals.clear();

        // TODO: Use class meta to get this
        var methodMeta = MethodMeta.fromContext(importManager, ctx);
        staticMethod = methodMeta.isStatic();

        if (!staticMethod) {
            addLocal("this", classMeta.asTypeMeta());
        }
        for (var parameter : methodMeta.parameters()) {
            addLocal(parameter.name(), parameter.type());
        }

        return methodMeta;
    }

    public boolean isStaticMethod() {
        return staticMethod;
    }

    public LocalMeta addLocal(String name, TypeMeta type) {
        if (locals.containsKey(name)) {
            throw new CompilationException("variable already present: " + name);
        }

        var localMeta = new LocalMeta(name, type, currentOffset);
        locals.put(name, localMeta);
        currentOffset += type.getLocalSize();
        return localMeta;
    }

    public LocalMeta getLocal(String name) {
        return locals.get(name);
    }

    public void forEachLocal(Consumer<LocalMeta> action) {
        locals.values().forEach(action);
    }

    public FieldMeta getField(String name) {
        return getField(classMeta, name);
    }

    public FieldMeta getField(ClassMeta classMeta, String name) {
        var currentClassFieldMeta = classMeta.fields().stream()
                .filter(fieldMeta -> name.equals(fieldMeta.name()))
                .findFirst()
                .orElse(null);
        if (currentClassFieldMeta == null) {
            if (classMeta.baseClassName() == null) {
                return null;
            }
            return getField(classMetaManager.resolveClass(classMeta.baseClassName()), name);
        }
        return currentClassFieldMeta;
    }

    public MethodMeta getMethod(ClassMeta classMeta, String name, List<TypeMeta> parameterTypes) {
        var currentClassMethodMeta = classMeta.methods().stream()
                .filter(methodMeta -> name.equals(methodMeta.name()))
                .filter(methodMeta -> methodMeta.parameters().stream()
                        .map(ParameterMeta::type)
                        .toList().equals(parameterTypes))
                .findFirst()
                .orElse(null);

        if (currentClassMethodMeta == null) {
            if (classMeta.iface()) {
                for (String interfaze : classMeta.interfaces()) {
                    var methodMeta = getMethod(classMetaManager.resolveClass(interfaze), name, parameterTypes);
                    if (methodMeta != null) {
                        return methodMeta;
                    }
                }
            } else {
                if (classMeta.baseClassName() == null) {
                    return null;
                }
                return getMethod(classMetaManager.resolveClass(classMeta.baseClassName()), name, parameterTypes);
            }
        }
        return currentClassMethodMeta;
    }
}
