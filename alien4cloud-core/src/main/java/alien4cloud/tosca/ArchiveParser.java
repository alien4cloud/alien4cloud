package alien4cloud.tosca;

import java.nio.file.Path;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.ToscaArchiveParser;
import lombok.extern.slf4j.Slf4j;

/**
 * Alien 4 Cloud implementation of the Tosca Archive Parser.
 *
 * It ensures that node template names are matching valid patterns (no dot, accents, or other character not allowed in alien4cloud).
 */
@Slf4j
@Component
public class ArchiveParser {
    @Inject
    private ToscaArchiveParser toscaArchiveParser;
    @Inject
    private ArchivePostProcessor postProcessor;

    /**
     * Parse an archive file from a zip.
     *
     * @param archiveFile The archive file currently zipped.
     * @return A parsing result that contains the Archive Root and eventual errors and/or warnings.
     * @throws ParsingException
     */
    public ParsingResult<ArchiveRoot> parse(Path archiveFile) throws ParsingException {
        return postProcessor.process(archiveFile, toscaArchiveParser.parse(archiveFile));
    }

    /**
     * Parse an archive file from a zip or from a single yaml file (if allowYamlFile is true).
     *
     * @param archiveFile The archive file currently zipped.
     * @param allowYamlFile If true the archive parser will accept single Yaml template rather than archive.
     * @return A parsing result that contains the Archive Root and eventual errors and/or warnings.
     * @throws ParsingException
     */
    public ParsingResult<ArchiveRoot> parse(Path archiveFile, boolean allowYamlFile) throws ParsingException {
        return postProcessor.process(archiveFile, toscaArchiveParser.parse(archiveFile, allowYamlFile));
    }

    /**
     * Parse an archive from a directory, it's very convenient for internal use and test
     *
     * @param archiveDir the directory which contains the archive
     * @return the parsing result
     * @throws ParsingException
     */
    public ParsingResult<ArchiveRoot> parseDir(Path archiveDir) throws ParsingException {
        return postProcessor.process(archiveDir, toscaArchiveParser.parseDir(archiveDir));
    }
}