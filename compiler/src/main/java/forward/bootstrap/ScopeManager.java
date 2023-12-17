// SPDX-License-Identifier: MIT

package forward.bootstrap;

import forward.ForwardParser.ClassDefinitionContext;
import forward.ForwardParser.ImportSectionContext;
import forward.ForwardParser.MethodDefinitionContext;
import forward.ForwardParser.ProgramContext;
import forward.bootstrap.metadata.ClassMeta;
import forward.bootstrap.metadata.FieldMeta;
import forward.bootstrap.metadata.LocalMeta;
import forward.bootstrap.metadata.MethodMeta;
import forward.bootstrap.metadata.ParameterMeta;
import forward.bootstrap.metadata.TypeMeta;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ScopeManager {
    private final ClassLoader classLoader;

    private final Map<String, LocalMeta> locals = new HashMap<>();
    private final Map<String, ClassMeta> classMetaMap = new HashMap<>();

    private Map<String, String> imports;
    private ClassMeta classMeta;
    private int currentOffset = 0;

    public ScopeManager(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void enterSource(ProgramContext ctx) {
        imports = Optional.ofNullable(ctx.importSection())
                .map(ImportSectionContext::IDENTIFIER)
                .map(imports -> imports.stream()
                        .map(TerminalNode::getText)
                        .collect(Collectors.toMap(ClassMeta::shortClassName, Function.identity())))
                .orElse(Map.of());
    }

    public String resolveImport(String identifier) {
        return imports.getOrDefault(identifier, identifier);
    }

    public ClassMeta resolveClass(String identifier) {
        return classMetaMap.computeIfAbsent(identifier, id -> {
            try {
                return ClassMeta.fromJavaClass(classLoader.loadClass(id));
            } catch (ClassNotFoundException e) {
                throw new CompilationException("unknown class: " + id);
            }
        });
    }

    public void addToSourceSet(ClassMeta classMeta) {
        classMetaMap.put(classMeta.name(), classMeta);
    }

    public ClassMeta enterClass(ClassDefinitionContext ctx) {
        var className = ctx.IDENTIFIER().getText();
        classMeta = classMetaMap.get(className);

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
        var methodMeta = MethodMeta.fromContext(this, ctx);
        if (!methodMeta.isStatic()) {
            addLocal("this", classMeta.asTypeMeta());
        }

        for (var parameter : methodMeta.parameters()) {
            addLocal(parameter.name(), parameter.type());
        }

        return methodMeta;
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
            return getField(resolveClass(classMeta.baseClassName()), name);
        }
        return currentClassFieldMeta;
    }

    public MethodMeta getMethod(String name, List<TypeMeta> parameterTypes) {
        return getMethod(classMeta, name, parameterTypes);
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
            if (classMeta.isInterface()) {
                for (String interfaze : classMeta.interfaces()) {
                    return getMethod(resolveClass(interfaze), name, parameterTypes);
                }
            } else {
                if (classMeta.baseClassName() == null) {
                    return null;
                }
                return getMethod(resolveClass(classMeta.baseClassName()), name, parameterTypes);
            }
        }
        return currentClassMethodMeta;
    }
}
