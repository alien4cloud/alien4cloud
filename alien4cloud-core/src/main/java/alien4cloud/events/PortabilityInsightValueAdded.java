package alien4cloud.events;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PortabilityInsightValueAdded extends AlienEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private String portabilityKey;
    
    private Object portabilityValue;
    
    public PortabilityInsightValueAdded(Object source) {
        super(source);
    }
    
}
