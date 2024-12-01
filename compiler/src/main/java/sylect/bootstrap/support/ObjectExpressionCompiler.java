// SPDX-License-Identifier: MIT

package sylect.bootstrap.support;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import sylect.CompilationException;
import sylect.SylectParser.ExpressionContext;
import sylect.SylectParser.ObjectExpressionContext;
import sylect.SylectParser.ObjectTermContext;
import sylect.bootstrap.ScopeManager;
import sylect.bootstrap.metadata.ClassMeta;
import sylect.bootstrap.metadata.TypeMeta;
import sylect.bootstrap.metadata.expression.ObjectMeta;
import sylect.bootstrap.util.ClassUtils;
import sylect.util.Pair;

import java.util.List;
import java.util.Objects;

/**
 * Class that compiles "object expressions" - chain of field access and method call operations.
 * {@link ObjectMeta} helps to track current object that we operate on. If it's a class, we don't have
 * anything on stack, if it's an object/value - it's stored at the top.
 */
public class ObjectExpressionCompiler {
    private final ScopeManager scopeManager;
    private final MethodVisitor mv;

    public ObjectExpressionCompiler(ScopeManager scopeManager, MethodVisitor mv) {
        this.scopeManager = Objects.requireNonNull(scopeManager);
        this.mv = Objects.requireNonNull(mv);
    }

    public TypeMeta compile(ObjectExpressionContext ctx) {
        // Follow chain of field access operations/method calls
        var objectMeta = (ObjectMeta) null;
        for (var objectTermCtx : ctx.objectTerm()) {
            objectMeta = compileObjectTerm(objectMeta, objectTermCtx);
        }

        // If we end up with object/value - return it, otherwise create and return Class<?> object
        if (objectMeta.isTypeMeta()) {
            return objectMeta.typeMeta();
        } else {
            mv.visitLdcInsn(Type.getType(objectMeta.classMeta().asTypeMeta().asDescriptor()));
            return new TypeMeta(TypeMeta.Kind.CLASS, false, "java/lang/Class");
        }
    }

    private ObjectMeta compileObjectTerm(ObjectMeta objectMeta, ObjectTermContext ctx) {
        if (ctx.STRING_LITERAL() != null) {
            if (objectMeta != null) {
                throw new CompilationException("string literal can only be a first term");
            }
            return new ObjectMeta(null, ClassUtils.visitStringLiteral(ctx.STRING_LITERAL(), mv::visitLdcInsn));
        }

        var identifier = ctx.IDENTIFIER().getText();

        if (ctx.getText().contains("(")) {
            return compileMethodCall(objectMeta, identifier, ctx.expression());
        }

        // If we immediately start with method call/field access - we are working within current class
        if (objectMeta == null) {
            return compileLocalAccess(identifier);
        } else {
            return compileNonLocalAccess(objectMeta, identifier);
        }
    }

    private ObjectMeta compileLocalAccess(String identifier) {
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
            return new ObjectMeta(null, local.type());
        }

        // Try to find corresponding local field
        var field = scopeManager.getField(identifier);
        if (field == null) {
            var classMeta = scopeManager.resolveClass(scopeManager.resolveImport(identifier));
            return new ObjectMeta(classMeta, null);
        }

        var owner = scopeManager.getClassMeta().name();
        if (field.isStatic()) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, owner, identifier, field.asDescriptor());
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, owner, identifier, field.asDescriptor());
        }
        return new ObjectMeta(null, field.type());
    }

    private ObjectMeta compileNonLocalAccess(ObjectMeta objectMeta, String identifier) {
        // If previous term produced a class - we are trying to access a static field
        if (objectMeta.isClassMeta()) {
            var field = scopeManager.getField(objectMeta.classMeta(), identifier);
            if (field == null) {
                throw new CompilationException(
                        "unknown field: " + identifier + " in " + objectMeta.classMeta().name());
            }

            if (!field.isStatic()) {
                throw new CompilationException(
                        "field is not static: " + identifier + " in " + objectMeta.classMeta().name());
            }

            var owner = objectMeta.classMeta().name();
            mv.visitFieldInsn(Opcodes.GETSTATIC, owner, identifier, field.asDescriptor());
            return new ObjectMeta(null, field.type());
        }

        // If previous term produced an object - we are trying to access instance field
        if (objectMeta.isTypeMeta()) {
            if (objectMeta.typeMeta().kind() != TypeMeta.Kind.CLASS) {
                throw new CompilationException("accessing field on primitive type");
            }

            var classMeta = scopeManager.resolveClass(objectMeta.typeMeta().className());
            var field = scopeManager.getField(classMeta, identifier);
            if (field == null) {
                throw new CompilationException("unknown field: " + identifier + " in " + classMeta);
            }

            var owner = classMeta.name();
            if (field.isStatic()) {
                mv.visitInsn(Opcodes.POP);
                mv.visitFieldInsn(Opcodes.GETSTATIC, owner, identifier, field.asDescriptor());
            } else {
                mv.visitFieldInsn(Opcodes.GETFIELD, owner, identifier, field.asDescriptor());
            }

            return new ObjectMeta(null, field.type());
        }

        // Otherwise something is really wrong...
        throw new CompilationException("unsupported access type: " + objectMeta);
    }

    private ObjectMeta compileMethodCall(ObjectMeta objectMeta, String identifier, List<ExpressionContext> arguments) {
        // Prepare target for method class (new object/this object/simply class meta)
        var pair = prepareTarget(objectMeta, identifier);
        var classMeta = pair.left();
        var newObject = pair.right();

        // Compile arguments to determine parameter types
        var parameterTypes = compileArguments(arguments);

        var method = scopeManager.getMethod(classMeta, newObject ? "<init>" : identifier, parameterTypes);
        if (method == null) {
            throw new CompilationException("unknown method: " + identifier + " in " + classMeta.name());
        }

        // Target method must be static if called from static method or when called on class
        if ((objectMeta == null && scopeManager.isStaticMethod()) || (objectMeta != null && objectMeta.isClassMeta())) {
            if (!method.isStatic()) {
                throw new CompilationException("method is not static: " + method.name() + " in " + classMeta.name());
            }
        }

        // Determine the exact call instruction
        var owner = classMeta.name();
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
        if (method.isStatic() && (objectMeta == null || objectMeta.isTypeMeta())) {
            if (method.returnType().kind() != TypeMeta.Kind.VOID) {
                mv.visitInsn(Opcodes.SWAP);
            }
            mv.visitInsn(Opcodes.POP);
        }

        return new ObjectMeta(null, newObject ? classMeta.asTypeMeta() : method.returnType());
    }

    private Pair<ClassMeta, Boolean> prepareTarget(ObjectMeta objectMeta, String identifier) {
        // If we immediately start with method call - we are working within current class
        if (objectMeta == null) {
            // If identifier is a valid class name - we are constructing an object
            try {
                var classMeta = scopeManager.resolveClass(scopeManager.resolveImport(identifier));
                mv.visitTypeInsn(Opcodes.NEW, classMeta.name());
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
            if (objectMeta.isClassMeta()) {
                return new Pair<>(objectMeta.classMeta(), false);
            }

            if (objectMeta.isTypeMeta()) {
                if (objectMeta.typeMeta().kind() != TypeMeta.Kind.CLASS) {
                    throw new CompilationException("calling method on primitive type");
                }

                return new Pair<>(scopeManager.resolveClass(objectMeta.typeMeta().className()), false);
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
