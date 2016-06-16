package alien4cloud.utils.jackson;

public final class ConditionalAttributes {
    private ConditionalAttributes() {
    }

    public static final String REST = "ALIEN_REST_CONTEXT";
    public static final String ES = "ALIEN_ES_CONTEXT";
    /** ES serialization since 1.2.0-RC1 */
    public static final String ES_1_2 = "ALIEN_ES_1_2_CONTEXT";
}