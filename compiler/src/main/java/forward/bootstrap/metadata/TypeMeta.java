// SPDX-License-Identifier: MIT

package forward.bootstrap.metadata;

import forward.ForwardParser.TypeContext;
import forward.bootstrap.ScopeManager;

public record TypeMeta(Kind kind, boolean isArray, String className) {
    public static TypeMeta fromContext(ScopeManager scopeManager, TypeContext ctx) {
        var typeString = ctx.getText();

        boolean isArray = false;
        if (typeString.endsWith("[]")) {
            isArray = true;
            typeString = typeString.substring(0, typeString.length() - 2);
        }

        var kind = switch (typeString) {
            case "void" -> Kind.VOID;
            case "int" -> Kind.INTEGER;
            case "long" -> Kind.LONG;
            case "float" -> Kind.FLOAT;
            case "double" -> Kind.DOUBLE;
            default -> Kind.CLASS;
        };
        return new TypeMeta(
                kind, isArray,
                kind == Kind.CLASS ? scopeManager.resolveImport(typeString) : null);
    }

    public static TypeMeta fromJavaType(Class<?> clazz) {
        if (clazz.equals(void.class)) {
            return new TypeMeta(Kind.VOID, false, null);
        }

        var isArray = clazz.isArray();
        if (isArray) {
            clazz = clazz.getComponentType();
        }

        if (clazz.equals(int.class)) {
            return new TypeMeta(Kind.INTEGER, isArray, null);
        }
        if (clazz.equals(long.class)) {
            return new TypeMeta(Kind.LONG, isArray, null);
        }
        if (clazz.equals(float.class)) {
            return new TypeMeta(Kind.FLOAT, isArray, null);
        }
        if (clazz.equals(double.class)) {
            return new TypeMeta(Kind.DOUBLE, isArray, null);
        }

        if (clazz.equals(boolean.class)) {
            return new TypeMeta(Kind.BOOLEAN, isArray, null);
        }
        if (clazz.equals(byte.class)) {
            return new TypeMeta(Kind.BYTE, isArray, null);
        }
        if (clazz.equals(char.class)) {
            return new TypeMeta(Kind.CHAR, isArray, null);
        }
        if (clazz.equals(short.class)) {
            return new TypeMeta(Kind.SHORT, isArray, null);
        }

        return new TypeMeta(Kind.CLASS, isArray, clazz.getCanonicalName());
    }

    public int getLocalSize() {
        return switch (kind) {
            case VOID -> 0;
            case LONG, DOUBLE -> 2;
            default -> 1;
        };
    }

    public String asDescriptor() {
        return (isArray ? "[" : "") + switch (kind) {
            case VOID -> "V";
            case INTEGER -> "I";
            case LONG -> "J";
            case FLOAT -> "F";
            case DOUBLE -> "D";
            case CLASS -> "L" + ClassMeta.javaClassFromClassName(className) + ";";

            case BOOLEAN -> "Z";
            case BYTE -> "B";
            case CHAR -> "C";
            case SHORT -> "S";
        };
    }

    public enum Kind {
        // Supported by Forward
        VOID, INTEGER, LONG, FLOAT, DOUBLE, CLASS,

        // Supported by Java
        BOOLEAN, BYTE, CHAR, SHORT
    }
}
