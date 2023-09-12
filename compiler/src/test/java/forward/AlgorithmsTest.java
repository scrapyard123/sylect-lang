// SPDX-License-Identifier: MIT

package forward;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AlgorithmsTest extends AbstractCompilerTest {
    @Test
    public void fibonacciIterativeTest() {
        testCompiler("forward.basic.Algorithms", "forward/basic/Algorithms.fw", (clazz, obj) -> {
            Assertions.assertTrue(obj instanceof Object);

            var negative = callMethod(obj, "fibIter", -10L);
            var zeroth = callMethod(obj, "fibIter", 0L);
            var first = callMethod(obj, "fibIter", 1L);
            var tenth = callMethod(obj, "fibIter", 10L);

            Assertions.assertEquals(-1L, negative);
            Assertions.assertEquals(0L, zeroth);
            Assertions.assertEquals(1L, first);
            Assertions.assertEquals(55L, tenth);
        });
    }

    @Test
    public void fibonacciRecursiveTest() {
        testCompiler("forward.basic.Algorithms", "forward/basic/Algorithms.fw", (clazz, obj) -> {
            Assertions.assertTrue(obj instanceof Object);

            var negative = callMethod(obj, "fibRec", -10L);
            var zeroth = callMethod(obj, "fibRec", 0L);
            var first = callMethod(obj, "fibRec", 1L);
            var tenth = callMethod(obj, "fibRec", 10L);

            Assertions.assertEquals(-1L, negative);
            Assertions.assertEquals(0L, zeroth);
            Assertions.assertEquals(1L, first);
            Assertions.assertEquals(55L, tenth);
        });
    }

    @Test
    public void bubbleSortTest() {
        testCompiler("forward.basic.Algorithms", "forward/basic/Algorithms.fw", (clazz, obj) -> {
            Assertions.assertTrue(obj instanceof Object);

            var list = new ArrayList<>(List.of(1, 2, 5, 3, 4, 5));
            var comparator = Comparator.comparingInt(i -> (int) i);
            callMethod(obj, "bubbleSort", list, comparator);

            Assertions.assertEquals(List.of(5, 5, 4, 3, 2, 1), list);
        });
    }
}
