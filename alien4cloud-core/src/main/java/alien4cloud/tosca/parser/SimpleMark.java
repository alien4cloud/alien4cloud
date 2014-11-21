package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class SimpleMark {
    private int line;
    private int column;
}