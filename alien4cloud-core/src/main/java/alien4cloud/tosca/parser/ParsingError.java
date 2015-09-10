package alien4cloud.tosca.parser;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Contains error informations relative to Yaml parsing.
 */
@Getter
@Setter
@Slf4j
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
        if (errorLevel == ParsingErrorLevel.ERROR) {
            // this is a handy place to put a breakpoint if you want to know where errors are coming from
            log.debug("Error found during parse, rethrowing:\n"+this);
        }
    }

    public ParsingError(ErrorCode errorCode, String context, Mark startMark, String problem, Mark endMark, String note) {
        this(ParsingErrorLevel.ERROR, errorCode, context, startMark, problem, endMark, note);
    }

    public ParsingError(ErrorCode errorCode, MarkedYAMLException cause) {
        this(ParsingErrorLevel.ERROR, errorCode, cause.getContext(), cause.getContextMark(), cause.getProblem(), cause.getProblemMark(), null);
    }

    @Override
    public String toString() {
        return "Context: " + context + "\nProblem: " + problem + "\nNote: " + note + "\nStart: " + startMark + "\nEnd  : " + endMark;
    }
}