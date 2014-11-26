package alien4cloud.tosca.parser;

import lombok.Getter;

@Getter
public class MappingTarget {
    private boolean isRootPath;
    private boolean asCollection = false;
    private String path;
    private INodeParser<?> parser;

    public MappingTarget(String path, INodeParser<?> parser) {
        this.isRootPath = path.startsWith("/");
        if (isRootPath) {
            this.path = path.substring(1);
        } else {
            this.path = path;
        }
        this.parser = parser;
    }

    public MappingTarget(String path, INodeParser<?> parser, boolean asCollection) {
        this.isRootPath = path.startsWith("/");
        if (isRootPath) {
            this.path = path.substring(1);
        } else {
            this.path = path;
        }
        this.parser = parser;
        this.asCollection = asCollection;
    }
}