// SPDX-License-Identifier: MIT

package sylect.bootstrap;

import sylect.SylectLexer;
import sylect.SylectParser;
import sylect.SylectParser.ProgramContext;
import sylect.bootstrap.metadata.ClassMeta;
import sylect.bootstrap.util.Pair;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class BootstrapCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapCompiler.class);

    private static final String SOURCE_FILE_EXTENSION = ".sy";
    private static final String TARGET_ENV_VARIABLE = "JVM_VERSION";
    private static final int DEFAULT_TARGET = 17;

    private final int target;
    private final ScopeManager scopeManager;

    public BootstrapCompiler() {
        this(BootstrapCompiler.class.getClassLoader(), DEFAULT_TARGET);
    }

    public BootstrapCompiler(ClassLoader classLoader, int target) {
        this.target = target;
        this.scopeManager = new ScopeManager(classLoader);
    }

    public byte[] compile(ProgramContext tree) {
        var walker = new ParseTreeWalker();

        var bytecodeTargetListener = new BytecodeTargetListener(target, scopeManager);
        walker.walk(bytecodeTargetListener, tree);
        return bytecodeTargetListener.getBytecode();
    }

    public ProgramContext generateTree(String source) {
        var errorListener = new ExceptionErrorListener();

        var lexer = new SylectLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        var tokenStream = new CommonTokenStream(lexer);
        var parser = new SylectParser(tokenStream);

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        var tree = parser.program();

        var partialClassMeta = ClassMeta.fromSylectTree(scopeManager, tree);
        scopeManager.addToSourceSet(partialClassMeta);

        return tree;
    }

    public static void compileSourceTrees(
            ClassLoader classLoader, int target,
            List<Path> sources, Path targetDir,
            Consumer<String> logger) {
        logger.accept("JVM Target: " + target);
        var compiler = new BootstrapCompiler(classLoader, target);

        logger.accept("Sources: " + sources);
        var sourceTrees = new ArrayList<Pair<Path, SylectParser.ProgramContext>>();
        for (var source : sources) {
            if (!Files.exists(source)) {
                logger.accept("Broken path: " + source);
                continue;
            }

            try (var fileStream = Files.walk(source)) {
                var sourceFileList = fileStream
                        .filter(path -> path.getFileName().toString().endsWith(SOURCE_FILE_EXTENSION))
                        .toList();
                for (var sourceFile : sourceFileList) {
                    sourceTrees.add(new Pair<>(
                            source.equals(sourceFile) ? sourceFile : source.relativize(sourceFile),
                            compiler.generateTree(Files.readString(sourceFile))));
                }
            } catch (IOException e) {
                throw new CompilationException("failed to parse sources", e);
            }
        }

        for (var pair : sourceTrees) {
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
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            LOGGER.error("usage: sylect.bootstrap.BootstrapCompiler DIR/FILE...");
            LOGGER.error("Use JVM_VERSION environment variable to control target JVM version (default is 17)");
            System.exit(1);
        }

        var target = Integer.parseInt(System.getProperty(TARGET_ENV_VARIABLE, String.valueOf(DEFAULT_TARGET)));
        var pwd = System.getProperty("user.dir");

        compileSourceTrees(
                BootstrapCompiler.class.getClassLoader(),
                target,
                Arrays.stream(args).map(Paths::get).map(Path::toAbsolutePath).toList(),
                Paths.get(pwd).toAbsolutePath(),
                LOGGER::info);
    }
}
