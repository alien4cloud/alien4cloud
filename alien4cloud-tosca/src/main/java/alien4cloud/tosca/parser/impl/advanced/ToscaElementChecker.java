package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.IChecker;
import alien4cloud.tosca.parser.ParsingContextExecution;

@Component
public class ToscaElementChecker implements IChecker<IndexedToscaElement> {

    private static final String KEY = "toscaElementChecker";

    @Override
    public String getName() {
        return KEY;
    }

    @Override
    public void before(ParsingContextExecution context, Node node) {
    }

    @Override
    public void check(IndexedToscaElement instance, ParsingContextExecution context, Node node) {
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        instance.setArchiveName(archiveRoot.getArchive().getName());
        instance.setArchiveVersion(archiveRoot.getArchive().getVersion());
    }

}
