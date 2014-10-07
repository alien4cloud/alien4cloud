package alien4cloud.rest.model;

/**
 * An error in the rest service
 * 
 * @author luc boutier
 */
public final class RestErrorBuilder {
    private RestError restError;

    public static RestErrorBuilder builder(RestErrorCode code) {
        return new RestErrorBuilder(code);
    }

    private RestErrorBuilder(RestErrorCode code) {
        restError = new RestError();
        restError.setCode(code.getCode());
    }

    public RestError build() {
        return restError;
    }

    public RestErrorBuilder message(String message) {
        restError.setMessage(message);
        return this;
    }
}