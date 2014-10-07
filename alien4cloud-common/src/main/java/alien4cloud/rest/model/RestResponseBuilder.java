package alien4cloud.rest.model;

/**
 * Utility to build {@link RestResponse}.
 * 
 * @author luc boutier
 */
public final class RestResponseBuilder<T> {
    private RestResponse<T> response;

    private RestResponseBuilder() {
        response = new RestResponse<T>();
    }

    public static <T> RestResponseBuilder<T> builder() {
        return new RestResponseBuilder<T>();
    }

    public RestResponse<T> build() {
        return response;
    }

    public RestResponseBuilder<T> data(T data) {
        response.setData(data);
        return this;
    }

    public RestResponseBuilder<T> error(RestError error) {
        response.setError(error);
        return this;
    }
}
