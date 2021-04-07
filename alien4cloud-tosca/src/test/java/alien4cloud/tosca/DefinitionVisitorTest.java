package alien4cloud.tosca;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

import alien4cloud.tosca.parser.DefinitionVisitor;
import org.junit.Assert;
import org.junit.Test;

public class DefinitionVisitorTest {

    @Test
    public void testVisitorInZip() throws IOException {
        Path path = Paths.get("src/test/resources/tosca/visitor/zipped.zip");
        FileSystem fs = FileSystems.newFileSystem(path, (ClassLoader)null);
        doTest(fs);
    }

    private void doTest(FileSystem fs) throws IOException {
        DefinitionVisitor visitor = new DefinitionVisitor(fs);
        Files.walkFileTree(fs.getPath(fs.getSeparator()), EnumSet.noneOf(FileVisitOption.class), 1, visitor);
        Assert.assertEquals(2, visitor.getDefinitionFiles().size());
    }
}
