package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ParsingResult<T> {
    private T result;
    private ParsingContext context;

    public ParsingResult() {
    }
}