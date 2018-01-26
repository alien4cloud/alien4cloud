package org.alien4cloud.tosca.model.definitions;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.alien4cloud.tosca.normative.ToscaNormativeUtil;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import alien4cloud.ui.form.annotation.FormProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A TOSCA function to be used as the value for a property (or operation parameter).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FormProperties({ "function", "parameters" })
public class FunctionPropertyValue extends AbstractPropertyValue {
    private String function;

    private List<String> parameters;

    /**
     * Get the modelable entity's (node or relationship template) name related to the function, represented by the first parameter.
     */
    @JsonIgnore
    public String getTemplateName() {
        return parameters.get(0);
    }

    /**
     * get the name of the property or attribute or the output we want to retrieve, represented by the last parameter in the list
     */
    @JsonIgnore
    public String getElementNameToFetch() {
        return parameters.get(parameters.size() - 1);
    }

    /**
     * Get, if provided, the capability/requirement name within the template which contains the prop or attr to retrieve
     */
    @JsonIgnore
    public String getCapabilityOrRequirementName() {
        if (function != null && parameters.size() > 2) {
            switch (function) {
            case ToscaFunctionConstants.GET_PROPERTY:
            case ToscaFunctionConstants.GET_ATTRIBUTE:
            case ToscaFunctionConstants.GET_INPUT:
                return parameters.get(1);
            default:
                return null;
            }
        }
        return null;
    }

    /**
     * Get, in case of get_operation_output, the name of the interface related to the function, represented by the second parameter in the list
     */
    @JsonIgnore
    public String getInterfaceName() {
        if (function != null) {
            switch (function) {
            case ToscaFunctionConstants.GET_OPERATION_OUTPUT:
                return parameters.size() > 2 ? ToscaNormativeUtil.getLongInterfaceName(parameters.get(1)) : null;
            default:
                return null;
            }
        }
        return null;
    }

    /**
     * Get, in case of get_operation_output, the name of the operation related to the function, represented by the third parameter in the list
     */
    @JsonIgnore
    public String getOperationName() {
        if (function != null) {
            switch (function) {
            case ToscaFunctionConstants.GET_OPERATION_OUTPUT:
                return parameters.size() > 3 ? parameters.get(2) : null;
            default:
                return null;
            }
        }
        return null;
    }

    public void replaceAllParamsExceptTemplateNameWith(String... replacements) {
        if (parameters.size() > 0) {
            parameters.subList(1, parameters.size()).clear();
        } else {
            parameters.set(0, "");
        }
        for (String string : replacements) {
            parameters.add(string);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FunctionPropertyValue that = (FunctionPropertyValue) o;

        if (function != null ? !function.equals(that.function) : that.function != null) return false;
        return parameters != null ? parameters.equals(that.parameters) : that.parameters == null;
    }

    @Override
    public int hashCode() {
        int result = function != null ? function.hashCode() : 0;
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }
}