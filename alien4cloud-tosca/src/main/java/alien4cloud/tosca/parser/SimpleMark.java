package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.yaml.snakeyaml.error.Mark;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@ToString
public class SimpleMark {
    private int line;
    private int column;

    /** No args constructor for de-serialization. */
    public SimpleMark() {
    }

    public SimpleMark(Mark mark) {
        this.line = mark.getLine() + 1;
        this.column = mark.getColumn() + 1;
    }
}