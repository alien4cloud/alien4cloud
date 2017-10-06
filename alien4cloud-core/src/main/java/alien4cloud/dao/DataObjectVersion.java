package alien4cloud.dao;

import alien4cloud.utils.VersionUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.query.FetchContext;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

/**
 * DataObjectVersion is used to store the current data version of Alien4Cloud objects (index) for migration purpose.
 */
@Getter
@Setter
@NoArgsConstructor
@ESObject
public class DataObjectVersion {

    /* ID to ensure that this object is unique and perform better search */
    public static final String ID = "alien_data_model";

    @Id
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String id;

    /* Version of the current data in Alien, should be updated by migration plugin after breaking change in the model */
    private String version;

    public boolean isOlderThan(String otherVersion) {
        return VersionUtil.compare(version, otherVersion) < 0;
    }
}
