package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class SimpleMark {
    private int line;
    private int column;

    public SimpleMark() {
    }
}