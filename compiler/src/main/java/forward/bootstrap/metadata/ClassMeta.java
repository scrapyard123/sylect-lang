// SPDX-License-Identifier: MIT

package forward.bootstrap.metadata;

import forward.ForwardParser;
import forward.ForwardParser.BaseClassContext;
import forward.ForwardParser.ClassDefinitionContext;
import forward.ForwardParser.ProgramContext;
import forward.bootstrap.CompilationException;
import forward.bootstrap.ScopeManager;
import forward.bootstrap.metadata.TypeMeta.Kind;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record ClassMeta(String name, boolean iface,
                        String baseClassName, Set<String> interfaces,
                        Set<FieldMeta> fields, Set<MethodMeta> methods) {
    public static String javaClassFromClassName(String className) {
        return className.replace('.', '/');
    }

    public static String shortClassName(String identifier) {
        var parts = identifier.split("\\.");
        return parts[parts.length - 1];
    }

    public static ClassMeta fromForwardTree(ScopeManager scopeManager, ProgramContext ctx) {
        scopeManager.enterSource(ctx);

        var iface = ctx.getText().startsWith("interface");

        var className = ctx.classDefinition().IDENTIFIER().getText();
        var baseClassName = Optional.of(ctx.classDefinition())
                .map(ClassDefinitionContext::baseClass)
                .map(BaseClassContext::IDENTIFIER)
                .map(TerminalNode::getText)
                .map(scopeManager::resolveImport)
                .orElse("java.lang.Object");
        var interfaces = Optional.of(ctx.classDefinition())
                .map(ClassDefinitionContext::interfaceClass)
                .map(interfaceList -> interfaceList.stream()
                        .map(ForwardParser.InterfaceClassContext::IDENTIFIER)
                        .map(TerminalNode::getText)
                        .map(scopeManager::resolveImport)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
        var fields = Optional.ofNullable(ctx.fieldDefinition())
                .map(fieldDefinitions -> fieldDefinitions.stream()
                        .map(fieldDefinition -> FieldMeta.fromContext(scopeManager, fieldDefinition))
                        .collect(Collectors.toSet()))
                .orElse(Set.of());

        if (iface && !fields.isEmpty()) {
            throw new CompilationException("interface classes cannot contain fields");
        }

        var methods = Optional.ofNullable(ctx.methodDefinition())
                .map(methodDefinitions -> methodDefinitions.stream()
                        .map(methodDefinition -> MethodMeta.fromContext(scopeManager, methodDefinition))
                        .collect(Collectors.toSet()))
                .orElse(Set.of());

        return new ClassMeta(
                className, iface,
                baseClassName, interfaces,
                fields, methods);
    }

    public static ClassMeta fromJavaClass(Class<?> clazz) {
        var className = clazz.getName();
        var baseClassName = Optional.ofNullable(clazz.getSuperclass())
                .map(Class::getName)
                .orElse(null);
        var interfaces = Arrays.stream(clazz.getInterfaces())
                .map(Class::getName)
                .collect(Collectors.toSet());
        var fields = Arrays.stream(clazz.getDeclaredFields())
                .map(field -> new FieldMeta(
                        field.getName(),
                        Modifier.isStatic(field.getModifiers()),
                        TypeMeta.fromJavaType(field.getType())))
                .collect(Collectors.toSet());
        var methods = Arrays.stream(clazz.getDeclaredMethods())
                .map(method -> new MethodMeta(
                        method.getName(),
                        Modifier.isStatic(method.getModifiers()),
                        Modifier.isAbstract(method.getModifiers()),
                        TypeMeta.fromJavaType(method.getReturnType()),
                        Arrays.stream(method.getParameters())
                                .map(parameter -> new ParameterMeta(
                                        parameter.getName(),
                                        TypeMeta.fromJavaType(parameter.getType())))
                                .collect(Collectors.toList())))
                .collect(Collectors.toSet());
        return new ClassMeta(className, clazz.isInterface(), baseClassName, interfaces, fields, methods);
    }

    public TypeMeta asTypeMeta() {
        return new TypeMeta(Kind.CLASS, false, name);
    }
}
