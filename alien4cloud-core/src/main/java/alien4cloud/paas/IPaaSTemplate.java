package alien4cloud.paas;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;

/**
 * Utility interface to operate a PaaSTemplate element.
 *
 * @author luc boutier
 */
public interface IPaaSTemplate<V extends AbstractToscaType> {

    /**
     * Get the unique Id of the PaaSTemplate
     */
    String getId();

    /**
     * get the template (nodetemplate or relationship template) for the PaaSTemplate {@link PaaSNodeTemplate} or {@link PaaSRelationshipTemplate}.
     *
     * @return The AbstractTemplate bind to this PaasTemplate.
     */
    AbstractTemplate getTemplate();

    /**
     * Set the indexed tosca element (type) for the PaaSTemplate {@link PaaSNodeTemplate} or {@link PaaSRelationshipTemplate}.
     *
     * @param indexedToscaElement
     *            The indexed tosca element that represents the type of the template (it contains all the parent node informations and.
     */
    void setIndexedToscaElement(V indexedToscaElement);

    /**
     * Get derived from types
     * 
     * @return
     */
    List<V> getDerivedFroms();

    /**
     * Set derived from types
     * 
     * @param derivedFroms
     */
    void setDerivedFroms(List<V> derivedFroms);

    /**
     * Get the indexed tosca element (type) for the PaaSTemplate {@link PaaSNodeTemplate} or {@link PaaSRelationshipTemplate}.
     *
     * @return The indexed tosca element
     *
     */
    V getIndexedToscaElement();

    /**
     * Set the path of the CSAR that contains the related tosca element.
     *
     * @param csarPath
     *            path of the CSAR that contains the related tosca element
     */
    void setCsarPath(Path csarPath);

    /**
     * Get the path of the CSAR that contains the related tosca element.
     *
     * @return The Path
     */
    Path getCsarPath();

    Map<String, Interface> getInterfaces();
}