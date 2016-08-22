package alien4cloud.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import lombok.AllArgsConstructor;

import com.google.common.io.Closeables;

@AllArgsConstructor(suppressConstructorProperties = true)
public class ZipDirWalker extends SimpleFileVisitor<Path> {

    private Path inputPath;

    private ZipOutputStream zipOutputStream;

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (!dir.equals(inputPath)) {
            zipOutputStream.putNextEntry(new ZipEntry(FileUtil.getChildEntryRelativePath(inputPath, dir, true)));
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        FileUtil.putZipEntry(zipOutputStream, new ZipEntry(FileUtil.getChildEntryRelativePath(inputPath, file, true)), file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        Closeables.close(zipOutputStream, true);
        throw exc;
    }
}
