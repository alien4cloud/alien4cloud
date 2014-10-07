package alien4cloud.images.exception;

import alien4cloud.exception.TechnicalException;

public class ImageUploadException extends TechnicalException {

    private static final long serialVersionUID = 171917520842336653L;

    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageUploadException(String message) {
        super(message);
    }
}
