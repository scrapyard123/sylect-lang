// SPDX-License-Identifier: MIT

package forward;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SyntaxTest extends AbstractCompilerTest {
    @Test
    public void syntaxTest() {
        testCompiler("forward.basic.Syntax", "forward/basic/Syntax.fw", (clazz, obj) -> {
            Assertions.assertTrue(obj instanceof Object);

            var result = callMethod(obj, "testMethod", 5);
            Assertions.assertEquals(20, result);

            result = callMethod(obj, "staticTestMethod");
            Assertions.assertEquals(System.out, result);
        });
    }
}
