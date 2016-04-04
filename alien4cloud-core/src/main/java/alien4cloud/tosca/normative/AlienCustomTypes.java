package alien4cloud.tosca.normative;

import alien4cloud.model.components.ComplexPropertyValue;

public final class AlienCustomTypes {
    public static final String DELETABLE_BLOCKSTORAGE_TYPE = "alien.nodes.DeletableBlockStorage";


    public static final boolean checkDefaultIsComplex(String defaultValue) {
        return defaultValue.startsWith("{") && defaultValue.endsWith("}");
    }

    public static final boolean checkDefaultIsList(String defaultValue) {
        return defaultValue.startsWith("[") && defaultValue.endsWith("]");
    }

    public static final boolean checkDefaultIsObject(String defaultValue) {
        return checkDefaultIsComplex(defaultValue) || checkDefaultIsList(defaultValue);
    }
}
