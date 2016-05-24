package alien4cloud.tosca.normative;

public final class AlienCustomTypes {
    // public static final String DELETABLE_BLOCKSTORAGE_TYPE = "alien.nodes.DeletableBlockStorage";

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
