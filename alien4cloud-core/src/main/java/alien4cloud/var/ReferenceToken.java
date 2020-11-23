package alien4cloud.var;

import lombok.ToString;

@ToString(callSuper = true)
public class ReferenceToken extends InputToken {

    public ReferenceToken(String value){
        super(value);
    }
}
