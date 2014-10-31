package alien4cloud.tosca.parser;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.google.common.collect.Lists;

@Getter
@Setter
public class ParsingContextResult {
    private final String fileName;
    private final List<ParsingError> parsingErrors = Lists.newArrayList();
    /** If parsing triggers parsing of other related yaml files. */
    private final List<ParsingContextResult> subContexts = Lists.newArrayList();

    public ParsingContextResult(String fileName) {
        this.fileName = fileName;
    }
}