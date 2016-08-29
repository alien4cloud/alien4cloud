package alien4cloud.tosca.parser.impl.advanced;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;

@Component
public class StringListParser implements INodeParser<List<String>> {
    @Resource
    private BaseParserFactory baseParserFactory;
    @Resource
    private ScalarParser scalarParser;

    private ListParser<String> listParser;

    @PostConstruct
    public void init() {
        listParser = baseParserFactory.getListParser(scalarParser, "string");
    }

    @Override
    public List<String> parse(Node node, ParsingContextExecution context) {
        return (List<String>) listParser.parse(node, context);
    }

}
