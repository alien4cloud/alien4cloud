package alien4cloud.var;

import lombok.ToString;

@ToString(callSuper = true)
public class VariableToken extends AbstractToken {

    public VariableToken(String value){
        super(value);
    }
}
