package alien4cloud.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

public final class FileUtil {
    /** Utility class should have private constructor. */
    private FileUtil() {
    }

    private static class ZipDirWalker implements FileVisitor<Path> {

        private Path inputPath;

        private ZipOutputStream zipOutputStream;

        /**
         * Create the zip directory walker (Walk a directory and add it's content to a {@link ZipOutputStream}.
         * 
         * @param inputPath The path of the directory to add to the {@link ZipOutputStream}.
         * @param zipOutputStream The {@link ZipOutputStream} in which to add directory content.
         */
        public ZipDirWalker(Path inputPath, ZipOutputStream zipOutputStream) {
            super();
            this.inputPath = inputPath;
            this.zipOutputStream = zipOutputStream;
        }

        private String getZipEntryName(Path file) {
            return inputPath.toUri().relativize(file.toUri()).getPath();
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (!dir.equals(inputPath)) {
                zipOutputStream.putNextEntry(new ZipEntry(getZipEntryName(dir)));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            putZipEntry(zipOutputStream, new ZipEntry(getZipEntryName(file)), file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            Closeables.close(zipOutputStream, true);
            throw exc;
        }

    }

    private static void putZipEntry(ZipOutputStream zipOutputStream, ZipEntry zipEntry, Path file) throws IOException {
        zipOutputStream.putNextEntry(zipEntry);
        ByteStreams.copy(new BufferedInputStream(Files.newInputStream(file)), zipOutputStream);
        zipOutputStream.closeEntry();
    }

    /**
     * Recursively zip file and directory
     * 
     * @param inputPath file path can be directory
     * @param outputPath where to put the zip
     * @throws IOException when IO error happened
     */
    public static void zip(Path inputPath, Path outputPath) throws IOException {
        Path parentDir = outputPath.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        if (!Files.exists(outputPath)) {
            Files.createFile(outputPath);
        }
        ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(outputPath)));
        try {
            if (!Files.isDirectory(inputPath) && !Files.isSymbolicLink(inputPath)) {
                putZipEntry(zipOutputStream, new ZipEntry(inputPath.getFileName().toString()), inputPath);
            } else {
                Files.walkFileTree(inputPath, new ZipDirWalker(inputPath, zipOutputStream));
            }
            zipOutputStream.flush();
        } finally {
            Closeables.close(zipOutputStream, true);
        }
    }

    /**
     * Unzip a zip file to a destination folder.
     * 
     * @param zipFile The zip file to unzip.
     * @param destination The destination folder in which to save the file.
     * @throws IOException In case something fails.
     */
    public static void unzip(final Path zipFile, final Path destination) throws IOException {
        if (Files.notExists(destination)) {
            Files.createDirectories(destination);
        }

        try (FileSystem zipFS = FileSystems.newFileSystem(zipFile, null)) {
            final Path root = zipFS.getPath("/");
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    final Path destFile = Paths.get(destination.toString(), file.toString());
                    Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    final Path destDir = Paths.get(destination.toString(), dir.toString());
                    Files.createDirectories(destDir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static class EraserWalker extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            file.toFile().delete();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc == null) {
                dir.toFile().delete();
                return FileVisitResult.CONTINUE;
            }
            throw exc;
        }
    }

    /**
     * Recursively delete file and directory
     * 
     * @param deletePath file path can be directory
     * @throws IOException when IO error happened
     */
    public static void delete(Path deletePath) throws IOException {
        if (!Files.isDirectory(deletePath)) {
            deletePath.toFile().delete();
            return;
        }
        Files.walkFileTree(deletePath, new EraserWalker());
    }

    /**
     * Read all files bytes and create a string.
     * 
     * @param path The file's path.
     * @param charset The charset to use to convert the bytes to string.
     * @return A string from the file content.
     * @throws IOException In case the file cannot be read.
     */
    public static String readTextFile(Path path, Charset charset) throws IOException {
        return new String(Files.readAllBytes(path), charset);
    }

    /**
     * Read all files bytes and create a string using UTF_8 charset.
     * 
     * @param path The file's path.
     * @return A string from the file content.
     * @throws IOException In case the file cannot be read.
     */
    public static String readTextFile(Path path) throws IOException {
        return readTextFile(path, Charsets.UTF_8);
    }
}
