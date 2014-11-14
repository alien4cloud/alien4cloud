package alien4cloud.tosca.container.services.csar;

import java.util.Collection;
import java.util.Map;

import alien4cloud.component.model.IndexedInheritableToscaElement;
import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.tosca.container.model.CSARDependency;

public interface ICSARRepositoryIndexerService {

    void indexElements(String archiveName, String archiveVersion, Map<String, IndexedToscaElement> archiveElements);

    void indexInheritableElements(String archiveName, String archiveVersion, Map<String, ? extends IndexedInheritableToscaElement> archiveElements,
            Collection<CSARDependency> dependencies);

    void indexInheritableElement(String archiveName, String archiveVersion, IndexedInheritableToscaElement element, Collection<CSARDependency> dependencies);
}