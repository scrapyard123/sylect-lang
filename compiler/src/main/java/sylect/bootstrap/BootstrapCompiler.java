// SPDX-License-Identifier: MIT

package sylect.bootstrap;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import sylect.SylectCompiler;
import sylect.SylectLexer;
import sylect.SylectParser;
import sylect.SylectParser.ProgramContext;
import sylect.bootstrap.context.ClassMetaManager;
import sylect.bootstrap.metadata.ClassMeta;

public class BootstrapCompiler implements SylectCompiler {

    private final int target;
    private final ClassMetaManager classMetaManager;

    public BootstrapCompiler() {
        this(BootstrapCompiler.class.getClassLoader(), SylectCompiler.DEFAULT_TARGET);
    }

    public BootstrapCompiler(ClassLoader classLoader, int target) {
        this.target = target;
        this.classMetaManager = new ClassMetaManager(classLoader);
    }

    @Override
    public byte[] compile(ProgramContext tree) {
        var walker = new ParseTreeWalker();

        var bytecodeTargetListener = new BytecodeTargetListener(target, classMetaManager);
        walker.walk(bytecodeTargetListener, tree);
        return bytecodeTargetListener.getBytecode();
    }

    @Override
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

        var classMeta = ClassMeta.fromSylectTree(tree);
        classMetaManager.addToSourceSet(classMeta);

        return tree;
    }
}
