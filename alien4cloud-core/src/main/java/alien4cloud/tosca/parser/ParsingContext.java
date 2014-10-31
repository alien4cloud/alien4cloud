package alien4cloud.tosca.parser;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.BeanWrapper;

import com.google.common.collect.Lists;

@Getter
@Setter
public class ParsingContext {
    private BeanWrapper root;
    private final List<Runnable> defferedParsers = Lists.newArrayList();
    private final ParsingContextResult parsingContextResult;

    public ParsingContext(String fileName) {
        parsingContextResult = new ParsingContextResult(fileName);
    }

    public String getFileName() {
        return parsingContextResult.getFileName();
    }

    public List<ParsingError> getParsingErrors() {
        return parsingContextResult.getParsingErrors();
    }

    public List<ParsingResult<?>> getSubResults() {
        return parsingContextResult.getSubResults();
    }
}