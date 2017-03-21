package org.alien4cloud.tosca.exceptions;

import alien4cloud.exception.FunctionalException;

/**
 * This exception is thrown when Alien cannot deserialize text into property value
 * 
 * @author Minh Khang VU
 */
public class InvalidPropertyValueException extends FunctionalException {

    public InvalidPropertyValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPropertyValueException(String message) {
        super(message);
    }
}
