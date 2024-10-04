package sylect.bootstrap.util;

import sylect.CompilationException;
import sylect.bootstrap.metadata.TypeMeta;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.Opcodes;

import java.util.function.Consumer;

public final class ClassUtils {
    private ClassUtils() {
    }

    public static int getVersion(int target) {
        return switch (target) {
            case 5 -> Opcodes.V1_5;
            case 6 -> Opcodes.V1_6;
            case 7 -> Opcodes.V1_7;
            case 8 -> Opcodes.V1_8;
            case 9 -> Opcodes.V9;
            case 10 -> Opcodes.V10;
            case 11 -> Opcodes.V11;
            case 12 -> Opcodes.V12;
            case 13 -> Opcodes.V13;
            case 14 -> Opcodes.V14;
            case 15 -> Opcodes.V15;
            case 16 -> Opcodes.V16;
            case 17 -> Opcodes.V17;
            case 18 -> Opcodes.V18;
            case 19 -> Opcodes.V19;
            case 20 -> Opcodes.V20;
            case 21 -> Opcodes.V21;
            case 22 -> Opcodes.V22;
            default -> throw new CompilationException("unsupported target: " + target);
        };
    }

    public static TypeMeta visitLiteral(TerminalNode literalNode, Consumer<Object> block) {
        var kind = (TypeMeta.Kind) null;
        var className = (String) null;

        var literal = literalNode.getText();
        if (literal.startsWith("\"") && literal.endsWith("\"")) {
            kind = TypeMeta.Kind.CLASS;
            className = "java.lang.String";
            literal = literal.substring(1, literal.length() - 1);
        } else if (literal.endsWith("F")) {
            kind = TypeMeta.Kind.FLOAT;
            literal = literal.substring(0, literal.length() - 1);
        } else if (literal.endsWith("L")) {
            kind = TypeMeta.Kind.LONG;
            literal = literal.substring(0, literal.length() - 1);
        } else if (literal.contains(".")) {
            kind = TypeMeta.Kind.DOUBLE;
        } else {
            kind = TypeMeta.Kind.INTEGER;
        }

        switch (kind) {
            case INTEGER -> block.accept(Integer.parseInt(literal));
            case LONG -> block.accept(Long.parseLong(literal));
            case FLOAT -> block.accept(Float.parseFloat(literal));
            case DOUBLE -> block.accept(Double.parseDouble(literal));
            case CLASS -> block.accept(literal);
            default -> throw new CompilationException("unsupported literal type: " + kind);
        }

        return new TypeMeta(kind, false, className);
    }
}
