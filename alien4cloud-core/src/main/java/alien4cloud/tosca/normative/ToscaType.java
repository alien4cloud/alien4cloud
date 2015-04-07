package alien4cloud.tosca.normative;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * The primitive type that TOSCA YAML supports.
 * 
 * @author mkv
 */
public class ToscaType {

    public static final IPropertyType<?> BOOLEAN_TYPE = new BooleanType();
    public static final IPropertyType<?> INTEGER_TYPE = new IntegerType();
    public static final IPropertyType<?> FLOAT_TYPE = new FloatType();
    public static final IPropertyType<?> STRING_TYPE = new StringType();
    public static final IPropertyType<?> TIMESTAMP_TYPE = new TimestampType();
    public static final IPropertyType<?> SIZE_TYPE = new SizeType();
    public static final IPropertyType<?> TIME_TYPE = new TimeType();
    public static final IPropertyType<?> VERSION_TYPE = new VersionType();

    public static final String BOOLEAN = BooleanType.NAME;
    public static final String INTEGER = IntegerType.NAME;
    public static final String FLOAT = FloatType.NAME;
    public static final String STRING = StringType.NAME;
    public static final String TIMESTAMP = TimestampType.NAME;
    public static final String SIZE = SizeType.NAME;
    public static final String TIME = TimeType.NAME;
    public static final String VERSION = VersionType.NAME;

    private static final Map<String, IPropertyType<?>> TYPES_MAP = Maps.newHashMap();

    static {
        TYPES_MAP.put(BOOLEAN, BOOLEAN_TYPE);
        TYPES_MAP.put(INTEGER, INTEGER_TYPE);
        TYPES_MAP.put(FLOAT, FLOAT_TYPE);
        TYPES_MAP.put(STRING, STRING_TYPE);
        TYPES_MAP.put(TIMESTAMP, TIMESTAMP_TYPE);
        TYPES_MAP.put(SIZE, SIZE_TYPE);
        TYPES_MAP.put(TIME, TIME_TYPE);
        TYPES_MAP.put(VERSION, VERSION_TYPE);
    }

    public static IPropertyType<?> fromYamlTypeName(String typeName) {
        if (typeName == null) {
            return null;
        }
        return TYPES_MAP.get(typeName);
    }
}