package alien4cloud.tosca.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.ui.form.annotation.FormProperties;

import com.google.common.collect.Maps;

/**
 * Definition of the operations that can be performed on (instances of) a Node Type.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@FormProperties({ "operations" })
public class Interface {
    private String description;
    /** Defines an operation available to manage particular aspects of the Node Type. */
    private Map<String, Operation> operations = Maps.newHashMap();
}