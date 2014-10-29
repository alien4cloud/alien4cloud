package alien4cloud.tosca.parser;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.model.ArchiveRoot;

@Getter
@Setter
@AllArgsConstructor
public class ParsingResult {
    private ArchiveRoot archiveRoot;
    private List<ToscaParsingError> parsingErrors;
}