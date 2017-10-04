package alien4cloud.utils;

import java.util.regex.Pattern;

import alien4cloud.exception.InvalidNameException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility for names validation
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class NameValidationUtils {

    /**
     * The default for naming resources in alien4cloud is to use only alphanumeric ann underscore (_) characters
     */
    public static final String DEFAULT_NAME_REGEX = "^\\w+$";
    public static final Pattern DEFAULT_NAME_REPLACE_PATTERN = Pattern.compile("\\W");

    public static final String APPLICATION_NAME_REGEX = "[^/\\\\]+";
    public static final String NODE_NAME_REGEX = DEFAULT_NAME_REGEX;

    private static boolean isValid(String name, String regex) {
        return Pattern.matches(regex, name);
    }

    /**
     * Checks if a given name is valid according to the regex {@link NameValidationUtils#DEFAULT_NAME_REGEX}
     * 
     * @param name The name to check for validation
     * @return
     */
    public static boolean isValid(String name) {
        return isValid(name, DEFAULT_NAME_REGEX);
    }

    private static void validate(String ownerKey, String name, String regex, String message) {
        if (!isValid(name, regex)) {
            if (log.isDebugEnabled()) {
                log.debug("{}: invalid name [ {} ]. Expects one matching the regex [ {} ]", ownerKey, name, regex);
            }
            throw new InvalidNameException(ownerKey, name, message);
        }
    }

    public static void validate(String ownerKey, String name) {
        String message = ownerKey + " <" + name + "> contains forbidden characters. Only alphanumeric and _ characters are allowed here.";
        validate(ownerKey, name, DEFAULT_NAME_REGEX, message);
    }

    /**
     * Validates the given application name, based on the regex {@link NameValidationUtils#APPLICATION_NAME_REGEX}
     *
     * @param name The name to validate
     */
    public static void validateApplicationName(String name) {
        String message = "Application name <" + name + "> contains forbidden character.";
        validate("applicationName", name, APPLICATION_NAME_REGEX, message);
    }

    /**
     * Validates the given application id, based on the regex {@link NameValidationUtils#DEFAULT_NAME_REGEX}
     *
     * @param id The id to validate
     */
    public static void validateApplicationId(String id) {
        String message = "Application Id <" + id + "> contains forbidden characters. Only alphanumeric and _ characters are allowed here.";
        validate("applicationId", id, DEFAULT_NAME_REGEX, message);
    }

    /**
     * Validates the given node name, based on the regex {@link NameValidationUtils#NODE_NAME_REGEX}
     *
     * @param name The name to validate
     */
    public static void validateNodeName(String name) {
        String message = "Nodetemplate name <" + name + "> contains forbidden characters. Only alphanumeric and _ characters are allowed here.";
        validate("nodetemplateName", name, NODE_NAME_REGEX, message);
    }

}
