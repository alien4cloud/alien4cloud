package alien4cloud.tosca.parser;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import alien4cloud.tosca.parser.impl.ErrorCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Contains error information relative to Yaml parsing.
 */
@Getter
@Setter
@Slf4j
@ToString
@NoArgsConstructor
public class ParsingError {
    private ParsingErrorLevel errorLevel;
    private ErrorCode errorCode;
    private String context;
    private SimpleMark startMark;
    private String problem;
    private SimpleMark endMark;
    private String note;

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
            log.debug("Error found during parsing, rethrowing:\n" + this);
        }
    }

    public ParsingError(ErrorCode errorCode, String context, Mark startMark, String problem, Mark endMark, String note) {
        this(ParsingErrorLevel.ERROR, errorCode, context, startMark, problem, endMark, note);
    }

    /**
     * Create a new parsing error that is the same as the given parsing error but with a new level.
     * 
     * @param errorLevel The error level for the new error.
     * @param from The parsing error to clone.
     */
    public ParsingError(ParsingErrorLevel errorLevel, ParsingError from) {
        this.errorLevel = errorLevel;
        this.errorCode = from.errorCode;
        this.context = from.context;
        this.startMark = from.startMark;
        this.problem = from.problem;
        this.endMark = from.endMark;
        this.note = from.note;
    }

    public ParsingError(ErrorCode errorCode, MarkedYAMLException cause) {
        this(ParsingErrorLevel.ERROR, errorCode, cause.getContext(), cause.getProblemMark(), cause.getProblem(), cause.getContextMark(), null);
    }
}