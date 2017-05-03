package org.alien4cloud.tosca.editor.events;

import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;

import alien4cloud.events.AlienEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * Event triggered when a substitution type has changed.
 */
@Getter
@Setter
public class SubstitutionTypeChangedEvent extends AlienEvent {
    /** The topology that defines the substitution. */
    private Topology topology;
    /** The node type out of the substitution. */
    private NodeType substituteNodeType;

    /**
     * 
     * @param source Bean that dispatched the event.
     * @param topology The topology that defines the substitution.
     * @param substituteNodeType The node type out of the substitution.
     */
    public SubstitutionTypeChangedEvent(Object source, Topology topology, NodeType substituteNodeType) {
        super(source);
        this.topology = topology;
        this.substituteNodeType = substituteNodeType;
    }
}