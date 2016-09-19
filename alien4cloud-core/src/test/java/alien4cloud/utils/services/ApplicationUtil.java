package alien4cloud.utils.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Resource;

import org.alien4cloud.tosca.catalog.ArchiveParser;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.common.AlienConstants;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationUtil {
    @Resource
    private ArchiveParser parser;

    @SneakyThrows
    public Topology parseYamlTopology(String topologyPath) throws IOException, ParsingException {
        Path zipPath = Files.createTempFile("csar", ".zip");
        FileUtil.zip(Paths.get(topologyPath + ".yml"), zipPath);
        ParsingResult<ArchiveRoot> parsingResult = parser.parse(zipPath, AlienConstants.GLOBAL_WORKSPACE_ID);
        return parsingResult.getResult().getTopology();
    }
}
