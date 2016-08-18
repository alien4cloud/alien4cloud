package alien4cloud.component;

import alien4cloud.model.components.IndexedToscaElement;

public interface IToscaElementFinder {

    <T extends IndexedToscaElement> T findElement(Class<T> elementClass, String elementId);

}
