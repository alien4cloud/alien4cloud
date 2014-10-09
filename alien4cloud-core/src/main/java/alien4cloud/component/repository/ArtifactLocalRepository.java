package alien4cloud.component.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ArtifactLocalRepository extends AbstractLocalRepository {

    private Path repositoryPath;

    @Override
    public Path getRepositoryPath() {
        return this.repositoryPath;
    }

    @Required
    @Value("${directories.alien}/${directories.artifact_repository}")
    public void setRepositoryPath(String path) throws IOException {
        this.repositoryPath = Paths.get(path).toAbsolutePath();
        if (!Files.exists(repositoryPath)) {
            Files.createDirectories(repositoryPath);
        }
    }

    @Override
    public void checkRepository() throws IOException {
        setRepositoryPath(repositoryPath.toString());
    }
}
