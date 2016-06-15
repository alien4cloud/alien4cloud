package alien4cloud.tosca;

import org.springframework.stereotype.Component;

import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingResult;

@Component
public class ArchivePostProcessor {
    /**
     * Post process the archive: For every definition of the model it fills the id fields in the TOSCA elements from the key of the elements map.
     * 
     * @param parsedArchive The archive to post process
     */
    public ParsingResult<ArchiveRoot> process(ParsingResult<ArchiveRoot> parsedArchive) {
        Topology topology = parsedArchive.getResult().getTopology();
        if (topology != null) {
            TopologyUtils.normalizeAllNodeTemplateName(topology, parsedArchive);
        }
        return parsedArchive;
    }
}