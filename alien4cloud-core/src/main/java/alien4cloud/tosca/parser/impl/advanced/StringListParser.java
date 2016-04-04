package alien4cloud.tosca.parser.impl.advanced;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultParser;

@Component
public class StringListParser extends DefaultParser<List<String>> {

    @Resource
    private ScalarParser scalarParser;

    private ListParser<String> listParser;

    @PostConstruct
    public void init() {
        listParser = new ListParser<String>(scalarParser, "string");
    }

    @Override
    public List<String> parse(Node node, ParsingContextExecution context) {
        return (List<String>) listParser.parse(node, context);
    }

}
