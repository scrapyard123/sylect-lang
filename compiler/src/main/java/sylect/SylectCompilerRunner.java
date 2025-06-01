// SPDX-License-Identifier: MIT

package sylect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sylect.bootstrap.BootstrapCompiler;
import sylect.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SylectCompilerRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SylectCompilerRunner.class);

    private static final String SOURCE_FILE_EXTENSION = ".sy";
    private static final String TARGET_ENV_VARIABLE = "JVM_VERSION";

    public static void compileSourceTrees(
            ClassLoader classLoader, int target,
            List<Path> sources, Path targetDir,
            Consumer<String> logger) {
        logger.accept("JVM Target: " + target);
        var compiler = new BootstrapCompiler(classLoader, target);

        logger.accept("Sources: " + sources);
        var sourceTrees = sources.stream()
                .parallel()
                .filter(Files::exists)
                .flatMap(source -> {
                    try {
                        return Files.walk(source)
                                .filter(path -> path.getFileName().toString().endsWith(SOURCE_FILE_EXTENSION))
                                .map(sourceFile -> {
                                    try {
                                        return new Pair<>(
                                                source.equals(sourceFile) ? sourceFile : source.relativize(sourceFile),
                                                compiler.generateTree(Files.readString(sourceFile)));
                                    } catch (IOException e) {
                                        throw new CompilationException("failed to read source: " + source, e);
                                    }
                                });
                    } catch (IOException e) {
                        throw new CompilationException("failed to walk source: " + source, e);
                    }
                })
                .toList();

        sourceTrees.stream()
                .parallel()
                .forEach(pair -> {
                    var sourceFile = pair.left();

                    var classFileName = sourceFile.getFileName().toString()
                            .replaceAll("(?i)\\" + SOURCE_FILE_EXTENSION + "$", ".class");
                    var classPath = sourceFile.getParent() == null ? targetDir : targetDir.resolve(sourceFile.getParent());
                    var classFilePath = classPath.resolve(classFileName);

                    try {
                        Files.createDirectories(classPath);

                        logger.accept("Compiling: " + sourceFile + " -> " + classFilePath);
                        Files.write(classFilePath, compiler.compile(pair.right()));
                    } catch (IOException e) {
                        throw new CompilationException("could not write: " + classFilePath);
                    }
                });
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            LOGGER.error("usage: sylect.SylectCompilerRunner DIR/FILE...");
            LOGGER.error("Use JVM_VERSION environment variable to control target JVM version");
            LOGGER.error("By default, target JVM version is the same as the version of JVM compiler runs on");
            System.exit(1);
        }

        var target = Integer.parseInt(
                System.getProperty(TARGET_ENV_VARIABLE, String.valueOf(SylectCompiler.DEFAULT_TARGET)));
        var pwd = System.getProperty("user.dir");

        compileSourceTrees(
                BootstrapCompiler.class.getClassLoader(),
                target,
                Arrays.stream(args).map(Paths::get).map(Path::toAbsolutePath).toList(),
                Paths.get(pwd).toAbsolutePath(),
                LOGGER::info);
    }
}
