package alien4cloud.tosca;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedInheritableToscaElement;

@Component
public class ToscaService {

    public boolean isOfType(String type, IndexedInheritableToscaElement toscaElement) {
        return type.equals(toscaElement.getElementId()) || (toscaElement.getDerivedFrom() != null && toscaElement.getDerivedFrom().contains(type));
    }
}
