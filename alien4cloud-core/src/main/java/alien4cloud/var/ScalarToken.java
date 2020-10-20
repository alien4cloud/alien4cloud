package alien4cloud.var;

import lombok.ToString;


@ToString(callSuper = true)
public class ScalarToken extends AbstractToken {

    public ScalarToken(String value){
        super(value);
    }
}
