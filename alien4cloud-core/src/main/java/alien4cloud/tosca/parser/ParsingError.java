package alien4cloud.tosca.parser;

import lombok.Getter;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Contains error informations relative to Yaml parsing.
 */
@Getter
public class ParsingError {
    private final ParsingErrorLevel errorLevel;
    private final ErrorCode errorCode;
    private final String context;
    private final SimpleMark startMark;
    private final String problem;
    private final SimpleMark endMark;
    private final String note;

    public ParsingError(ParsingErrorLevel errorLevel, ErrorCode errorCode, String context, Mark startMark, String problem, Mark endMark, String note) {
        this.errorLevel = errorLevel;
        this.errorCode = errorCode;
        this.context = context;
        this.startMark = startMark == null ? null : new SimpleMark(startMark.getLine(), startMark.getColumn());
        this.problem = problem;
        this.endMark = endMark == null ? null : new SimpleMark(endMark.getLine(), endMark.getColumn());
        this.note = note;
    }

    public ParsingError(ErrorCode errorCode, String context, Mark startMark, String problem, Mark endMark, String note) {
        this.errorLevel = ParsingErrorLevel.ERROR;
        this.errorCode = errorCode;
        this.context = context;
        this.startMark = startMark == null ? null : new SimpleMark(startMark.getLine(), startMark.getColumn());
        this.problem = problem;
        this.endMark = endMark == null ? null : new SimpleMark(endMark.getLine(), endMark.getColumn());
        this.note = note;
    }

    public ParsingError(ErrorCode errorCode, MarkedYAMLException cause) {
        this.errorLevel = ParsingErrorLevel.ERROR;
        this.errorCode = errorCode;
        this.context = cause.getContext();
        this.startMark = cause == null ? null : new SimpleMark(cause.getContextMark().getLine(), cause.getContextMark().getColumn());
        this.problem = cause.getProblem();
        this.endMark = cause == null ? null : new SimpleMark(cause.getProblemMark().getLine(), cause.getProblemMark().getColumn());
        this.note = null;
    }

    @Override
    public String toString() {
        return "Context: " + context + "\nProblem: " + problem + "\nNote: " + note + "\nStart: " + startMark + "\nEnd  : " + endMark;
    }
}