package alien4cloud.component;

import java.util.Collection;

import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedToscaElement;

public interface IToscaElementFinder {

    <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies);

}
