package alien4cloud.it.utils.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data received from stomp connection
 * 
 * @author Minh Khang VU
 */
@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class StompData<T> {
    private String destination;
    private T data;
}
