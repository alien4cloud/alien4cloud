package utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@ToString
public class ExecReturn {
    private String[] resultLines = null;
    private String[] errorLines = null;
    private int returnCode;
}