package sylect.bootstrap.metadata.statement;

import org.objectweb.asm.Label;

public record LoopContext(Label loopStart, Label eachBlock, Label otherCode) {
}
