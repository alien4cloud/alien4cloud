package alien4cloud.dao.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result for a multiple data query.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class GetMultipleDataResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private String[] types;
    private T[] data;
    private long queryDuration;
    private long totalResults;
    private int from;
    private int to;

    /**
     * Construct an object only with data and types
     * 
     * @param types
     * @param data
     */
    public GetMultipleDataResult(String[] types, T[] data) {
        this.types = types;
        this.data = data;
    }
}