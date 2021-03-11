package alien4cloud.var;

import lombok.ToString;

@ToString(callSuper = true)
public class VariableToken extends InputToken {

    public VariableToken(String value){
        super(value);
    }
}
