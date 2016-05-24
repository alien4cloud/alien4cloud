package alien4cloud.tosca.parser;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.google.common.collect.Lists;

@Getter
@Setter
public class ParsingContext {
    private String fileName;
    private List<ParsingError> parsingErrors = Lists.newArrayList();

    // TODO manage sub archives
    /** If parsing triggers parsing of other related yaml files. */
    // private List<ParsingResult<?>> subResults = Lists.newArrayList();

    public ParsingContext() {
    }

    public ParsingContext(String fileName) {
        this.fileName = fileName;
    }
}