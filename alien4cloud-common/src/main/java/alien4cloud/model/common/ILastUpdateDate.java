package alien4cloud.model.common;

import java.util.Date;

/**
 * Interface to be implemented by resources who need a last modification date.
 * Alien needs this information in order to re-synchronize deployment topology if resources has been changed.
 */
public interface ILastUpdateDate {

    void setLastUpdateDate(Date date);

    Date getLastUpdateDate();
}