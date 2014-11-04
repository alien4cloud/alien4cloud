package alien4cloud.tosca;

import java.nio.file.Path;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.model.Csar;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;

@Component
public class ArchiveUploadService {
    @Resource
    private ArchiveParser parser;
    @Resource
    private ArchivePostProcessor postProcessor;

    /**
     * Upload a TOSCA archive and index it's components.
     * 
     * @param path The archive path.
     * @return The Csar object from the parsing.
     * @throws ParsingException
     */
    public Csar upload(Path path) throws ParsingException {
        // parse the archive.
        ParsingResult<ArchiveRoot> archiveResult = parser.parse(path);
        postProcessor.postProcessArchive(archiveResult);
        return null;
    }
}