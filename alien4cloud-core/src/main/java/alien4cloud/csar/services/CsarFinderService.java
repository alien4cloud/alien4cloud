package alien4cloud.csar.services;

import alien4cloud.exception.GitException;
import alien4cloud.tosca.parser.ToscaArchiveParser;
import alien4cloud.utils.FileUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

/**
 * This service detects TOSCA cloud service archives in a given folder and return an ordered list of archives path to import.
 */
@Service
public class CsarFinderService {

    /**
     * Search in the given path for folders that contains CloudServiceArchives and zip them so they.
     *
     * @param searchPath The path in which to search for archives.
     * @return a list of path that contains archives.
     */
    public Set<Path> prepare(Path searchPath, Path zipPath, String subpath) {
        ToscaFinderWalker toscaFinderWalker = new ToscaFinderWalker();
        toscaFinderWalker.zipRootPath = zipPath;
        toscaFinderWalker.rootPath = searchPath;
        toscaFinderWalker.subpath = subpath;
        try {
            Files.walkFileTree(searchPath, toscaFinderWalker);
        } catch (IOException e) {
            throw new GitException("Failed to browse git repository content in order to import archives.", e);
        }
        return toscaFinderWalker.toscaArchives;
    }

    private static class ToscaFinderWalker extends SimpleFileVisitor<Path> {
        private Path rootPath;
        private Path zipRootPath;
        private String subpath;
        private Set<Path> toscaArchives = Sets.newHashSet();

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (ToscaArchiveParser.TOSCA_META_FOLDER_NAME.equals(dir.getFileName())) {
                // zip parent folder and add the path.
                addToscaArchive(dir.getParent());
                return FileVisitResult.SKIP_SIBLINGS;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            // TODO check that the file has the tosca_definitions_version header.
            Path fileName = file.getFileName();
            if (fileName.toString().endsWith(".yaml") || fileName.toString().endsWith(".yml")) {
                addToscaArchive(file.getParent());
                return FileVisitResult.SKIP_SIBLINGS;
            }
            return FileVisitResult.CONTINUE;
        }

        private void addToscaArchive(Path path) {
            if (!(Strings.isNullOrEmpty(subpath) || path.endsWith(subpath))) {
                return;
            }
            Path relativePath = rootPath.relativize(path);
            Path zipPath = zipRootPath.resolve(relativePath).resolve("archive.zip");
            try {
                if (Files.exists(zipPath)) {
                    FileUtil.delete(zipPath);
                }
                FileUtil.zip(path, zipPath);
                toscaArchives.add(zipPath);
            } catch (IOException e) {
                throw new GitException("Failed to zip archives in order to import them.", e);
            }
        }
    }
}