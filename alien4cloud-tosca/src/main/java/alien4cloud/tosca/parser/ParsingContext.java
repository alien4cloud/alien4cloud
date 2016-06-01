package alien4cloud.tosca.parser;

import java.util.IdentityHashMap;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.google.common.collect.Lists;
import org.yaml.snakeyaml.nodes.Node;

@Getter
@Setter
public class ParsingContext {
    private String fileName;
    private List<ParsingError> parsingErrors = Lists.newArrayList();

    public ParsingContext() {
    }

    public ParsingContext(String fileName) {
        this.fileName = fileName;
    }
}