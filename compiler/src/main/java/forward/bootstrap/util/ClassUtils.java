package forward.bootstrap.util;

import forward.bootstrap.CompilationException;
import org.objectweb.asm.Opcodes;

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
            default -> throw new CompilationException("unsupported target: " + target);
        };
    }
}
