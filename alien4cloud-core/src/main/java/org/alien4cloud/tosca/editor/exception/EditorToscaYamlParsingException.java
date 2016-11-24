package org.alien4cloud.tosca.editor.exception;

import org.alien4cloud.tosca.model.Csar;

import alien4cloud.exception.TechnicalException;
import alien4cloud.tosca.parser.ParsingResult;
import lombok.Getter;

/**
 * Exception to be triggered if the user tries to update a topology from YAML file.
 */
public class EditorToscaYamlParsingException extends TechnicalException {

    @Getter
    private ParsingResult<Csar> parsingResult;

    public EditorToscaYamlParsingException(String message) {
        super(message);
    }

    public EditorToscaYamlParsingException(String message, ParsingResult<Csar> parsingResult) {
        super(message);
        this.parsingResult = parsingResult;
    }
}