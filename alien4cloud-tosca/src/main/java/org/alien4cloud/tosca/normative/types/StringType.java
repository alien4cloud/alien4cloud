package org.alien4cloud.tosca.normative.types;

/**
 * @author Minh Khang VU
 */
public class StringType implements IPropertyType<String> {

    public static final String NAME = "string";

    @Override
    public String parse(String text) {
        return text;
    }

    @Override
    public String print(String value) {
        return value;
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
