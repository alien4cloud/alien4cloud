package alien4cloud.tosca.parser;

import lombok.Getter;
import lombok.Setter;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Contains error informations relative to Yaml parsing.
 */
@Getter
@Setter
public class ParsingError {
    private ParsingErrorLevel errorLevel;
    private ErrorCode errorCode;
    private String context;
    private SimpleMark startMark;
    private String problem;
    private SimpleMark endMark;
    private String note;

    public ParsingError() {
    }

    public ParsingError(ParsingErrorLevel errorLevel, ErrorCode errorCode, String context, Mark startMark, String problem, Mark endMark, String note) {
        this.errorLevel = errorLevel;
        this.errorCode = errorCode;
        this.context = context;
        this.startMark = startMark == null ? null : new SimpleMark(startMark);
        this.problem = problem;
        this.endMark = endMark == null ? null : new SimpleMark(endMark);
        this.note = note;
    }

    public ParsingError(ErrorCode errorCode, String context, Mark startMark, String problem, Mark endMark, String note) {
        this.errorLevel = ParsingErrorLevel.ERROR;
        this.errorCode = errorCode;
        this.context = context;
        this.startMark = startMark == null ? null : new SimpleMark(startMark);
        this.problem = problem;
        this.endMark = endMark == null ? null : new SimpleMark(endMark);
        this.note = note;
    }

    public ParsingError(ErrorCode errorCode, MarkedYAMLException cause) {
        this.errorLevel = ParsingErrorLevel.ERROR;
        this.errorCode = errorCode;
        this.context = cause.getContext();
        this.startMark = cause.getContextMark() == null ? null : new SimpleMark(cause.getContextMark());
        this.problem = cause.getProblem();
        this.endMark = cause.getContextMark() == null ? null : new SimpleMark(cause.getProblemMark());
        this.note = null;
    }

    @Override
    public String toString() {
        return "Context: " + context + "\nProblem: " + problem + "\nNote: " + note + "\nStart: " + startMark + "\nEnd  : " + endMark + "\nLevel: " + errorLevel + "\nCode : " + errorCode;
    }

}