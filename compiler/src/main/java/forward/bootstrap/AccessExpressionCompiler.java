// SPDX-License-Identifier: MIT

package forward.bootstrap;

import forward.ForwardParser.AccessExpressionContext;
import forward.ForwardParser.AccessTermContext;
import forward.ForwardParser.ExpressionContext;
import forward.bootstrap.metadata.ClassMeta;
import forward.bootstrap.metadata.TypeMeta;
import forward.bootstrap.metadata.TypeMeta.Kind;
import forward.bootstrap.metadata.expression.AccessMeta;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Objects;

public class AccessExpressionCompiler {
    private final ScopeManager scopeManager;
    private final MethodVisitor mv;

    public AccessExpressionCompiler(ScopeManager scopeManager, MethodVisitor mv) {
        this.scopeManager = Objects.requireNonNull(scopeManager);
        this.mv = Objects.requireNonNull(mv);
    }

    public TypeMeta compile(AccessExpressionContext ctx) {
        var accessMeta = (AccessMeta) null;
        for (AccessTermContext accessTermCtx : ctx.accessTerm()) {
            accessMeta = compileAccessTerm(accessMeta, accessTermCtx);
        }

        if (accessMeta.isTypeMeta()) {
            return accessMeta.typeMeta();
        } else {
            mv.visitLdcInsn(Type.getType(accessMeta.classMeta().asTypeMeta().asDescriptor()));
            return new TypeMeta(Kind.CLASS, false, "java.lang.Class");
        }
    }

    private AccessMeta compileAccessTerm(AccessMeta accessMeta, AccessTermContext ctx) {
        var identifier = ctx.IDENTIFIER().getText();
        if (accessMeta == null) {
            if (ctx.getText().contains("(")) {
                return compileLocalMethodCall(identifier, ctx.expression());
            } else {
                return compileLocalAccess(identifier);
            }
        } else {
            if (ctx.getText().contains("(")) {
                return compileNonLocalMethodCall(accessMeta, identifier, ctx.expression());
            } else {
                return compileNonLocalAccess(accessMeta, identifier);
            }
        }
    }

    private AccessMeta compileLocalAccess(String identifier) {
        var local = scopeManager.getLocal(identifier);
        if (local == null) {
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
        } else {
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
    }

    private AccessMeta compileNonLocalAccess(AccessMeta accessMeta, String identifier) {
        // If parent term is a class name, don't expect object on stack
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
        } else if (accessMeta.isTypeMeta()) {
            if (accessMeta.typeMeta().kind() != Kind.CLASS) {
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
        } else {
            throw new CompilationException("unsupported access type: " + accessMeta);
        }
    }

    private AccessMeta compileLocalMethodCall(String identifier, List<ExpressionContext> arguments) {
        // We always add target object (ourselves) to the stack
        // TODO: BUG: Calling a static method from a static method fails
        mv.visitVarInsn(Opcodes.ALOAD, 0);

        var parameterTypes = compileArguments(arguments);
        var method = scopeManager.getMethod(identifier, parameterTypes);

        if (method == null) {
            throw new CompilationException(
                    "unknown method: " + identifier + " in " + scopeManager.getClassMeta().name());
        }

        var classMeta = scopeManager.getClassMeta();
        var owner = ClassMeta.javaClassFromClassName(classMeta.name());
        if (method.isStatic()) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, identifier, method.asDescriptor(),
                    classMeta.iface());
        } else {
            if (classMeta.iface()) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, identifier, method.asDescriptor(),
                        classMeta.iface());
            } else {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, identifier, method.asDescriptor(),
                        classMeta.iface());
            }
        }

        // Remove target object if it was not required
        if (method.isStatic()) {
            if (method.returnType().kind() != Kind.VOID) {
                mv.visitInsn(Opcodes.SWAP);
            }
            mv.visitInsn(Opcodes.POP);
        }

        return new AccessMeta(null, method.returnType());
    }

    private AccessMeta compileNonLocalMethodCall(
            AccessMeta accessMeta, String identifier, List<ExpressionContext> arguments) {
        // If parent term is a class name, don't expect object on stack
        if (accessMeta.isClassMeta()) {
            var parameterTypes = compileArguments(arguments);
            var method = scopeManager.getMethod(accessMeta.classMeta(), identifier, parameterTypes);

            if (method == null) {
                throw new CompilationException(
                        "unknown method: " + identifier + " in " + accessMeta.classMeta().name());
            }
            if (!method.isStatic()) {
                throw new CompilationException(
                        "method is not static: " + identifier + " in " + accessMeta.classMeta().name());
            }

            var classMeta = accessMeta.classMeta();
            var owner = ClassMeta.javaClassFromClassName(classMeta.name());
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, identifier, method.asDescriptor(),
                    classMeta.iface());
            return new AccessMeta(null, method.returnType());
        } else if (accessMeta.isTypeMeta()) {
            if (accessMeta.typeMeta().kind() != Kind.CLASS) {
                throw new CompilationException("calling method on primitive type");
            }

            var classMeta = scopeManager.resolveClass(accessMeta.typeMeta().className());
            var parameterTypes = compileArguments(arguments);
            var method = scopeManager.getMethod(classMeta, identifier, parameterTypes);

            if (method == null) {
                throw new CompilationException("unknown method: " + identifier + " in " + classMeta);
            }

            var owner = ClassMeta.javaClassFromClassName(classMeta.name());
            if (method.isStatic()) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, identifier, method.asDescriptor(),
                        classMeta.iface());
            } else {
                if (classMeta.iface()) {
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, identifier, method.asDescriptor(),
                            classMeta.iface());
                } else {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, identifier, method.asDescriptor(),
                            classMeta.iface());
                }
            }

            // Remove target object if it was not required
            if (method.isStatic()) {
                if (method.returnType().kind() != Kind.VOID) {
                    mv.visitInsn(Opcodes.SWAP);
                }
                mv.visitInsn(Opcodes.POP);
            }

            return new AccessMeta(null, method.returnType());
        } else {
            throw new CompilationException("unsupported access type: " + accessMeta);
        }
    }

    private List<TypeMeta> compileArguments(List<ExpressionContext> arguments) {
        return arguments.stream()
                .map(expressionCtx -> new ExpressionCompiler(scopeManager, mv).compile(expressionCtx))
                .toList();
    }
}
