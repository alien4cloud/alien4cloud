package alien4cloud.tosca.container.archive;

import javax.annotation.Resource;

import lombok.Getter;
import lombok.Setter;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.tosca.container.model.CloudServiceArchive;
import alien4cloud.tosca.container.services.csar.ICSARRepositorySearchService;

@Component
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class ArchiveValidator extends StandaloneArchiveValidator {

    @Resource
    private ICSARRepositorySearchService searchService;

    /**
     * Override in order to do not query Alien repository for existing elements
     */
    @Override
    protected boolean isElementExist(Class<? extends IndexedToscaElement> classToSearchFor, String element, CloudServiceArchive cloudServiceArchive) {
        return cloudServiceArchive.getArchiveInheritableElements().containsKey(element)
                || searchService.isElementExistInDependencies(classToSearchFor, element, cloudServiceArchive.getMeta().getDependencies());
    }
}
