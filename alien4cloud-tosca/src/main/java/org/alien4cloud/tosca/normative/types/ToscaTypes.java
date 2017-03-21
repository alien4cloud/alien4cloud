package org.alien4cloud.tosca.normative.types;

import java.util.Date;
import java.util.Map;

import org.alien4cloud.tosca.normative.primitives.Frequency;
import org.alien4cloud.tosca.normative.primitives.Range;
import org.alien4cloud.tosca.normative.primitives.Size;
import org.alien4cloud.tosca.normative.primitives.Time;

import com.google.common.collect.Maps;

import alien4cloud.utils.version.Version;

/**
 * The primitive type that TOSCA YAML supports.
 * 
 * @author mkv
 */
public class ToscaTypes {
    public static final IPropertyType<Boolean> BOOLEAN_TYPE = new BooleanType();
    public static final IPropertyType<Long> INTEGER_TYPE = new IntegerType();
    public static final IPropertyType<Double> FLOAT_TYPE = new FloatType();
    public static final IPropertyType<String> STRING_TYPE = new StringType();
    public static final IPropertyType<Date> TIMESTAMP_TYPE = new TimestampType();
    public static final IPropertyType<Size> SIZE_TYPE = new SizeType();
    public static final IPropertyType<Frequency> FREQUENCY_TYPE = new FrequencyType();
    public static final IPropertyType<Time> TIME_TYPE = new TimeType();
    public static final IPropertyType<Version> VERSION_TYPE = new VersionType();
    // public static final IPropertyType<Range> RANGE_TYPE = new RangeType();

    public static final String BOOLEAN = BooleanType.NAME;
    public static final String INTEGER = IntegerType.NAME;
    public static final String FLOAT = FloatType.NAME;
    public static final String STRING = StringType.NAME;
    public static final String TIMESTAMP = TimestampType.NAME;
    public static final String SIZE = SizeType.NAME;
    public static final String TIME = TimeType.NAME;
    public static final String FREQUENCY = FrequencyType.NAME;
    public static final String VERSION = VersionType.NAME;
    // public static final String RANGE = RangeType.NAME;
    public static final String LIST = "list";
    public static final String MAP = "map";

    private static final Map<String, IPropertyType<?>> TYPES_MAP = Maps.newHashMap();

    static {
        TYPES_MAP.put(BOOLEAN, BOOLEAN_TYPE);
        TYPES_MAP.put(INTEGER, INTEGER_TYPE);
        TYPES_MAP.put(FLOAT, FLOAT_TYPE);
        TYPES_MAP.put(STRING, STRING_TYPE);
        TYPES_MAP.put(TIMESTAMP, TIMESTAMP_TYPE);
        TYPES_MAP.put(SIZE, SIZE_TYPE);
        TYPES_MAP.put(FREQUENCY, FREQUENCY_TYPE);
        TYPES_MAP.put(TIME, TIME_TYPE);
        TYPES_MAP.put(VERSION, VERSION_TYPE);
        // TYPES_MAP.put(RANGE, RANGE_TYPE);
    }

    public static IPropertyType<?> fromYamlTypeName(String typeName) {
        if (typeName == null) {
            return null;
        }
        return TYPES_MAP.get(typeName);
    }

    /**
     * A simple type is represented as a scalar in TOSCA yaml.
     */
    public static boolean isSimple(String typeName) {
        return TYPES_MAP.containsKey(typeName);
    }

    /**
     * Indicates if the type is a simple type or a list or a map (everything except custom data type).
     */
    public static boolean isPrimitive(String typeName) {
        return isSimple(typeName) || LIST.equals(typeName) || MAP.equals(typeName);
    }

}