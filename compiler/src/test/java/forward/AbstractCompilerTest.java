// SPDX-License-Identifier: MIT

package forward;

import forward.bootstrap.BootstrapCompiler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

abstract public class AbstractCompilerTest {
    protected BootstrapCompiler compiler;

    @BeforeEach
    public void setUp() {
        compiler = new BootstrapCompiler();
    }

    protected void testCompiler(String name, String fileName, Consumer<Class<?>> tester) {
        try (var is = this.getClass().getClassLoader().getResourceAsStream(fileName)) {
            var source = new String(is.readAllBytes());
            var tree = compiler.generateTree(source);
            var bytecode = compiler.compile(tree);
            var localClassLoader = new ClassLoader() {
                public Class<?> getCompiledClass() {
                    return defineClass(name, bytecode, 0, bytecode.length);
                }
            };
            var clazz = localClassLoader.getCompiledClass();
            tester.accept(clazz);
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    protected void testCompiler(String name, String fileName, BiConsumer<Class<?>, Object> tester) {
        testCompiler(name, fileName, clazz -> {
            try {
                var obj = clazz.getConstructor().newInstance();
                tester.accept(clazz, obj);
            } catch (Exception e) {
                Assertions.fail(e);
            }
        });
    }

    protected Object callMethod(Object obj, String name, Object... args) {
        var methodOptional = Arrays.stream(obj.getClass().getMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst();

        if (methodOptional.isEmpty()) {
            Assertions.fail("no such method: " + name);
            return null;
        }
        var method = methodOptional.get();

        try {
            return method.invoke(obj, args);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail("failed to call method: " + name, e);
            return null;
        }
    }
}
