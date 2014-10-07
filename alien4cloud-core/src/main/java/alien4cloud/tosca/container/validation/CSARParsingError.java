package alien4cloud.tosca.container.validation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CSARParsingError extends CSARError {
    private int lineNr;
    private int colNr;

    public CSARParsingError(int lineNr, int colNr, String errorCode, String message) {
        super(errorCode, message);
        this.lineNr = lineNr;
        this.colNr = colNr;
    }
}