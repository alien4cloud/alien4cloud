package alien4cloud.tosca.parser;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.BeanWrapper;

@Getter
@Setter
public class ParsingContext {
    private BeanWrapper root;
    private final List<ToscaParsingError> parsingErrors;
    private final List<Runnable> defferedParsers;

    public ParsingContext(List<ToscaParsingError> parsingErrors, List<Runnable> defferedParsers) {
        this.parsingErrors = parsingErrors;
        this.defferedParsers = defferedParsers;
    }
}