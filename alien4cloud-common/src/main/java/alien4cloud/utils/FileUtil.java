package alien4cloud.utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.DatatypeConverter;

@Slf4j
public final class FileUtil {
    /**
     * Utility class should have private constructor.
     */
    private FileUtil() {
    }

    /**
     * Check if the file matching the given path is a zip file or not.
     * 
     * @param path The patch to check.
     */
    public static boolean isZipFile(Path path) {
        File f = path.toFile();
        if (f.isDirectory() || f.length() < 4) {
            return false;
        }

        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(f))) {
            return inputStream.readInt() == 0x504b0304;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    protected static void putZipEntry(ZipOutputStream zipOutputStream, ZipEntry zipEntry, Path file) throws IOException {
        zipOutputStream.putNextEntry(zipEntry);
        InputStream input = new BufferedInputStream(Files.newInputStream(file));
        try {
            ByteStreams.copy(input, zipOutputStream);
            zipOutputStream.closeEntry();
        } finally {
            input.close();
        }
    }

    protected static void putTarEntry(TarArchiveOutputStream tarOutputStream, TarArchiveEntry tarEntry, Path file) throws IOException {
        tarEntry.setSize(Files.size(file));
        tarOutputStream.putArchiveEntry(tarEntry);
        InputStream input = new BufferedInputStream(Files.newInputStream(file));
        try {
            ByteStreams.copy(input, tarOutputStream);
            tarOutputStream.closeArchiveEntry();
        } finally {
            input.close();
        }
    }

    public static String getChildEntryRelativePath(Path base, Path child, boolean convertToLinuxPath) {
        String path = base.toUri().relativize(child.toUri()).getPath();
        if (convertToLinuxPath && !"/".equals(base.getFileSystem().getSeparator())) {
            return path.replace(base.getFileSystem().getSeparator(), "/");
        } else {
            return path;
        }
    }

    /**
     * Recursively zip file and directory
     *
     * @param inputPath file path can be directory
     * @param outputPath where to put the zip
     * @throws IOException when IO error happened
     */
    public static void zip(Path inputPath, Path outputPath) throws IOException {
        if (!Files.exists(inputPath)) {
            throw new FileNotFoundException("File not found " + inputPath);
        }
        touch(outputPath);
        ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(outputPath)));
        try {
            if (!Files.isDirectory(inputPath)) {
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
     * Recursively tar file
     *
     * @param inputPath file path can be directory
     * @param outputPath where to put the archived file
     * @param childrenOnly if inputPath is directory and if childrenOnly is true, the archive will contain all of its children, else the archive contains unique
     *            entry which is the inputPath itself
     * @param gZipped compress with gzip algorithm
     */
    public static void tar(Path inputPath, Path outputPath, boolean gZipped, boolean childrenOnly) throws IOException {
        if (!Files.exists(inputPath)) {
            throw new FileNotFoundException("File not found " + inputPath);
        }
        touch(outputPath);
        OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outputPath));
        if (gZipped) {
            outputStream = new GzipCompressorOutputStream(outputStream);
        }
        TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(outputStream);
        tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        try {
            if (!Files.isDirectory(inputPath)) {
                putTarEntry(tarArchiveOutputStream, new TarArchiveEntry(inputPath.getFileName().toString()), inputPath);
            } else {
                Path sourcePath = inputPath;
                if (!childrenOnly) {
                    // In order to have the dossier as the root entry
                    sourcePath = inputPath.getParent();
                }
                Files.walkFileTree(inputPath, new TarDirWalker(sourcePath, tarArchiveOutputStream));
            }
            tarArchiveOutputStream.flush();
        } finally {
            Closeables.close(tarArchiveOutputStream, true);
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
        try (FileSystem zipFS = FileSystems.newFileSystem(zipFile, null)) {
            final Path root = zipFS.getPath("/");
            copy(root, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String relativizePath(Path root, Path child) {
        String childPath = child.toAbsolutePath().toString();
        String rootPath = root.toAbsolutePath().toString();
        if (childPath.equals(rootPath)) {
            return "";
        }
        int indexOfRootInChild = childPath.indexOf(rootPath);
        if (indexOfRootInChild != 0) {
            throw new IllegalArgumentException("Child path " + childPath + "is not beginning with root path " + rootPath);
        }
        String relativizedPath = childPath.substring(rootPath.length(), childPath.length());
        while (relativizedPath.startsWith(root.getFileSystem().getSeparator())) {
            relativizedPath = relativizedPath.substring(1);
        }
        return relativizedPath;
    }

    public static void copy(final Path source, final Path destination, final CopyOption... options) throws IOException {
        if (Files.notExists(destination)) {
            Files.createDirectories(destination);
        }

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileRelativePath = relativizePath(source, file);
                Path destFile = destination.resolve(fileRelativePath);
                Files.copy(file, destFile, options);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String dirRelativePath = relativizePath(source, dir);
                Path destDir = destination.resolve(dirRelativePath);
                Files.createDirectories(destDir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static class EraserWalker extends SimpleFileVisitor<Path> {
        private Path[] keepPath;

        private EraserWalker(Path... keepPath) {
            this.keepPath = keepPath;
        }

        private boolean isKeepPath(Path path) {
            for (Path keep : keepPath) {
                if (path.equals(keep)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (isKeepPath(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (!isKeepPath(file)) {
                file.toFile().delete();
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc == null) {
                if (!isKeepPath(dir)) {
                    dir.toFile().delete();
                }
                return FileVisitResult.CONTINUE;
            }
            throw exc;
        }
    }

    /**
     * Recursively delete file and directory but the specified paths
     *
     * @param deletePath file path can be directory
     * @param keepPath paths not to be deleted.
     * @throws IOException when IO error happened
     */
    public static void delete(Path deletePath, Path... keepPath) throws IOException {
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

    /**
     * Create a directory from path if it does not exist
     *
     * @param directoryPath
     * @throws IOException
     */
    public static Path createDirectoryIfNotExists(String directoryPath) throws IOException {
        Path tempPath = Paths.get(directoryPath);
        if (!Files.exists(tempPath)) {
            log.info("Temp directory for uploaded file do not exist, trying to create [" + directoryPath + "]");
            Files.createDirectories(tempPath);
        }
        return tempPath;
    }

    /**
     * Create an empty file at the given path
     *
     * @param path to create file
     * @throws IOException
     */
    public static boolean touch(Path path) throws IOException {
        Path parentDir = path.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            return true;
        }
        if (!Files.exists(path)) {
            Files.createFile(path);
            return true;
        }
        return false;
    }

    /**
     * List all files of which name is matching the pattern
     * 
     * @param directory the start point
     * @param matcher the regex expression to match files
     * @return list of files of which name is matching the pattern
     * @throws IOException
     */
    public static List<Path> listFiles(Path directory, String matcher) throws IOException {
        final Pattern pattern = Pattern.compile(matcher);
        final List<Path> files = Lists.newArrayList();
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (pattern.matcher(file.toString()).matches()) {
                    files.add(file);
                }
                return super.visitFile(file, attrs);
            }
        });
        return files;
    }

    /**
     * Computes a SHA-1 checksum on a single file.
     * 
     * @param path The path of the file for which to compute the SHA-1 hash.
     * @return The SHA-1 hash string.
     */
    @SneakyThrows({ Exception.class })
    public static String getSHA1Checksum(Path path) {
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found in hash processor" + path);
        }
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        addFileToDigest(digest, path);
        return DatatypeConverter.printHexBinary(digest.digest());
    }

    /**
     * Computes a SHA-1 checksum on a directory. The checksum ignores hidden files.
     *
     * @param rootPath The root path for which to compute SHA-1 on every sub files and folders.
     * @return The SHA-1 hash string.
     */
    @SneakyThrows({ IOException.class })
    public static String deepSHA1(Path rootPath) {
        if (isZipFile(rootPath)) {
            try (FileSystem csarFS = FileSystems.newFileSystem(rootPath, null)) {
                Path innerZipPath = csarFS.getPath(FileSystems.getDefault().getSeparator());
                return computeDirectoryHash(innerZipPath);
            }
        } else if (Files.isRegularFile(rootPath)) {
            return getSHA1Checksum(rootPath);
        } else if (Files.isDirectory(rootPath)) {
            return computeDirectoryHash(rootPath);
        }
        throw new FileNotFoundException("Unable to compute hash for file " + rootPath);
    }

    @SneakyThrows({ IOException.class, NoSuchAlgorithmException.class })
    private static String computeDirectoryHash(Path rootPath) {
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        Files.walk(rootPath).filter(FileUtil::isNotHidden).filter(Files::isRegularFile).forEach(path -> addFileToDigest(digest, path));
        return DatatypeConverter.printHexBinary(digest.digest());

    }

    @SneakyThrows({ IOException.class })
    private static void addFileToDigest(MessageDigest digest, Path path) {
        try (InputStream digestInputStream = new DigestInputStream(new BufferedInputStream(Files.newInputStream(path)), digest)) {
            while (digestInputStream.read() != -1) {
            }
        }
    }

    @SneakyThrows({ IOException.class })
    private static boolean isNotHidden(Path path) {
        return !Files.isHidden(path);
    }
}