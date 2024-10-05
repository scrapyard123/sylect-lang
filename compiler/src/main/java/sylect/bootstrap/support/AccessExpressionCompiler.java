// SPDX-License-Identifier: MIT

package sylect.bootstrap.support;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import sylect.CompilationException;
import sylect.SylectParser.AccessExpressionContext;
import sylect.SylectParser.AccessTermContext;
import sylect.SylectParser.ExpressionContext;
import sylect.bootstrap.ScopeManager;
import sylect.bootstrap.metadata.ClassMeta;
import sylect.bootstrap.metadata.TypeMeta;
import sylect.bootstrap.metadata.expression.AccessMeta;
import sylect.bootstrap.util.ClassUtils;
import sylect.util.Pair;

import java.util.List;
import java.util.Objects;

/**
 * Class that compiles "access expressions" - chain of field access and method call operations.
 * {@link AccessMeta} helps to track current object that we operate on. If it's a class, we don't have
 * anything on stack, if it's an object/value - it's stored at the top.
 */
public class AccessExpressionCompiler {
    private final ScopeManager scopeManager;
    private final MethodVisitor mv;

    public AccessExpressionCompiler(ScopeManager scopeManager, MethodVisitor mv) {
        this.scopeManager = Objects.requireNonNull(scopeManager);
        this.mv = Objects.requireNonNull(mv);
    }

    public TypeMeta compile(AccessExpressionContext ctx) {
        // Follow chain of field access operations/method calls
        var accessMeta = (AccessMeta) null;
        for (AccessTermContext accessTermCtx : ctx.accessTerm()) {
            accessMeta = compileAccessTerm(accessMeta, accessTermCtx);
        }

        // If we end up with object/value - return it, otherwise create and return Class<?> object
        if (accessMeta.isTypeMeta()) {
            return accessMeta.typeMeta();
        } else {
            mv.visitLdcInsn(Type.getType(accessMeta.classMeta().asTypeMeta().asDescriptor()));
            return new TypeMeta(TypeMeta.Kind.CLASS, false, "java.lang.Class");
        }
    }

    private AccessMeta compileAccessTerm(AccessMeta accessMeta, AccessTermContext ctx) {
        var identifier = ctx.IDENTIFIER().getText();

        if (ctx.getText().contains("(")) {
            return compileMethodCall(accessMeta, identifier, ctx.expression());
        }

        // If we immediately start with method call/field access - we are working within current class
        if (accessMeta == null) {
            return compileLocalAccess(identifier);
        } else {
            return compileNonLocalAccess(accessMeta, identifier);
        }
    }

