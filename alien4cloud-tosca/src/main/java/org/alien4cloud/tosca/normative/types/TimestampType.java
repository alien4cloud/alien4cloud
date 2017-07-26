package org.alien4cloud.tosca.normative.types;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;

import alien4cloud.utils.YamlParserUtil;

public class TimestampType implements IComparablePropertyType<Date> {

    public static final String NAME = "timestamp";

    @Override
    public Date parse(String text) throws InvalidPropertyValueException {
        try {
            return YamlParserUtil.parse(text, Date.class);
        } catch (Exception e) {
            throw new InvalidPropertyValueException("Could not parse timestamp from value " + text, e);
        }
    }

    @Override
    public String print(Date value) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US).format(value);
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
