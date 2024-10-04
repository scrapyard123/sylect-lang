// SPDX-License-Identifier: MIT

package sylect;

/**
 * Interface for Sylect programming language compilers.
 */
public interface SylectCompiler {

    /**
     * By default, target the same JVM version as we are running on.
     */
    int DEFAULT_TARGET = Runtime.version().feature();

    /**
     * Generate AST from provided source string.
     *
     * @param source source code in Sylect
     * @return AST
     */
    SylectParser.ProgramContext generateTree(String source);

    /**
     * Compile AST to class file.
     *
     * @param tree AST
     * @return class file as byte array
     */
    byte[] compile(SylectParser.ProgramContext tree);
}
