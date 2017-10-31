package alien4cloud.plugin.archives;

import java.nio.file.Path;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.ArchiveParser;
import org.slf4j.Logger;

import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.plugin.exception.PluginArchiveException;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.AlienConstants;

/**
 * Abstract Service to help {@link IArchiveProviderPlugin} parse its provided archives<br/>
 * Implementation should be done inside plugins, and not inside Alien4Cloud main projects .<br/>
 * Every willing plugin can annotate the implementation with @{@link org.springframework.stereotype.Component}. Thus the related {@link ManagedPlugin} bean will
 * be injected
 */
public abstract class AbstractPluginArchiveService {

    /**
     * context of the plugin implementing this service
     */
    @Inject
    private ManagedPlugin selfContext;

    @Inject
    private ArchiveParser archiveParser;
    private Logger log = getLogger();

    protected abstract Logger getLogger();

    /**
     * Parse an archive given a path relative to the plugin
     * 
     * @param archiveRelativePath Relative path where the archive is in the plugin
     * @return The parsed archive as a @{@link PluginArchive}
     * @throws PluginArchiveException
     */
    public PluginArchive parse(String archiveRelativePath) throws PluginArchiveException {
        String archiveErrorMsge = "Archive in path: [ " + archiveRelativePath + " ]";
        // Parse the archives
        ParsingResult<ArchiveRoot> result;
        Path archivePath = selfContext.getPluginPath().resolve(archiveRelativePath);
        try {
            result = this.archiveParser.parseDir(archivePath, AlienConstants.GLOBAL_WORKSPACE_ID);
        } catch (ParsingException e) {
            throw new PluginArchiveException("Failed to parse " + archiveErrorMsge, e);
        }
        if (result.getContext().getParsingErrors() != null && !result.getContext().getParsingErrors().isEmpty()) {
            log.error("Parsing errors for" + archiveErrorMsge);
            for (ParsingError parsingError : result.getContext().getParsingErrors()) {
                log.error(parsingError.toString());
            }
            throw new PluginArchiveException(archiveErrorMsge + " is invalid");
        }
        return new PluginArchive(result.getResult(), archivePath);
    }
}
