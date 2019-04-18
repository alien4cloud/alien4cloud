package org.alien4cloud.tosca.model.definitions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Sets;

import alien4cloud.json.deserializer.OperationParameterDeserializer;
import alien4cloud.ui.form.annotation.FormProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines an operation available to manage particular aspects of the Node Type.
 *
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
@FormProperties({ "description" })
public class Operation {
    /** Implementation artifact for the interface. */
    private ImplementationArtifact implementationArtifact;
    /** List of artifacts required by the implementation artifact. */
    private List<DeploymentArtifact> dependencies;
    /** Description of the operation. */
    private String description;
    /** This OPTIONAL property contains a list of one or more input parameter definitions. */
    @JsonDeserialize(contentUsing = OperationParameterDeserializer.class)
    private Map<String, IValue> inputParameters;
    /** This element is not part of TOSCA but allows to specifies some portability meta-data on the operation. */
    private Map<String, AbstractPropertyValue> portability;

    /** operationHost for this operation  */
    private String operationHost;

    /**
     * This OPTIONAL property contains a map of one or more outputs this operation execution might generate.
     * This is not part of TOSCA, and is populated when building the plan, based on the use of the get_operation_output function in the types definition
     */
    @JsonIgnore
    private Set<OperationOutput> outputs = Sets.newHashSet();

    /**
     * Jackson DeSerialization workaround constructor to create an operation with no arguments.
     * 
     * @param emptyString The empty string provided by jackson.
     */
    @SuppressWarnings("PMD.UnusedFormalParameterRule")
    public Operation(String emptyString) {
    }

    /**
     * Create an operation from an implementation artifact.
     * 
     * @param implementationArtifact The operation's implementation artifact.
     */
    public Operation(ImplementationArtifact implementationArtifact) {
        this.implementationArtifact = implementationArtifact;
    }

    @JsonIgnore
    public OperationOutput getOutput(String name) {
        OperationOutput toFind = new OperationOutput(name);
        for (OperationOutput output : outputs) {
            if (output.equals(toFind)) {
                return output;
            }
        }
        return null;
    }

    /**
     * add an output, merge if needed the related attributes
     *
     * @param output
     */
    public void addOutput(OperationOutput output) {
        if (outputs.contains(output) && CollectionUtils.isNotEmpty(output.getRelatedAttributes())) {
            // merge related attributes
            getOutput(output.getName()).getRelatedAttributes().addAll(output.getRelatedAttributes());
        } else {
            outputs.add(output);
        }
    }
}
