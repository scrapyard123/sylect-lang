// SPDX-License-Identifier: MIT

package forward.bootstrap;

import forward.ForwardLexer;
import forward.ForwardParser;
import forward.ForwardParser.ProgramContext;
import forward.bootstrap.metadata.ClassMeta;
import forward.bootstrap.util.Pair;
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
import java.util.Comparator;

public class BootstrapCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapCompiler.class);

    private static final String TARGET_ENV_VARIABLE = "JVM_VERSION";
    private static final int DEFAULT_TARGET = 17;

    private final int target;
    private final ScopeManager scopeManager;

    public BootstrapCompiler() {
        this(DEFAULT_TARGET);
    }

    public BootstrapCompiler(int target) {
        this.target = target;
        this.scopeManager = new ScopeManager();
    }

    public byte[] compile(ProgramContext tree) {
        var walker = new ParseTreeWalker();

        var bytecodeTargetListener = new BytecodeTargetListener(target, scopeManager);
        walker.walk(bytecodeTargetListener, tree);
        return bytecodeTargetListener.getBytecode();
    }

    public ProgramContext generateTree(String source) {
        var errorListener = new ExceptionErrorListener();

        var lexer = new ForwardLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        var tokenStream = new CommonTokenStream(lexer);
        var parser = new ForwardParser(tokenStream);

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        var tree = parser.program();

        var partialClassMeta = ClassMeta.fromForwardTree(scopeManager, tree);
        scopeManager.addToSourceSet(partialClassMeta);

        return tree;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            LOGGER.error("usage: forward.bootstrap.BootstrapCompiler SOURCE...");
            LOGGER.error("Use JVM_VERSION environment variable to control target JVM version (default is 17)");
            System.exit(1);
        }

        var target = Integer.parseInt(System.getProperty(TARGET_ENV_VARIABLE, String.valueOf(DEFAULT_TARGET)));

        var compiler = new BootstrapCompiler(target);
        var sourceTrees = new ArrayList<Pair<Path, ProgramContext>>();

        for (String arg : args) {
            var sourcePath = Paths.get(arg).toAbsolutePath();
            LOGGER.info("parsing {}", sourcePath);

            try {
                sourceTrees.add(new Pair<>(sourcePath, compiler.generateTree(Files.readString(sourcePath))));
            } catch (IOException e) {
                throw new CompilationException("could not read " + sourcePath);
            }
        }

        sourceTrees.forEach(pair -> {
            var sourceName = pair.left().getFileName().toString();
            var className = sourceName.replaceAll("(?i)\\.fw$", ".class");
            if (className.equals(sourceName)) {
                throw new CompilationException("class name equals source name: " + className);
            }

            var classPath = pair.left().getParent().resolve(className);
            LOGGER.info("compiling {}", classPath);
            try {
                Files.write(classPath, compiler.compile(pair.right()));
            } catch (IOException e) {
                throw new CompilationException("could not write " + classPath);
            }
        });
    }
}
