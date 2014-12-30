package alien4cloud.paas;

import java.nio.file.Path;

import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;

/**
 * Utility interface to operate a PaaSTemplate element.
 * 
 * @author luc boutier
 */
public interface IPaaSTemplate<V extends IndexedToscaElement> {
    /**
     * Set the indexed tosca element (type) for the PaaSTemplate {@link PaaSNodeTemplate} or {@link PaaSRelationshipTemplate}.
     * 
     * @param indexedToscaElement
     *            The indexed tosca element that represents the type of the template (it contains all the parent node informations and.
     */
    void setIndexedToscaElement(V indexedToscaElement);

    /**
     * Set the path of the CSAR that contains the related tosca element.
     * 
     * @param csarPath
     *            path of the CSAR that contains the related tosca element
     */
    void setCsarPath(Path csarPath);

    Path getCsarPath();
}