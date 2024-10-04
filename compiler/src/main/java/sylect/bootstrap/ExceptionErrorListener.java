// SPDX-License-Identifier: MIT

package sylect.bootstrap;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import sylect.CompilationException;

public class ExceptionErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        throw new CompilationException("line " + line + ":" + charPositionInLine + " " + msg);
    }
}
