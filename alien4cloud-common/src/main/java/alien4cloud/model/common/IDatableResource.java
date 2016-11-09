package alien4cloud.model.common;

import java.util.Date;

/**
 * Interface to be implemented by resources who need a creation date and a last modification date.
 */
public interface IDatableResource {

    void setCreationDate(Date date);

    Date getCreationDate();

    void setLastUpdateDate(Date date);

    Date getLastUpdateDate();
}