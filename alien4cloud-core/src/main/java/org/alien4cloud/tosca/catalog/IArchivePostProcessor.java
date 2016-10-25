package org.alien4cloud.tosca.catalog;

import java.nio.file.Path;

import alien4cloud.plugin.aop.Overridable;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingResult;

/**
 * Provide a hook to post process parsed archive for Alien plugins, so that a plugin can eventually validate the archive and add parsing errors, warning or info
 */
public interface IArchivePostProcessor {

    /**
     * Post process an archive to enrich the result
     * 
     * @param archive path to the archive to post process
     * @param parsedArchive the archive parse result
     * @param workspace the workspace target
     * @return enriched parse result
     */
    @Overridable
    ParsingResult<ArchiveRoot> process(Path archive, ParsingResult<ArchiveRoot> parsedArchive, String workspace);
}
