package alien4cloud.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import lombok.AllArgsConstructor;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import com.google.common.io.Closeables;

@AllArgsConstructor(suppressConstructorProperties = true)
public class TarDirWalker extends SimpleFileVisitor<Path> {

    private Path basePath;

    private TarArchiveOutputStream tarArchiveOutputStream;

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (!dir.equals(basePath)) {
            tarArchiveOutputStream.putArchiveEntry(new TarArchiveEntry(FileUtil.getChildEntryRelativePath(basePath, dir, true)));
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        FileUtil.putTarEntry(tarArchiveOutputStream, new TarArchiveEntry(FileUtil.getChildEntryRelativePath(basePath, file, true)), file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        Closeables.close(tarArchiveOutputStream, true);
        throw exc;
    }
}
