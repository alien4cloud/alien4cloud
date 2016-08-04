package org.alien4cloud.tosca.editor.exception;

import alien4cloud.exception.TechnicalException;
import alien4cloud.tosca.parser.ParsingError;

import java.util.List;

/**
 * Exception to be triggered if the user tries to update a topology from YAML file.
 */
public class EditorToscaYamlUpdateException extends TechnicalException {
    private List<ParsingError> parsingErrors;

    public EditorToscaYamlUpdateException(String message) {
        super(message);
    }

    public EditorToscaYamlUpdateException(String message, List<ParsingError> parsingErrors) {
        super(message);
        this.parsingErrors = parsingErrors;
    }
}