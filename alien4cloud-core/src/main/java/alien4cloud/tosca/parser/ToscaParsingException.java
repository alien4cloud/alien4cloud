package alien4cloud.tosca.parser;

import java.util.List;

import lombok.Getter;
import alien4cloud.exception.FunctionalException;

import com.google.common.collect.Lists;

@Getter
public class ToscaParsingException extends FunctionalException {
    private static final long serialVersionUID = 1L;

    private final List<ToscaParsingError> parsingErrors;

    public ToscaParsingException(ToscaParsingError toscaParsingError) {
        super(toscaParsingError.toString());
        parsingErrors = Lists.newArrayList(toscaParsingError);
    }
    ToscaParsingException(List<ToscaParsingError> toscaParsingErrors) {
        super(toscaParsingErrors.toString());
        parsingErrors = toscaParsingErrors;
    }
}