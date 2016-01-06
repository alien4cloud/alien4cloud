package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import alien4cloud.utils.FileUtil;

/**
 * Use this class to export all ES index as JSON for the UI tests.
 */
public class ExportESIndex {

    public static void main(String[] args) throws IOException {
        String pathToAlien = System.getProperty("user.home") + "/.alien";
        String pathToData = "src/test/webapp/e2e/_data";

        // Delete images first
        FileUtil.delete(Paths.get(pathToData + "/images"));
        FileUtil.copy(Paths.get(pathToAlien + "/images"), Paths.get(pathToData + "/images"));

        export("curl -X POST \"http://localhost:9200/imagedata/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 1000,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/imagedatas.json"));

        // Application
        export("curl -X POST \"http://localhost:9200/application/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 1000,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/applications.json"));
        export("curl -X POST \"http://localhost:9200/applicationenvironment/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 1000,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/applicationenvironments.json"));
        export("curl -X POST \"http://localhost:9200/applicationversion/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 1000,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/applicationversions.json"));

        // Components
        export("curl -X POST \"http://localhost:9200/csar/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 1000,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/csars.json"));
        export("curl -X POST \"http://localhost:9200/csargitrepository/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 1000,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/csargitrepositories.json"));
        export("curl -X POST \"http://localhost:9200/toscaelement/indexedartifacttype/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 1000,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/indexedartifacttypes.json"));
        export("curl -X POST \"http://localhost:9200/toscaelement/indexedcapabilitytype/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 1000,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/indexedcapabilitytypes.json"));
        export("curl -X POST \"http://localhost:9200/toscaelement/indexeddatatype/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 1000,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/indexeddatatypes.json"));
        export("curl -X POST \"http://localhost:9200/toscaelement/indexednodetype/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 1000,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/indexednodetypes.json"));
        export("curl -X POST \"http://localhost:9200/toscaelement/indexedrelationshiptype/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 1000,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/indexedrelationshiptypes.json"));

        // Topology templates
        export("curl -X POST \"http://localhost:9200/topologytemplate/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 100,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/topologytemplates.json"));
        export("curl -X POST \"http://localhost:9200/topologytemplateversion/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 100,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/topologytemplateversions.json"));

        // Topologies
        export("curl -X POST \"http://localhost:9200/topology/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 100,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/topologies.json"));

        // Users and groups
        export("curl -X POST \"http://localhost:9200/user/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 100,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/users.json"));
        export("curl -X POST \"http://localhost:9200/group/_search?pretty=true\" -d '{\"from\" : 0, \"size\" : 100,\"sort\": { \"_uid\": { \"order\": \"asc\" }},\"query\" : {\"match_all\" : {}}}' | grep _source",
                Paths.get(pathToData + "/groups.json"));
    }

    private static void export(String command, Path targetFile) throws IOException {
        ExecReturn execReturn = RuntimeExec.execCommand(command, null);
        FileWriter fw = new FileWriter(targetFile.toFile());
        fw.write("[" + System.lineSeparator());
        for (int i = 0; i < execReturn.getResultLines().length; i++) {
            String line = execReturn.getResultLines()[i];
            line = line.trim();
            line = line.replaceAll("\"_source\":", "");
            line = "  " + line;
            if (i < execReturn.getResultLines().length - 1) {
                if (!line.endsWith(",")) {
                    line = line + ",";
                }
            } else {
                if (line.endsWith(",")) {
                    line = line.substring(0, line.length() - 1);
                }
            }
            line = line + System.lineSeparator();
            fw.write(line);
        }
        fw.write("]");
        fw.close();
    }
}