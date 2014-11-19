package alien4cloud.tosca;

import alien4cloud.component.model.IndexedInheritableToscaElement;

public class ToscaUtils {

    private ToscaUtils() {
    }

    /**
     * Verify that the given {@link IndexedInheritableToscaElement} is from the given type.
     *
     * @param indexedInheritableToscaElement The {@link IndexedInheritableToscaElement} to verify.
     * @param type The type to match
     * @return <code>true</code> if the {@link IndexedInheritableToscaElement} is from the given type.
     */
    public static boolean isFromType(String type, IndexedInheritableToscaElement indexedInheritableToscaElement) {
        return type.equals(indexedInheritableToscaElement.getElementId())
                || (indexedInheritableToscaElement.getDerivedFrom() != null && indexedInheritableToscaElement.getDerivedFrom().contains(type));
    }
}
