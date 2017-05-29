package alien4cloud.csar.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import alien4cloud.exception.GitException;
import alien4cloud.tosca.parser.ToscaArchiveParser;
import alien4cloud.utils.FileUtil;
import lombok.SneakyThrows;

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
    public Set<Path> prepare(Path searchPath, Path zipPath) {
        ToscaFinderWalker toscaFinderWalker = new ToscaFinderWalker();
        toscaFinderWalker.zipRootPath = zipPath;
        toscaFinderWalker.rootPath = searchPath;
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
            if (isToscaFile(file)) {
                addToscaArchive(file.getParent());
                return FileVisitResult.SKIP_SIBLINGS;
            }
            return FileVisitResult.CONTINUE;
        }

        private void addToscaArchive(Path path) {
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

        @SneakyThrows
        private boolean isToscaFile(Path path) {
            return isYamlFile(path) && readFirstLine(path).startsWith("tosca_definitions_version");
        }

        private boolean isYamlFile(Path file) {
            return Files.isRegularFile(file) && (file.getFileName().toString().endsWith(".yaml") || file.getFileName().toString().endsWith(".yml"));
        }

        /**
         * Read the first non-empty line of the file
         *
         * @param path to the file
         * @return the first non-empty line of the file or an empty string
         * @throws IOException
         */
        private static String readFirstLine(Path path) throws IOException {
            InputStreamReader stream = new InputStreamReader(Files.newInputStream(path), Charset.defaultCharset());
            try (BufferedReader reader = new BufferedReader(stream)) {
                String line = reader.readLine();
                while (line != null) {
                    line = line.trim();
                    if (line.length() > 0) {
                        return line;
                    }
                    line = reader.readLine();
                }
            }
            return "";
        }
    }
}