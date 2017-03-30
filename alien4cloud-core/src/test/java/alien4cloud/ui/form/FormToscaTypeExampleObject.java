package alien4cloud.ui.form;

import org.alien4cloud.tosca.normative.types.ToscaTypes;
import alien4cloud.ui.form.annotation.FormPropertyConstraint;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormToscaTypeExampleObject {

    @FormPropertyDefinition(
            type = ToscaTypes.VERSION,
            description = "Version of the component",
            constraints = @FormPropertyConstraint(
                    greaterThan = "1.6"
            ))
    private String versionField;

    @FormPropertyDefinition(
            type = ToscaTypes.INTEGER,
            description = "Integer of the component",
            constraints = @FormPropertyConstraint(
                    validValues = {
                            "1", "2", "3", "4"
                    }
            ))
    private int intField;

    @FormPropertyDefinition(
            type = ToscaTypes.STRING,
            description = "String of the component",
            constraints = @FormPropertyConstraint(
                    pattern = "\\d+"
            ))
    private String stringField;

    @FormPropertyDefinition(
            type = ToscaTypes.FLOAT,
            description = "All constraint of the component",
            constraints = @FormPropertyConstraint(
                    equal = "5",
                    greaterOrEqual = "6",
                    greaterThan = "7",
                    inRange = "[23,25]",
                    length = 7,
                    lessOrEqual = "7",
                    lessThan = "3",
                    maxLength = 9,
                    minLength = 4,
                    pattern = "\\d+",
                    validValues = { "4", "5", "6", "7" }
            ))
    private int allConstraintsField;
}
