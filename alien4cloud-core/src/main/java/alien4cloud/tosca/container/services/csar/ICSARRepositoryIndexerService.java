package alien4cloud.tosca.container.services.csar;

import java.util.Collection;
import java.util.Map;

import alien4cloud.tosca.container.model.CSARDependency;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.tosca.container.model.ToscaInheritableElement;

public interface ICSARRepositoryIndexerService {

    void indexElements(String archiveName, String archiveVersion, Map<String, ToscaElement> archiveElements);

    void indexInheritableElements(String archiveName, String archiveVersion, Map<String, ToscaInheritableElement> archiveElements,
            Collection<CSARDependency> dependencies);

    void indexInheritableElement(String archiveName, String archiveVersion, ToscaInheritableElement element, Collection<CSARDependency> dependencies);

    // TODO : This method should be removed once we manage correctly csar dependencies
    void indexInheritableElement(String archiveName, String archiveVersion, ToscaInheritableElement element);

    void deleteElement(String archiveName, String archiveVersion, ToscaElement element);
}