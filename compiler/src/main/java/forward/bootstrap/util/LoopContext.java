package forward.bootstrap.util;

import org.objectweb.asm.Label;

public record LoopContext(Label loopStart, Label thenBlock, Label otherCode) {
}
