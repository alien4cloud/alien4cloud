package alien4cloud.paas.model;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import alien4cloud.paas.IPaaSTemplate;

@Getter
@Setter
public abstract class AbstractPaaSTemplate<V extends AbstractToscaType, T extends AbstractTemplate> implements IPaaSTemplate<V> {

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
