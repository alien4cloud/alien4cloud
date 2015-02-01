package alien4cloud.model.cloud;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class MatchedStorageTemplate extends AbstractMatchedResource<StorageTemplate> {

    public MatchedStorageTemplate(StorageTemplate resource, String paaSResourceId) {
        super(resource, paaSResourceId);
    }

    public MatchedStorageTemplate() {
    }
}