    private AccessMeta compileLocalAccess(String identifier) {
        // Try to find corresponding local variable
        var local = scopeManager.getLocal(identifier);
        if (local != null) {
            switch (local.type().kind()) {
                case INTEGER -> mv.visitIntInsn(Opcodes.ILOAD, local.offset());
                case LONG -> mv.visitIntInsn(Opcodes.LLOAD, local.offset());
                case FLOAT -> mv.visitIntInsn(Opcodes.FLOAD, local.offset());
                case DOUBLE -> mv.visitIntInsn(Opcodes.DLOAD, local.offset());
                case CLASS -> mv.visitIntInsn(Opcodes.ALOAD, local.offset());
                default -> throw new CompilationException("unsupported variable type: " + local.type());
            }
            return new AccessMeta(null, local.type());
        }

        // Try to find corresponding local field
        var field = scopeManager.getField(identifier);
        if (field == null) {
            var classMeta = scopeManager.resolveClass(scopeManager.resolveImport(identifier));
            return new AccessMeta(classMeta, null);
        }

        var owner = ClassMeta.javaClassFromClassName(scopeManager.getClassMeta().name());
        if (field.isStatic()) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, owner, identifier, field.asDescriptor());
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, owner, identifier, field.asDescriptor());
        }
        return new AccessMeta(null, field.type());
    }

    private AccessMeta compileNonLocalAccess(AccessMeta accessMeta, String identifier) {
        // If previous term produced a class - we are trying to access a static field
        if (accessMeta.isClassMeta()) {
            var field = scopeManager.getField(accessMeta.classMeta(), identifier);
            if (field == null) {
                throw new CompilationException(
                        "unknown field: " + identifier + " in " + accessMeta.classMeta().name());
            }

            if (!field.isStatic()) {
                throw new CompilationException(
                        "field is not static: " + identifier + " in " + accessMeta.classMeta().name());
            }

            var owner = ClassMeta.javaClassFromClassName(accessMeta.classMeta().name());
            mv.visitFieldInsn(Opcodes.GETSTATIC, owner, identifier, field.asDescriptor());
            return new AccessMeta(null, field.type());
        }

        // If previous term produced an object - we are trying to access instance field
        if (accessMeta.isTypeMeta()) {
            if (accessMeta.typeMeta().kind() != TypeMeta.Kind.CLASS) {
                throw new CompilationException("accessing field on primitive type");
            }

            var classMeta = scopeManager.resolveClass(accessMeta.typeMeta().className());
            var field = scopeManager.getField(classMeta, identifier);
            if (field == null) {
                throw new CompilationException("unknown field: " + identifier + " in " + classMeta);
            }

            var owner = ClassMeta.javaClassFromClassName(classMeta.name());
            if (field.isStatic()) {
                mv.visitInsn(Opcodes.POP);
                mv.visitFieldInsn(Opcodes.GETSTATIC, owner, identifier, field.asDescriptor());
            } else {
                mv.visitFieldInsn(Opcodes.GETFIELD, owner, identifier, field.asDescriptor());
            }

            return new AccessMeta(null, field.type());
        }

        // Otherwise something is really wrong...
        throw new CompilationException("unsupported access type: " + accessMeta);
    }

    private AccessMeta compileMethodCall(AccessMeta accessMeta, String identifier, List<ExpressionContext> arguments) {
        // Prepare target for method class (new object/this object/simply class meta)
        var pair = prepareTarget(accessMeta, identifier);
        var classMeta = pair.left();
        var newObject = pair.right();

        // Compile arguments to determine parameter types
        var parameterTypes = compileArguments(arguments);

        var method = scopeManager.getMethod(classMeta, newObject ? "<init>" : identifier, parameterTypes);
        if (method == null) {
            throw new CompilationException("unknown method: " + identifier + " in " + classMeta.name());
        }

        // Target method must be static if called from static method or when called on class
        if ((accessMeta == null && scopeManager.isStaticMethod()) || (accessMeta != null && accessMeta.isClassMeta())) {
            if (!method.isStatic()) {
                throw new CompilationException("method is not static: " + method.name() + " in " + classMeta.name());
            }
        }

        // Determine the exact call instruction
        var owner = ClassMeta.javaClassFromClassName(classMeta.name());
        if (method.isStatic()) {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC, owner, method.name(), method.asDescriptor(), classMeta.iface());
        } else {
            if (newObject) {
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, method.name(), method.asDescriptor(), false);
            } else if (classMeta.iface()) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, method.name(), method.asDescriptor(), true);
            } else {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, method.name(), method.asDescriptor(), false);
            }
        }

        // Remove target object if it was not required and is present at the stack
        if (method.isStatic() && (accessMeta == null || accessMeta.isTypeMeta())) {
            if (method.returnType().kind() != TypeMeta.Kind.VOID) {
                mv.visitInsn(Opcodes.SWAP);
            }
            mv.visitInsn(Opcodes.POP);
        }

        return new AccessMeta(null, newObject ? classMeta.asTypeMeta() : method.returnType());
    }

    private Pair<ClassMeta, Boolean> prepareTarget(AccessMeta accessMeta, String identifier) {
        // If we immediately start with method call - we are working within current class
        if (accessMeta == null) {
            // If identifier is a valid class name - we are constructing an object
            try {
                var classMeta = scopeManager.resolveClass(scopeManager.resolveImport(identifier));
                mv.visitTypeInsn(Opcodes.NEW, ClassMeta.javaClassFromClassName(classMeta.name()));
                mv.visitInsn(Opcodes.DUP); // one for constructor call and one for next chain terms
                return new Pair<>(classMeta, true);
            } catch (CompilationException e) {
                // It's not a valid class name - proceed with compilation
            }

            // Add target object (ourselves) to the stack if we are not in static class
            if (!scopeManager.isStaticMethod()) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
            }
            return new Pair<>(scopeManager.getClassMeta(), false);
        } else {
            // If previous term is a class - it's going to be static method call
            if (accessMeta.isClassMeta()) {
                return new Pair<>(accessMeta.classMeta(), false);
            }

            if (accessMeta.isTypeMeta()) {
                if (accessMeta.typeMeta().kind() != TypeMeta.Kind.CLASS) {
                    throw new CompilationException("calling method on primitive type");
                }

                return new Pair<>(scopeManager.resolveClass(accessMeta.typeMeta().className()), false);
            }

            throw new CompilationException("failed to determine target class for method call");
        }
    }

    private List<TypeMeta> compileArguments(List<ExpressionContext> arguments) {
        return arguments.stream()
                .map(expressionCtx -> new ExpressionCompiler(scopeManager, mv).compile(expressionCtx))
                .toList();
    }
}
