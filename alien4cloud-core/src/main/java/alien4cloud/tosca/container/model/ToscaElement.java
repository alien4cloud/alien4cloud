package alien4cloud.tosca.container.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Parent class for every tosca element.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public abstract class ToscaElement {
    private String id;
    private String description;
    private Map<String, String> tags;
}