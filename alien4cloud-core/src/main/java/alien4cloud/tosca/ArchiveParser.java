package alien4cloud.tosca;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import javax.annotation.Resource;
import javax.validation.Validator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.container.exception.CSARIOException;
import alien4cloud.tosca.container.exception.CSARTechnicalException;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.model.ToscaMeta;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.ToscaParser;
import alien4cloud.tosca.parser.ValidatedNodeParser;
import alien4cloud.tosca.parser.YamlSimpleParser;
import alien4cloud.tosca.parser.mapping.CsarMetaMapping;
import alien4cloud.tosca.parser.mapping.ToscaMetaMapping;

@Slf4j
@Component
public class ArchiveParser {
    @Deprecated
    public static final String ALIEN_META_FILE_LOCATION = FileSystems.getDefault().getSeparator() + "TOSCA-Metadata" + FileSystems.getDefault().getSeparator()
            + "ALIEN-META.yaml";
    public static final String TOSCA_META_FILE_LOCATION = FileSystems.getDefault().getSeparator() + "TOSCA-Metadata" + FileSystems.getDefault().getSeparator()
            + "TOSCA.meta";

    @Resource
    private ToscaMetaMapping toscaMetaMapping;
    @Resource
    private CsarMetaMapping csarMetaMapping;
    @Resource
    private ToscaParser toscaParser;
    @Resource
    private Validator validator;

    public ParsingResult<ArchiveRoot> parse(Path archiveFile) throws ParsingException {
        FileSystem csarFS;
        try {
            csarFS = FileSystems.newFileSystem(archiveFile, null);
        } catch (IOException e) {
            throw new CSARIOException("Problem happened while accessing file [" + archiveFile + "]", e);
        } catch (ProviderNotFoundException e) {
            // CSARErrorCode.ERRONEOUS_ARCHIVE_FILE
            log.warn("Failed to import archive", e);
            throw new ParsingException("Archive", new ParsingError("File is not in good format, only zip file is supported ", null, e.getMessage(), null, null));
        }

        if (csarFS.getPath(TOSCA_META_FILE_LOCATION).toFile().exists()) {
            return parseFromToscaMeta(csarFS);
        } else if (csarFS.getPath(ALIEN_META_FILE_LOCATION).toFile().exists()) {
            return parseFromAlienMeta(csarFS);
        }
        return parseFromRootDefinitions(csarFS);
    }

    @Deprecated
    private ParsingResult<ArchiveRoot> parseFromAlienMeta(FileSystem csarFS) throws ParsingException {
        // add deprecated warning.
        YamlSimpleParser<ToscaMeta> parser = new YamlSimpleParser<ToscaMeta>(new ValidatedNodeParser<ToscaMeta>(validator, csarMetaMapping.getParser()));
        ParsingResult<ToscaMeta> parsingResult = parser.parseFile(csarFS.getPath(ALIEN_META_FILE_LOCATION));
        if (parsingResult.getResult().getEntryDefinitions() == null && parsingResult.getResult().getDefinitions().size() == 1) {
            parsingResult.getResult().setEntryDefinitions(parsingResult.getResult().getDefinitions().get(0));
        } else if (parsingResult.getResult().getDefinitions().size() > 1) {
            throw new ParsingException(ALIEN_META_FILE_LOCATION, new ParsingError("Alien only supports archives with a single definition.", null, null, null,
                    String.valueOf(parsingResult.getResult().getDefinitions().size())));
        }
        ParsingResult<ArchiveRoot> archiveResult = parseFromToscaMeta(csarFS, parsingResult.getResult(), ALIEN_META_FILE_LOCATION);
        return mergeWithToscaMeta(archiveResult, parsingResult);
    }

    private ParsingResult<ArchiveRoot> parseFromToscaMeta(FileSystem csarFS) throws ParsingException {
        YamlSimpleParser<ToscaMeta> parser = new YamlSimpleParser<ToscaMeta>(toscaMetaMapping.getParser());
        ParsingResult<ToscaMeta> parsingResult = parser.parseFile(csarFS.getPath(TOSCA_META_FILE_LOCATION));
        ParsingResult<ArchiveRoot> archiveResult = parseFromToscaMeta(csarFS, parsingResult.getResult(), TOSCA_META_FILE_LOCATION);
        return mergeWithToscaMeta(archiveResult, parsingResult);
    }

    private ParsingResult<ArchiveRoot> mergeWithToscaMeta(ParsingResult<ArchiveRoot> archiveResult, ParsingResult<ToscaMeta> toscaResult) {
        archiveResult.getContext().getSubContexts().add(0, toscaResult.getContext());
        archiveResult.getResult().getArchive().setName(toscaResult.getResult().getName());
        archiveResult.getResult().getArchive().setVersion(toscaResult.getResult().getVersion());
        if (toscaResult.getResult().getCreatedBy() != null) {
            archiveResult.getResult().getArchive().setTemplateAuthor(toscaResult.getResult().getCreatedBy());
        }
        return archiveResult;
    }

    private ParsingResult<ArchiveRoot> parseFromToscaMeta(FileSystem csarFS, ToscaMeta toscaMeta, String metaFileName) throws ParsingException {
        if (toscaMeta.getEntryDefinitions() != null) {
            return parseArchive(csarFS.getPath(toscaMeta.getEntryDefinitions()));
        }
        throw new ParsingException(metaFileName, new ParsingError("No entry definitions found in the meta file.", null, null, null, null));
    }

    private ParsingResult<ArchiveRoot> parseFromRootDefinitions(FileSystem csarFS) throws ParsingException {
        // load definitions from the archive root
        try {
            DefinitionVisitor visitor = new DefinitionVisitor(csarFS);
            Files.walkFileTree(csarFS.getPath(csarFS.getSeparator()), null, 0, visitor);
            if (visitor.definitionFiles.size() == 1) {
                return parseArchive(visitor.definitionFiles.get(0));
            }
            throw new ParsingException("Archive", new ParsingError("Alien only supports archives with a single root definition.", null, null, null,
                    String.valueOf(visitor.definitionFiles.size())));
        } catch (IOException e) {
            throw new CSARTechnicalException("Failed to list root definitions.", e);
        }
    }

    private class DefinitionVisitor extends SimpleFileVisitor<Path> {
        private PathMatcher yamlPathMatcher;
        private PathMatcher ymlPathMatcher;
        private List<Path> definitionFiles;

        public DefinitionVisitor(FileSystem csarFS) {
            this.yamlPathMatcher = csarFS.getPathMatcher("*.yaml");
            this.ymlPathMatcher = csarFS.getPathMatcher("*.yml");
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (yamlPathMatcher.matches(file) || ymlPathMatcher.matches(file)) {
                definitionFiles.add(file);
            }
            return super.visitFile(file, attrs);
        }
    }

    private ParsingResult<ArchiveRoot> parseArchive(Path definitionPath) throws ParsingException {
        return toscaParser.parseFile(definitionPath);
    }
}