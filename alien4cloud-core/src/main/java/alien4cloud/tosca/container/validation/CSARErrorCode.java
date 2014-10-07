package alien4cloud.tosca.container.validation;

import lombok.Getter;

@Getter
@SuppressWarnings("PMD.UnusedPrivateField")
public enum CSARErrorCode {

    ERRONEOUS_ARCHIVE_FILE(CSARParsingError.class),
    DUPLICATED_DEFINITION_DECLARATION(CSARDuplicatedTypeError.class),
    DUPLICATED_ELEMENT_DECLARATION(CSARDuplicatedTypeError.class),
    ERRONEOUS_ICON_FILE(CSARParsingError.class),
    MISSING_ICON_FILE(CSARParsingError.class),
    ERRONEOUS_DEFINITION_FILE(CSARParsingError.class),
    MAPPING_ERROR_DEFINITION_FILE(CSARParsingError.class),
    UNRECOGNIZED_PROP_ERROR_DEFINITION_FILE(CSARUnrecognizedPropertyError.class),
    MISSING_DEFINITION_FILE(CSARParsingError.class),
    ERRONEOUS_METADATA_FILE(CSARParsingError.class),
    MAPPING_ERROR_METADATA_FILE(CSARParsingError.class),
    UNRECOGNIZED_PROP_ERROR_METADATA_FILE(CSARUnrecognizedPropertyError.class),
    MISSING_METADATA_FILE(CSARParsingError.class),
    TYPE_NOT_FOUND(CSARTypeNotFoundError.class),
    SUPER_TYPE_NOT_FOUND(CSARTypeNotFoundError.class);

    // used for deserialization.
    @SuppressWarnings("PMD.SingularField")
    private Class<?> correspondedErrorClass;

    private CSARErrorCode(Class<?> correspondedErrorClass) {
        this.correspondedErrorClass = correspondedErrorClass;
    }

    public static CSARErrorCode fromErrorCode(String errorCode) {
        for (CSARErrorCode type : values()) {
            if (type.toString().equals(errorCode)) {
                return type;
            }
        }
        return null;
    }
}