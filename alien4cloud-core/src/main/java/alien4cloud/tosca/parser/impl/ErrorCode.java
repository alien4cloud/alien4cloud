package alien4cloud.tosca.parser.impl;

/**
 * Enumeration of error codes in TOSCA Parsing.
 */
public enum ErrorCode {
    /** File does not respect YAML format. */
    INVALID_YAML,
    /** The csar already exists (and the version is not a SNAPSHOT version). */
    CSAR_ALREADY_EXISTS,
    /** Temporary error, currently Alien supports only a single definition file. */
    SINGLE_DEFINITION_SUPPORTED,
    /** In TOSCA Meta the entry definition was missing. */
    ENTRY_DEFINITION_NOT_FOUND,
    /** If the file cannot be opened as a zip or in case of any IO exception */
    ERRONEOUS_ARCHIVE_FILE,
    /** Invalid syntax while parsing a type. */
    SYNTAX_ERROR,
    /** Tosca version cannot be found in a definition file. */
    MISSING_TOSCA_VERSION,
    /** A field is not recognized by Alien 4 Cloud and will be skipped. */
    UNRECOGNIZED_PROPERTY,
    /** A referenced file (definition, icon, artifact is missing). */
    MISSING_FILE,
    /** IO Error while reading a file. */
    FAILED_TO_READ_FILE,
    /** A TOSCA element specified in the archive already exists. */
    DUPLICATED_ELEMENT_DECLARATION,
    /** A referenced TOSCA type is missing. */
    TYPE_NOT_FOUND,
    /** The icon format is not supported. */
    INVALID_ICON_FORMAT,
    /** Error in Alien Mapping */
    ALIEN_MAPPING_ERROR,
    /** Error in type validation (may be property constraints or missing mandatory property). */
    VALIDATION_ERROR,
    /** This constraint is not a valid TOSCA constraint. */
    UNKNOWN_CONSTRAINT,
    /** An imported CSAR cannot be found. */
    MISSING_DEPENDENCY,
    /** Scalar unit declaration not well managed. */
    INVALID_SCALAR_UNIT,
    /** Implementation artifact is unknown. */
    UNKNOWN_IMPLEMENTATION_ARTIFACT,
    /** A topology has been detected. */
    TOPOLOGY_DETECTED,
    TOPOLOGY_UPDATED,
    /** A property defined as get_input has an issue */
    MISSING_TOPOLOGY_INPUT,
    YAML_SEQUENCE_EXPECTED,
    YAML_MAPPING_NODE_EXPECTED,
    YAML_SCALAR_NODE_EXPECTED,
    UNKNOWN_CAPABILITY,
    REQUIREMENT_TARGET_NODE_TEMPLATE_NAME_REQUIRED,
    RELATIONSHIP_NOT_BUILT,
    REQUIREMENT_NOT_FOUND,
    REQUIREMENT_TARGET_NOT_FOUND,
    REQUIREMENT_CAPABILITY_NOT_FOUND,
    OUTPUTS_BAD_PARAMS_COUNT,
    OUTPUTS_UNKNOWN_FUNCTION,
    UNKOWN_GROUP_POLICY,
    UNKOWN_GROUP_MEMBER,
    EMPTY_TOPOLOGY;
}