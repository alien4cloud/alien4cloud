package alien4cloud.ui.form;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.constraints.EqualConstraint;
import alien4cloud.model.components.constraints.GreaterOrEqualConstraint;
import alien4cloud.model.components.constraints.GreaterThanConstraint;
import alien4cloud.model.components.constraints.InRangeConstraint;
import alien4cloud.model.components.constraints.LengthConstraint;
import alien4cloud.model.components.constraints.LessOrEqualConstraint;
import alien4cloud.model.components.constraints.LessThanConstraint;
import alien4cloud.model.components.constraints.MaxLengthConstraint;
import alien4cloud.model.components.constraints.MinLengthConstraint;
import alien4cloud.model.components.constraints.PatternConstraint;
import alien4cloud.model.components.constraints.ValidValuesConstraint;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import alien4cloud.ui.form.exception.FormDescriptorGenerationException;

import com.google.common.collect.Lists;

@Component
public class PropertyDefinitionConverter {

    private static final Pattern IN_RANGE_REGEXP = Pattern.compile("\\[\\s*(\\S+)\\s*,\\s*(\\S+)\\s*\\]");

    public PropertyDefinition convert(FormPropertyDefinition definitionAnnotation) {
        if (definitionAnnotation == null) {
            return null;
        }
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(definitionAnnotation.type().toString());
        propertyDefinition.setDefault(definitionAnnotation.defaultValue());
        propertyDefinition.setDescription(definitionAnnotation.description());
        propertyDefinition.setPassword(definitionAnnotation.isPassword());
        propertyDefinition.setRequired(definitionAnnotation.isRequired());
        List<PropertyConstraint> constraints = Lists.newArrayList();
        if (!definitionAnnotation.constraints().equal().isEmpty()) {
            EqualConstraint equalConstraint = new EqualConstraint();
            equalConstraint.setEqual(definitionAnnotation.constraints().equal());
            constraints.add(equalConstraint);
        }
        if (!definitionAnnotation.constraints().greaterOrEqual().isEmpty()) {
            GreaterOrEqualConstraint greaterOrEqualConstraint = new GreaterOrEqualConstraint();
            greaterOrEqualConstraint.setGreaterOrEqual(definitionAnnotation.constraints().greaterOrEqual());
            constraints.add(greaterOrEqualConstraint);
        }
        if (!definitionAnnotation.constraints().greaterThan().isEmpty()) {
            GreaterThanConstraint greaterThanConstraint = new GreaterThanConstraint();
            greaterThanConstraint.setGreaterThan(definitionAnnotation.constraints().greaterThan());
            constraints.add(greaterThanConstraint);
        }
        if (!definitionAnnotation.constraints().inRange().isEmpty()) {
            String inRangeText = definitionAnnotation.constraints().inRange();
            Matcher matcher = IN_RANGE_REGEXP.matcher(inRangeText);
            if (matcher.matches()) {
                InRangeConstraint inRangeConstraint = new InRangeConstraint();
                inRangeConstraint.setRangeMinValue(matcher.group(1).trim());
                inRangeConstraint.setRangeMaxValue(matcher.group(2).trim());
                constraints.add(inRangeConstraint);
            } else {
                throw new FormDescriptorGenerationException("In range constraint definition must be in this format '[ $min - $max ]'");
            }
        }
        if (definitionAnnotation.constraints().length() >= 0) {
            LengthConstraint lengthConstraint = new LengthConstraint();
            lengthConstraint.setLength(definitionAnnotation.constraints().length());
            constraints.add(lengthConstraint);
        }
        if (!definitionAnnotation.constraints().lessOrEqual().isEmpty()) {
            LessOrEqualConstraint lessOrEqualConstraint = new LessOrEqualConstraint();
            lessOrEqualConstraint.setLessOrEqual(definitionAnnotation.constraints().lessOrEqual());
            constraints.add(lessOrEqualConstraint);
        }
        if (!definitionAnnotation.constraints().lessThan().isEmpty()) {
            LessThanConstraint lessThanConstraint = new LessThanConstraint();
            lessThanConstraint.setLessThan(definitionAnnotation.constraints().lessThan());
            constraints.add(lessThanConstraint);
        }
        if (definitionAnnotation.constraints().maxLength() >= 0) {
            MaxLengthConstraint maxLengthConstraint = new MaxLengthConstraint();
            maxLengthConstraint.setMaxLength(definitionAnnotation.constraints().maxLength());
            constraints.add(maxLengthConstraint);
        }
        if (definitionAnnotation.constraints().minLength() >= 0) {
            MinLengthConstraint minLengthConstraint = new MinLengthConstraint();
            minLengthConstraint.setMinLength(definitionAnnotation.constraints().minLength());
            constraints.add(minLengthConstraint);
        }
        if (!definitionAnnotation.constraints().pattern().isEmpty()) {
            PatternConstraint patternConstraint = new PatternConstraint();
            patternConstraint.setPattern(definitionAnnotation.constraints().pattern());
            constraints.add(patternConstraint);
        }
        if (definitionAnnotation.constraints().validValues().length > 0) {
            ValidValuesConstraint validValuesConstraint = new ValidValuesConstraint();
            validValuesConstraint.setValidValues(Lists.newArrayList(definitionAnnotation.constraints().validValues()));
            constraints.add(validValuesConstraint);
        }
        if (!constraints.isEmpty()) {
            propertyDefinition.setConstraints(constraints);
        }
        return propertyDefinition;
    }
}
