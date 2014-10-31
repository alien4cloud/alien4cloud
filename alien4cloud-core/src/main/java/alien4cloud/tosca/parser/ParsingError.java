package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

/**
 * Contains error informations relative to Yaml parsing.
 */
@Getter
@AllArgsConstructor
public class ParsingError {
    private final ParsingErrorLevel errorLevel;
    private final String context;
    private final Mark contextMark;
    private final String problem;
    private final Mark problemMark;
    private final String note;

    public ParsingError(String context, Mark contextMark, String problem, Mark problemMark, String note) {
        this.context = context;
        this.contextMark = contextMark;
        this.problem = problem;
        this.problemMark = problemMark;
        this.note = note;
        this.errorLevel = ParsingErrorLevel.ERROR;
    }

    public ParsingError(MarkedYAMLException cause) {
        this.context = cause.getContext();
        this.contextMark = cause.getContextMark();
        this.problem = cause.getProblem();
        this.problemMark = cause.getProblemMark();
        this.note = null;
        this.errorLevel = ParsingErrorLevel.ERROR;
    }

    @Override
    public String toString() {
        return context + " - " + contextMark + "; " + problem + " - " + problemMark + " - " + note;
    }
}
