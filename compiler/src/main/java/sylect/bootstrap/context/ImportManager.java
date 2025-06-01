// SPDX-License-Identifier: MIT

package sylect.bootstrap.context;

import org.antlr.v4.runtime.tree.TerminalNode;
import sylect.SylectParser;
import sylect.bootstrap.metadata.ClassMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportManager {

    private final Map<String, String> imports = new HashMap<>();

    public void enterSource(SylectParser.ProgramContext ctx) {
        imports.putAll(
                Optional.ofNullable(ctx.importSection())
                        .map(SylectParser.ImportSectionContext::IDENTIFIER)
                        .map(imports -> Stream.concat(
                                        Stream.of(ctx.classDefinition().IDENTIFIER().getText()),
                                        imports.stream().map(TerminalNode::getText))
                                .collect(Collectors.toMap(ClassMeta::shortClassName, Function.identity())))
                        .orElse(Map.of()));
    }

    public String resolveImport(String identifier) {
        return imports.getOrDefault(identifier, identifier);
    }
}
