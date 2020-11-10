package alien4cloud.var;

import lombok.ToString;

@ToString(callSuper = true)
public class InputToken extends AbstractToken {
    public InputToken(String value){
        super(value);
    }
}
