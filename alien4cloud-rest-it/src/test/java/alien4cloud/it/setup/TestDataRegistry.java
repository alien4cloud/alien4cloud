package alien4cloud.it.setup;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Maps;

@Slf4j
public class TestDataRegistry {
    public static final Map<Path, Path> FOLDER_TO_ZIP = Maps.newHashMap();
    public static final Map<String, Path> CONDITION_TO_PATH = Maps.newHashMap();

    static {
        addConditionFolder("tosca base types 1.0", "src/test/resources/data/csars/tosca-base-types-1.0");
        addConditionFolder("tosca base types 2.0", "src/test/resources/data/csars/tosca-base-types-2.0");
        addConditionFolder("tosca base types 3.0", "src/test/resources/data/csars/tosca-base-types-3.0");
        addConditionFolder("sample java types 1.0", "src/test/resources/data/csars/sample/java-types-1.0");
        addConditionFolder("sample java types 2.0", "src/test/resources/data/csars/sample/java-types-2.0");
        addConditionFolder("sample java types 3.0", "src/test/resources/data/csars/sample/java-types-3.0");

        addConditionFolder("ubuntu types 0.1", "src/test/resources/data/csars/sample/ubuntu-types-0.1");
        addConditionFolder("sample apache lb types 0.1", "src/test/resources/data/csars/sample/apache-lb-types-0.1");
        addConditionFolder("sample apache lb types 0.2", "src/test/resources/data/csars/sample/apache-lb-types-0.2");

        addConditionFolder("constraints", "src/test/resources/data/csars/definition/constraints");
        addConditionFolder("invalid (definition file not found)", "src/test/resources/data/csars/definition/missing");
        addConditionFolder("invalid (definition file is not valid yaml file)", "src/test/resources/data/csars/definition/erroneous");
        addConditionFolder("invalid (definition file's declaration duplicated)", "src/test/resources/data/csars/definition/duplicated");
        addConditionFolder("invalid (ALIEN-META.yaml not found)", "src/test/resources/data/csars/metadata/missing");
        addConditionFolder("invalid (ALIEN-META.yaml invalid)", "src/test/resources/data/csars/metadata/erroneous");
        addConditionFolder("invalid (ALIEN-META.yaml fail validation)", "src/test/resources/data/csars/metadata/validationFailure");
        addConditionFolder("invalid (icon not found)", "src/test/resources/data/csars/icon/missing");
        addConditionFolder("invalid (icon invalid)", "src/test/resources/data/csars/icon/erroneous");
        addConditionFolder("snapshot", "src/test/resources/data/csars/snapshot");
        addConditionFolder("relationship test types", "src/test/resources/data/csars/relationship-test-types");
        addConditionFolder("valid-csar-with-test", "src/test/resources/data/csars/snapshot-test/snapshot-test-valid");
        addConditionFolder("valid-csar-with-update1", "src/test/resources/data/csars/snapshot-test/snapshot-test-update1");
        addConditionFolder("valid-csar-with-update2", "src/test/resources/data/csars/snapshot-test/snapshot-test-update2");
        addConditionFolder("valid-csar-with-update3", "src/test/resources/data/csars/snapshot-test/snapshot-test-update3");
        addConditionFolder("csar-test-no-topology", "src/test/resources/data/csars/snapshot-test/missing-topology-yaml");

        addConditionFolder("topology-singlecompute", "src/test/resources/data/csars/topology_template/topology-singlecompute");
        addConditionFolder("topology apache", "src/test/resources/data/csars/topology_template/topology-apache");
        addConditionFolder("topology custom types", "src/test/resources/data/csars/topology_template/topology-custom-types");
        addConditionFolder("topology-error-missingtype", "src/test/resources/data/csars/topology_template/topology-error-missingtype");
        addConditionFolder("topology-unknown-req", "src/test/resources/data/csars/topology_template/topology-unknown-req");
        addConditionFolder("topology-unknown-reqtarget", "src/test/resources/data/csars/topology_template/topology-unknown-reqtarget");
        addConditionFolder("topology-unknown-capability", "src/test/resources/data/csars/topology_template/topology-unknown-capability");
        addConditionFolder("topology-unknown-capability-short-notation",
                "src/test/resources/data/csars/topology_template/topology-unknown-capability-short-notation");
        addConditionFolder("topology-unknown-relationshiptype", "src/test/resources/data/csars/topology_template/topology-unknown-relationshiptype");
        addConditionFolder("topology_inputs", "src/test/resources/data/csars/topology_template/topology_inputs");
        addConditionFolder("topology_outputs", "src/test/resources/data/csars/topology_template/topology_outputs");
        addConditionFolder("topology-capacility-prop", "src/test/resources/data/csars/topology_template/topology-capacility-prop");
        addConditionFolder("topology-capacility-prop-unkown", "src/test/resources/data/csars/topology_template/topology-capacility-prop-unkown");
        addConditionFolder("topology-capacility-unkown", "src/test/resources/data/csars/topology_template/topology-capacility-unkown");
        addConditionFolder("topology-template-relationship-funtionprop",
                "src/test/resources/data/csars/topology_template/topology-template-relationship-funtionprop");

        // test uploading an unzipped file (do not zip it)
        CONDITION_TO_PATH.put("unzipped", Paths.get("src/test/resources/alien/rest/csars/upload.feature"));
        CONDITION_TO_PATH.put("alien-base-types",
                Paths.get(PrepareTestData.RELATIVE_ARCHIVES_TARGET_PATH_ROOT + "alien-base-types/alien-base-types-1.0-SNAPSHOT.zip"));
        CONDITION_TO_PATH.put("alien-extended-storage-types",
                Paths.get(PrepareTestData.RELATIVE_ARCHIVES_TARGET_PATH_ROOT + "alien-base-types/alien-extended-storage-types-1.0-SNAPSHOT.zip"));
        CONDITION_TO_PATH.put("tosca-normative-types", Paths.get(PrepareTestData.RELATIVE_ARCHIVES_TARGET_PATH_ROOT + "tosca-normative-types.zip"));
    }

    public static void addConditionFolder(String condition, String folderPathStr) {
        Path folderPath = Paths.get(PrepareTestData.BASEDIR + folderPathStr);
        Path zipPath = Paths.get(PrepareTestData.ARCHIVES_TARGET_PATH, folderPath.getFileName() + ".csar");
        FOLDER_TO_ZIP.put(folderPath, zipPath);
        CONDITION_TO_PATH.put(condition, zipPath);
    }
}
