package alien4cloud.ui.form.exception;

import alien4cloud.exception.TechnicalException;

/**
 * Exception happened while generating meta-model for construction of our form
 * 
 * @author mkv
 * 
 */
public class FormDescriptorGenerationException extends TechnicalException {

    private static final long serialVersionUID = -1780838094539642829L;

    public FormDescriptorGenerationException(String message) {
        super(message);
    }

    public FormDescriptorGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

}
