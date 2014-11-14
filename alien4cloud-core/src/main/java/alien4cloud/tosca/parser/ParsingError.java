package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Contains error informations relative to Yaml parsing.
 */
@Getter
@AllArgsConstructor
public class ParsingError {
    private final ParsingErrorLevel errorLevel;
    private final ErrorCode errorCode;
    private final String context;
    private final Mark startMark;
    private final String problem;
    private final Mark endMark;
    private final String note;

    public ParsingError(ErrorCode errorCode, String context, Mark startMark, String problem, Mark endMark, String note) {
        this.errorLevel = ParsingErrorLevel.ERROR;
        this.errorCode = errorCode;
        this.context = context;
        this.startMark = startMark;
        this.problem = problem;
        this.endMark = endMark;
        this.note = note;
    }

    public ParsingError(ErrorCode errorCode, MarkedYAMLException cause) {
        this.errorLevel = ParsingErrorLevel.ERROR;
        this.errorCode = errorCode;
        this.context = cause.getContext();
        this.startMark = cause.getContextMark();
        this.problem = cause.getProblem();
        this.endMark = cause.getProblemMark();
        this.note = null;
    }

    @Override
    public String toString() {
        return "Context: " + context + "\nProblem: " + problem + "\nNote: " + note + "\nStart: " + startMark + "\nEnd  : " + endMark;
    }
}