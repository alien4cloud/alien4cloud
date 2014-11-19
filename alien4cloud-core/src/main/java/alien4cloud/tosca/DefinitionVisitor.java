package alien4cloud.tosca;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class DefinitionVisitor extends SimpleFileVisitor<Path> {
    private PathMatcher yamlPathMatcher;
    private PathMatcher ymlPathMatcher;
    private List<Path> definitionFiles = new ArrayList<Path>();

    public DefinitionVisitor(FileSystem csarFS) {
        this.yamlPathMatcher = csarFS.getPathMatcher("glob:**.yaml");
        this.ymlPathMatcher = csarFS.getPathMatcher("glob:**.yml");
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (yamlPathMatcher.matches(file) || ymlPathMatcher.matches(file)) {
            definitionFiles.add(file);
        }
        return super.visitFile(file, attrs);
    }
}