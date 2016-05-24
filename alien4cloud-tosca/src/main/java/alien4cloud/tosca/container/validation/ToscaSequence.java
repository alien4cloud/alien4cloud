package alien4cloud.tosca.container.validation;

import javax.validation.GroupSequence;
import javax.validation.groups.Default;

/**
 * Defines the sequence for validation of tosca definitions.
 * 
 * @author luc boutier
 */
@GroupSequence({ Default.class, ToscaPropertyPostValidationGroup.class })
public interface ToscaSequence {
}