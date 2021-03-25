package org.alien4cloud.tosca.catalog;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

import alien4cloud.deployment.exceptions.UnresolvableArtifactException;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingResult;

@Component
public class ArchivePostProcessor extends AbstractArchivePostProcessor implements IArchivePostProcessor {

    private abstract class AbstractArchivePathResolver implements ArchivePathChecker {
        @Override
        public boolean exists(String artifactReference) {
            return Files.exists(resolve(artifactReference));
        }

        public abstract Path resolve(String artifactReference);
    }

    private class ZipArchivePathResolver extends AbstractArchivePathResolver {
        private FileSystem fileSystem;

        private ZipArchivePathResolver(Path archive) throws IOException {
            fileSystem = FileSystems.newFileSystem(archive, (ClassLoader)null);
        }

        @Override
        public Path resolve(String artifactReference) {
            return fileSystem.getPath(artifactReference);
        }

        @Override
        public void close() {
            try {
                fileSystem.close();
            } catch (Exception e) {
                // Ignored
            }
        }
    }

    private class DirArchivePathResolver extends AbstractArchivePathResolver {
        private Path archive;

        private DirArchivePathResolver(Path archive) {
            this.archive = archive;
        }

        @Override
        public Path resolve(String artifactReference) {
            return archive.resolve(artifactReference);
        }

        @Override
        public void close() {
            // Do nothing
        }
    }

    @Override
    protected ArchivePathChecker createPathChecker(Path archive) {
        if (Files.isRegularFile(archive)) {
            try {
                return new ZipArchivePathResolver(archive);
            } catch (Exception e) {
                throw new UnresolvableArtifactException("Csar's temporary file is not accessible as a Zip", e);
            }
        } else {
            return new DirArchivePathResolver(archive);
        }
    }

    @Override
    public ParsingResult<ArchiveRoot> process(Path archive, ParsingResult<ArchiveRoot> parsedArchive, String workspace) {
        return doProcess(archive, parsedArchive, workspace);
    }
}
