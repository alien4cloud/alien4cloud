package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

/**
 * Contains error informations relative to TOSCA parsing.
 */
@Getter
@AllArgsConstructor
public class ToscaParsingError {
    private final boolean isBlocker;
    private final String definitionFileName;
    private final String context;
    private final Mark contextMark;
    private final String problem;
    private final Mark problemMark;
    private final String note;

    public ToscaParsingError(String definitionFileName, String context, Mark contextMark, String problem, Mark problemMark, String note) {
        this.definitionFileName = definitionFileName;
        this.context = context;
        this.contextMark = contextMark;
        this.problem = problem;
        this.problemMark = problemMark;
        this.note = note;
        this.isBlocker = true;
    }

    public ToscaParsingError(String definitionFileName, MarkedYAMLException cause) {
        this.definitionFileName = definitionFileName;
        this.context = cause.getContext();
        this.contextMark = cause.getContextMark();
        this.problem = cause.getProblem();
        this.problemMark = cause.getProblemMark();
        this.note = null;
        this.isBlocker = true;
    }

    @Override
    public String toString() {
        return context + " - " + contextMark + "; " + problem + " - " + problemMark + " - " + note;
    }
}
