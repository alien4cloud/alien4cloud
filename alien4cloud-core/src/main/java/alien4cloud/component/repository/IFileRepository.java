package alien4cloud.component.repository;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * A contract for an internal file system. It enables store file and retrieve file.
 * 
 * @author mkv
 * 
 */
public interface IFileRepository {

    /**
     * Store a file in an arbitrary file system and get the UID of the saved file. After the data stream has been consumed, the stream will not be closed, it's
     * the caller responsibility to do it.
     * 
     * @param data the data to save
     * @return the UID of the saved file
     */
    String storeFile(InputStream data);

    /**
     * Check if a file exists for the given UID.
     * 
     * @param id UID of the file to check
     * @return true if file exist, false otherwise
     */
    boolean isFileExist(String id);

    /**
     * Delete a file with given UID
     * 
     * @param id the UID of the file to delete
     */
    boolean deleteFile(String id);

    /**
     * Store/update a file in an arbitrary file system with the given UID. After the data stream has been consumed, the stream will not be
     * closed, it's the caller responsibility to do it.
     * 
     * @param data the data to save
     * @return the UID of the saved file
     */
    void storeFile(String id, InputStream data);

    /**
     * Retrieve the content of the file with given id from the file repository. It's the caller's responsibility to close the stream.
     * 
     * @param id the UID of the file to retrieve
     * @return an {@link InputStream} which contain the file data
     */
    InputStream getFile(String id);

    /**
     * Retrieve the path to access a given file based on it's id.
     * 
     * @param id The id of the file to retrieve.
     * @return a path to the file.
     */
    Path resolveFile(String id);

    /**
     * Retrieve the lenght of the content of the file with given id from the file repository.
     *
     * @param id the UID of the file for which to get length
     * @return the length of the file.
     */
    long getFileLength(String id);
}
