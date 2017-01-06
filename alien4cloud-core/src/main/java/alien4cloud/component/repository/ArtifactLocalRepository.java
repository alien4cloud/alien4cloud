
package alien4cloud.component.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import alien4cloud.component.repository.exception.RepositoryIOException;

@Component
public class ArtifactLocalRepository extends AbstractLocalRepository {

    private Path repositoryPath;

    @Override
    public Path getRepositoryPath() {
        return this.repositoryPath;
    }

    @Override
    public void storeFile(String id, InputStream data) {
        try {
            super.storeFile(id, data);
        } catch (RepositoryIOException e) {
            ensureRepositoryExists();
            super.storeFile(id, data);
        }
    }

    @Override
    public String storeFile(InputStream data) {
        try {
            return super.storeFile(data);
        } catch (RepositoryIOException e) {
            ensureRepositoryExists();
            return super.storeFile(data);
        }
    }

    @Required
    @Value("${directories.alien}/${directories.artifact_repository}")
    public void setRepositoryPath(String path) throws IOException {
        this.repositoryPath = Paths.get(path).toAbsolutePath();
        ensureRepositoryExists();
    }

    private void ensureRepositoryExists() {
        if (!Files.exists(repositoryPath)) {
            try {
                Files.createDirectories(repositoryPath);
            } catch (IOException e) {
                throw new RepositoryIOException("Fails to create artifact repository at " + repositoryPath.toString());
            }
        }
    }

    @Override
    public void checkRepository() throws IOException {
        setRepositoryPath(repositoryPath.toString());
    }
}
