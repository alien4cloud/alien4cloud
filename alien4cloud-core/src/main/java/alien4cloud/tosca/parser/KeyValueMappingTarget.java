package alien4cloud.tosca.parser;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KeyValueMappingTarget extends MappingTarget {
    private boolean keyPathRelativeToValue;
    private String keyPath;

    public KeyValueMappingTarget(String keyPath, boolean keyPathRelativeToValue, String path, INodeParser<?> parser) {
        super(path, parser);
        this.keyPathRelativeToValue = keyPathRelativeToValue;
        this.keyPath = keyPath;
    }
}