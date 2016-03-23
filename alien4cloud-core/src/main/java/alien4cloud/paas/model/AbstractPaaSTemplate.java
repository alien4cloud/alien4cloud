package alien4cloud.paas.model;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.components.Interface;
import alien4cloud.model.topology.AbstractTemplate;
import alien4cloud.paas.IPaaSTemplate;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public abstract class AbstractPaaSTemplate<V extends IndexedToscaElement, T extends AbstractTemplate> implements IPaaSTemplate<V> {

    /** The unique id for the template within the topology. */
    private String id;

    /** The wrapped template. **/
    private T template;

    /**
     * The combination of interfaces inherited from type and those defined at template level.
     */
    private Map<String, Interface> interfaces;

    /** Type for the wrapped template. */
    private V indexedToscaElement;

    /** Derived from types **/
    private List<V> derivedFroms;

    public AbstractPaaSTemplate(String id, T template) {
        this.id = id;
        this.template = template;
    }

    @Override
    public T getTemplate() {
        return template;
    }
}
