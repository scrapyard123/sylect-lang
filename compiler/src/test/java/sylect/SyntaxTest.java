// SPDX-License-Identifier: MIT

package sylect;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SyntaxTest extends AbstractCompilerTest {
    @Test
    public void syntaxTest() {
        testCompiler("sylect.basic.ClassSyntax", "sylect/basic/ClassSyntax.sy", (clazz, obj) -> {
            Assertions.assertTrue(obj instanceof Object);
            Assertions.assertTrue(obj instanceof AutoCloseable);

            var result = callMethod(obj, "testMethod", 5, 0);
            Assertions.assertEquals(38, result);

            result = callMethod(obj, "staticTestMethod");
            Assertions.assertEquals(System.err, result);
        });
    }

    @Test
    public void interfaceTest() {
        testCompiler(
                "sylect.basic.InterfaceSyntax", "sylect/basic/InterfaceSyntax.sy",
                clazz -> Assertions.assertTrue(clazz.isInterface()));
    }
}
