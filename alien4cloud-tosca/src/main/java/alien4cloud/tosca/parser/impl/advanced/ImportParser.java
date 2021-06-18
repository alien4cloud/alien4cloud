package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.VersionUtil;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import javax.annotation.Resource;

@Slf4j
@Component
public class ImportParser implements INodeParser<CSARDependency> {

    @Resource
    private LaxImportParser laxImportParser;

    @Value("${features.archive_indexer.accept_missing_requirement:#{false}}")
    private boolean acceptMissingRequirementDependency;

    @Override
    public CSARDependency parse(Node node, ParsingContextExecution context) {
        CSARDependency dependency = laxImportParser.parse(node, context);
        if (dependency == null) {
            return null;
        }
        String valueAsString = dependency.getName() + ":" + dependency.getVersion();
        String currentArchiveVersion = context.<ArchiveRoot> getRootObj().getArchive().getVersion();
        Csar csar = ToscaContext.get().getArchive(dependency.getName(), dependency.getVersion(), acceptMissingRequirementDependency);
        log.debug("Import {} {} {}", dependency.getName(), dependency.getVersion(), csar);
        if (null == csar.getHash()) {
            // in case of acceptMissingRequirementDependency == true, a CSAR is returned with null hashCode, this is an unresolved dependency
            // error is not a blocker, as long as no type is missing we just mark it as a warning.
            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.MISSING_DEPENDENCY, "Import definition is not valid",
                    node.getStartMark(), "Specified dependency is not found in Alien 4 Cloud repository.", node.getEndMark(), valueAsString));
            ToscaContext.get().addDependency(dependency, acceptMissingRequirementDependency);
            return dependency;
        } else {
            if (!VersionUtil.isSnapshot(currentArchiveVersion) && VersionUtil.isSnapshot(dependency.getVersion())) {
                // the current archive is a released version but depends on a snapshot version
                context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.SNAPSHOT_DEPENDENCY, "Import definition is not valid",
                        node.getStartMark(), "A released archive cannot depends on snapshots archives.", node.getEndMark(), valueAsString));
            }
            dependency.setHash(csar.getHash());
            ToscaContext.get().addDependency(dependency);
            return dependency;
        }
    }

}
