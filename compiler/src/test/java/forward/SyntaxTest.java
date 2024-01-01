// SPDX-License-Identifier: MIT

package forward;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SyntaxTest extends AbstractCompilerTest {
    @Test
    public void syntaxTest() {
        testCompiler("forward.basic.ClassSyntax", "forward/basic/ClassSyntax.fw", (clazz, obj) -> {
            Assertions.assertTrue(obj instanceof Object);
            Assertions.assertTrue(obj instanceof AutoCloseable);

            var result = callMethod(obj, "testMethod", 5);
            Assertions.assertEquals(20, result);

            result = callMethod(obj, "staticTestMethod");
            Assertions.assertEquals(System.err, result);
        });
    }

    @Test
    public void interfaceTest() {
        testCompiler(
                "forward.basic.InterfaceSyntax", "forward/basic/InterfaceSyntax.fw",
                clazz -> Assertions.assertTrue(clazz.isInterface()));
    }
}
