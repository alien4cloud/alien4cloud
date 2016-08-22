package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
public class ParsingResult<T> {
    private T result;
    private ParsingContext context;

    public ParsingResult() {
    }

    /**
     * Checks if a given parsing result has any error.
     *
     * @param level The level of error to check. Null means any levels.
     * @return true if the parsing result as at least one error of the requested level.
     */
    public boolean hasError(ParsingErrorLevel level) {
        for (ParsingError error : context.getParsingErrors()) {
            if (level == null || level.equals(error.getErrorLevel())) {
                return true;
            }
        }

        return false;
    }
}