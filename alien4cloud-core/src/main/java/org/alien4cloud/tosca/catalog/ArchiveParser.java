package org.alien4cloud.tosca.catalog;

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
     * Parse a TOSCA archive and reuse an existing TOSCA Context. Other methods will create an independent context for the parsing.
     * Note that a TOSCA Context MUST have been initialized in order to use this method.
     *
     * @param archiveFile The archive file to parse.
     * @return A parsing result that contains the Archive Root and eventual errors and/or warnings.
     * @throws ParsingException In case of a severe issue while parsing (incorrect yaml, no tosca file etc.)
     */
    public ParsingResult<ArchiveRoot> parseWithExistingContext(Path archiveFile, String workspace) throws ParsingException {
        return postProcessor.process(archiveFile, toscaArchiveParser.parseWithExistingContext(archiveFile), workspace);
    }

    /**
     * Parse an archive file from a zip.
     *
     * @param archiveFile The archive file currently zipped.
     * @return A parsing result that contains the Archive Root and eventual errors and/or warnings.
     * @throws ParsingException In case of a severe issue while parsing (incorrect yaml, no tosca file etc.)
     */
    public ParsingResult<ArchiveRoot> parse(Path archiveFile, String workspace) throws ParsingException {
        return postProcessor.process(archiveFile, toscaArchiveParser.parse(archiveFile), workspace);
    }

    /**
     * Parse an archive from a directory, it's very convenient for internal use and test
     *
     * @param archiveDir the directory which contains the archive
     * @return A parsing result that contains the Archive Root and eventual errors and/or warnings.
     * @throws ParsingException In case of a severe issue while parsing (incorrect yaml, no tosca file etc.)
     */
    public ParsingResult<ArchiveRoot> parseDir(Path archiveDir, String workspace) throws ParsingException {
        return postProcessor.process(archiveDir, toscaArchiveParser.parseDir(archiveDir), workspace);
    }
}