package alien4cloud.model.common;

import java.util.Date;

/**
 * Interface to be implemented by resources who need a creation date.
 */
public interface ICreationDate {

    void setCreationDate(Date date);

    Date getCreationDate();
}