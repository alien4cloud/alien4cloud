package alien4cloud.utils;

import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

public class NotAnalyzedTextMapEntry extends MapEntry<String, String> {

    @Override
    @StringField(indexType = IndexType.not_analyzed)
    public String getKey() {
        return super.getKey();
    }

    @Override
    @StringField(indexType = IndexType.not_analyzed)
    public String getValue() {
        return super.getValue();
    }

    @Override
    public void setKey(String key) {
        super.setKey(key);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
    }
}
