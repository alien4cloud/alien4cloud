package alien4cloud.tosca.parser.impl;

/**
 * Enumeration of error codes in TOSCA Parsing.
 */
public enum ErrorCode {
    /** File does not respect YAML format. */
    INVALID_YAML,
    /** The csar is already indexed in the repository with the exact same content. */
    CSAR_ALREADY_INDEXED,
    /** The csar already exists (and the version is not a SNAPSHOT version). */
    CSAR_ALREADY_EXISTS,
    /** The csar is already indexed in the repository in other workspace. */
    CSAR_ALREADY_EXISTS_IN_ANOTHER_WORKSPACE,
    /** The csar is used in an active deployment (It cannot be overrided). */
    CSAR_USED_IN_ACTIVE_DEPLOYMENT,
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
    /** Tosca version is not recognized by Alien 4 Cloud. */
    UNKNOWN_TOSCA_VERSION,
    /** Tosca version must be the first line of the TOSCA document. */
    TOSCA_VERSION_NOT_FIRST,
    /** A field is not recognized by Alien 4 Cloud and will be skipped. */
    UNRECOGNIZED_PROPERTY,
    /** A discriminator is not known and no failback parser. */
    UNKNWON_DISCRIMINATOR_KEY,
    /** A referenced file (definition, icon, artifact is missing). */
    MISSING_FILE,
    /** IO Error while reading a file. */
    FAILED_TO_READ_FILE,
    /** A TOSCA element specified in the archive already exists. */
    DUPLICATED_ELEMENT_DECLARATION,
    /** A referenced TOSCA type is missing. */
    TYPE_NOT_FOUND,
    /** A TOSCA type defines a derived from on a type that also derives from it. */
    CYCLIC_DERIVED_FROM,
    /** The icon format is not supported. */
    INVALID_ICON_FORMAT,
    /** Error in Alien Mapping */
    ALIEN_MAPPING_ERROR,
    /** Error in type validation (may be property constraints or missing mandatory property). */
    VALIDATION_ERROR,
    /** This constraint is not a valid TOSCA constraint. */
    UNKNOWN_CONSTRAINT,
    /** Constraint is known but invalid with other parameters **/
    INVALID_CONSTRAINT,
    /** An imported CSAR cannot be found. */
    MISSING_DEPENDENCY,
    /** Scalar unit declaration not well managed. */
    INVALID_SCALAR_UNIT,
    /** Detect a potential bad property value based on precedent inserted values **/
    POTENTIAL_BAD_PROPERTY_VALUE,
    /** Implementation artifact is unknown. */
    UNKNOWN_ARTIFACT_KEY, UNKNOWN_REPOSITORY, INVALID_ARTIFACT_REFERENCE, UNRESOLVED_ARTIFACT,
    /** A topology has been detected. */
    TOPOLOGY_DETECTED, TOPOLOGY_UPDATED,
    /** A property defined as get_input has an issue */
    MISSING_TOPOLOGY_INPUT, YAML_SEQUENCE_EXPECTED, YAML_MAPPING_NODE_EXPECTED, YAML_SCALAR_NODE_EXPECTED,
    UNKNOWN_CAPABILITY, REQUIREMENT_TARGET_NODE_TEMPLATE_NAME_REQUIRED, REQUIREMENT_NOT_FOUND, REQUIREMENT_TARGET_NOT_FOUND,
    REQUIREMENT_CAPABILITY_NOT_FOUND, OUTPUTS_BAD_PARAMS_COUNT, OUTPUTS_UNKNOWN_FUNCTION,
    UNKOWN_GROUP_POLICY, UNKOWN_GROUP_MEMBER,
    EMPTY_TOPOLOGY,
    UNKNWON_WORKFLOW_STEP, WORKFLOW_HAS_ERRORS,
    /** Invalid node template name (contains dot, dash or accent) **/
    INVALID_NODE_TEMPLATE_NAME;
}