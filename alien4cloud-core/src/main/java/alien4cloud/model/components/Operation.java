package alien4cloud.model.components;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.json.deserializer.OperationParameterDeserializer;
import alien4cloud.ui.form.annotation.FormProperties;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Sets;

/**
 * Defines an operation available to manage particular aspects of the Node Type.
 *
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
@FormProperties({ "description" })
public class Operation {
    /** Implementation artifact for the interface. */
    private ImplementationArtifact implementationArtifact;
    /** Description of the operation. */
    private String description;
    /** This OPTIONAL property contains a list of one or more input parameter definitions. */
    @JsonDeserialize(contentUsing = OperationParameterDeserializer.class)
    private Map<String, IValue> inputParameters;

    /**
     * This OPTIONAL property contains a set of one or more outputs this operation execution might generate.
     * This is not part of TOSCA, and is populated when building the plan, based on the use of the get_operation_output function in the types definition
     */
    private Set<String> outputs = Sets.newHashSet();

    /**
     * <p>
     * Jackson DeSerialization workaround constructor to create an operation with no arguments.
     * </p>
     *
     * @param emptyString The empty string provided by jackson.
     */
    @SuppressWarnings("PMD.UnusedFormalParameterRule")
    public Operation(String emptyString) {
    }
}