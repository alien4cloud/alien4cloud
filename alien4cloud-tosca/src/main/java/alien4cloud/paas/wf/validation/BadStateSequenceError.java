package alien4cloud.paas.wf.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The steps are not in a correct order regarding constrains : 'from' should not be before 'to'.
 */
@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class BadStateSequenceError extends AbstractWorkflowError {
    private String from;
    private String to;

    @Override
    public String toString() {
        return "BadStateOrderError: <" + from + "> should be before <" + to + ">";
    }

}
