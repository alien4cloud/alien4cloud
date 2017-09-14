package alien4cloud.variable;

import lombok.Getter;

@Getter
class UnknownVariableException extends RuntimeException {
    private String variableName;

    UnknownVariableException(String variableName) {
        super(("variable '" + variableName + "' is unknown"));
        this.variableName = variableName;
    }
}
