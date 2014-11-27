package alien4cloud.tosca.parser;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.BeanWrapper;

import com.google.common.collect.Lists;

@Getter
@Setter
public class ParsingContextExecution {
    private BeanWrapper root;
    private final List<Runnable> deferredParsers = Lists.newArrayList();
    private final ParsingContext parsingContext;
    /** Map of parsers by type */
    private Map<String, INodeParser> registry;

    public ParsingContextExecution(String fileName) {
        parsingContext = new ParsingContext(fileName);
    }

    public String getFileName() {
        return parsingContext.getFileName();
    }

    public List<ParsingError> getParsingErrors() {
        return parsingContext.getParsingErrors();
    }
}