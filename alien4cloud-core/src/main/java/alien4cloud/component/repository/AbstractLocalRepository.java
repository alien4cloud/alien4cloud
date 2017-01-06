package alien4cloud.component.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import alien4cloud.component.repository.exception.RepositoryIOException;

public abstract class AbstractLocalRepository implements IFileRepository {

    @Override
    public String storeFile(InputStream data) {
        String generatedId = UUID.randomUUID().toString();
        storeFile(generatedId, data);
        return generatedId;
    }

    @Override
    public InputStream getFile(String id) {
        try {
            return Files.newInputStream(resolveFile(id));
        } catch (IOException e) {
            throw new RepositoryIOException("Could not retrieve file with UID [" + id + "]", e);
        }
    }

    @Override
    public boolean isFileExist(String id) {
        return Files.isRegularFile(resolveFile(id));
    }

    @Override
    public boolean deleteFile(String id) {
        return resolveFile(id).toFile().delete();
    }

    @Override
    public void storeFile(String id, InputStream data) {
        try {
            checkRepository();
            Files.copy(data, resolveFile(id), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RepositoryIOException("Could not store file with UID [" + id + "]", e);
        }
    }

    @Override
    public long getFileLength(String id) {
        return resolveFile(id).toFile().length();
    }

    public Path resolveFile(String id) {
        return getRepositoryPath().resolve(id);
    }

    public abstract Path getRepositoryPath();

    public abstract void checkRepository() throws IOException;
}
