// SPDX-License-Identifier: MIT

package sylect.bootstrap.support;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import sylect.CompilationException;
import sylect.SylectParser;
import sylect.bootstrap.ScopeManager;
import sylect.bootstrap.metadata.MethodMeta;
import sylect.bootstrap.metadata.TypeMeta;
import sylect.bootstrap.util.ClassUtils;

import java.util.function.Function;

public class AnnotationCompiler {

    private final ScopeManager scopeManager;

    public AnnotationCompiler(ScopeManager scopeManager) {
        this.scopeManager = scopeManager;
    }

    public void visitAnnotationBlock(
            SylectParser.AnnotationBlockContext ctx,
            Function<String, AnnotationVisitor> visitorGenerator) {
        if (ctx == null) {
            return;
        }

        for (var definition : ctx.annotationDefinition()) {
            visitAnnotationDefinition(definition, visitorGenerator, null);
        }
    }

    private void visitAnnotationDefinition(
            SylectParser.AnnotationDefinitionContext ctx,
            Function<String, AnnotationVisitor> visitorGenerator,
            TypeMeta expectedType) {

        var classMeta = scopeManager.resolveClass(scopeManager.resolveImport(ctx.type().getText()));
        var visitor = visitorGenerator.apply(classMeta.asTypeMeta().asDescriptor());

        if (expectedType != null && !classMeta.asTypeMeta().equals(expectedType.arrayElementType())) {
            throw new CompilationException("bad annotation type " + ctx.type() + ", expected: " + expectedType);
        }

        for (var param : ctx.annotationParameter()) {
            var name = param.IDENTIFIER(0).getText();
            var paramType = classMeta.methods().stream()
                    .filter(method -> method.name().equals(name))
                    .map(MethodMeta::returnType)
                    .findFirst()
                    .orElseThrow(() -> new CompilationException("unknown annotation field: " + name));

            if (paramType.isArray()) {
                visitArrayParameter(name, paramType, visitor, param);
            } else {
                visitParameter(name, paramType, visitor, param);
            }
        }

        visitor.visitEnd();
    }

    private void visitParameter(
            String name, TypeMeta paramType,
            AnnotationVisitor visitor,
            SylectParser.AnnotationParameterContext param) {

        if (param.LITERAL().size() > 1 || param.IDENTIFIER().size() > 2 || param.annotationDefinition().size() > 1) {
            throw new CompilationException("single value is expected: " + name);
        }

        if (!param.LITERAL().isEmpty()) {
            var literalType = ClassUtils.visitLiteral(param.LITERAL(0), value -> visitor.visit(name, value));
            if (!literalType.equals(paramType)) {
                throw new CompilationException("bad literal type " + literalType + ", expected: " + paramType);
            }
        }
        if (!param.STRING_LITERAL().isEmpty()) {
            ClassUtils.visitStringLiteral(param.STRING_LITERAL(0), value -> visitor.visit(name, value));
        }
        if (param.IDENTIFIER().size() > 1) {
            visitor.visit(name, Type.getType(
                    scopeManager.resolveClass(scopeManager.resolveImport(param.IDENTIFIER(1).getText()))
                            .asTypeMeta()
                            .asDescriptor()));
        }
        if (!param.annotationDefinition().isEmpty()) {
            visitAnnotationDefinition(
                    param.annotationDefinition(0),
                    desc -> visitor.visitAnnotation(name, desc),
                    paramType);
        }
    }

    private void visitArrayParameter(
            String name, TypeMeta paramType,
            AnnotationVisitor visitor,
            SylectParser.AnnotationParameterContext param) {

        var arrayVisitor = visitor.visitArray(name);
        var elementType = paramType.arrayElementType();

        if (!param.LITERAL().isEmpty()) {
            param.LITERAL().forEach(ctx -> {
                var literalType = ClassUtils.visitLiteral(ctx, value -> arrayVisitor.visit(null, value));
                if (!literalType.equals(elementType)) {
                    throw new CompilationException("bad literal type " + literalType + ", expected: " + paramType);
                }
            });
        }
        if (!param.STRING_LITERAL().isEmpty()) {
            param.STRING_LITERAL().forEach(
                    ctx -> ClassUtils.visitStringLiteral(ctx, value -> arrayVisitor.visit(null, value)));
        }
        if (param.IDENTIFIER().size() > 1) {
            param.IDENTIFIER().forEach(ctx ->
                    visitor.visit(name, Type.getType(
                            scopeManager.resolveClass(scopeManager.resolveImport(ctx.getText()))
                                    .asTypeMeta()
                                    .asDescriptor())));
        }
        if (!param.annotationDefinition().isEmpty()) {
            param.annotationDefinition().forEach(ctx ->
                    visitAnnotationDefinition(
                            ctx,
                            desc -> arrayVisitor.visitAnnotation(null, desc),
                            elementType));
        }

        arrayVisitor.visitEnd();
    }
}
