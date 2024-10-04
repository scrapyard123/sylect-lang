// SPDX-License-Identifier: MIT

package sylect.bootstrap;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import sylect.SylectCompiler;
import sylect.SylectLexer;
import sylect.SylectParser;
import sylect.SylectParser.ProgramContext;
import sylect.bootstrap.metadata.ClassMeta;

public class BootstrapCompiler implements SylectCompiler {

    private final int target;
    private final ScopeManager scopeManager;

    public BootstrapCompiler() {
        this(BootstrapCompiler.class.getClassLoader(), SylectCompiler.DEFAULT_TARGET);
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
}
