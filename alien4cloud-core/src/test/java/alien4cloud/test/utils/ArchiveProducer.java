package alien4cloud.test.utils;

import java.io.IOException;
import java.nio.file.Paths;

import alien4cloud.utils.FileUtil;

public class ArchiveProducer {

    public static void main(String[] args) throws IOException {
        // Valid example files
        FileUtil.zip(Paths.get("/Users/mkv/alien/alien4cloud-parent/alien4cloud-rest-it/src/test/resources/data/csars/snapshot-test/valid"), Paths.get("alien4cloud-core/src/test/resources/examples/snaphost-test.csar"));
        // FileUtil.zip(Paths.get("src/test/resources/alien/tosca/container/csar/osgi"), Paths.get("src/test/resources/examples/osgi-types-1.0.csar"));
        //
        // // Invalid definition file
        // FileUtil.zip(Paths.get("../alien4cloud-rest-it/src/test/resources/data/csars/definition/missing"),
        // Paths.get("src/test/resources/examples/definition-missing.csar"));
        // FileUtil.zip(Paths.get("../alien4cloud-rest-it/src/test/resources/data/csars/definition/erroneous"),
        // Paths.get("src/test/resources/examples/definition-erroneous.csar"));
        //
        // // Invalid metadata file
        // FileUtil.zip(Paths.get("../alien4cloud-rest-it/src/test/resources/data/csars/metadata/missing"),
        // Paths.get("src/test/resources/examples/metadata-missing.csar"));
        // FileUtil.zip(Paths.get("../alien4cloud-rest-it/src/test/resources/data/csars/metadata/erroneous"),
        // Paths.get("src/test/resources/examples/metadata-erroneous.csar"));
        // FileUtil.zip(Paths.get("../alien4cloud-rest-it/src/test/resources/data/csars/metadata/validationFailure"),
        // Paths.get("src/test/resources/examples/metadata-validationFailure.csar"));
        //
        // // Invalid icon file
        // FileUtil.zip(Paths.get("../alien4cloud-rest-it/src/test/resources/data/csars/icon/missing"), Paths.get("src/test/resources/examples/icon-missing.csar"));
        // FileUtil.zip(Paths.get("../alien4cloud-rest-it/src/test/resources/data/csars/icon/erroneous"),
        // Paths.get("src/test/resources/examples/icon-erroneous.csar"));
        //
        // // Invalid duplicated file
        // FileUtil.zip(Paths.get("../alien4cloud-rest-it/src/test/resources/data/csars/definition/duplicated"),
        // Paths.get("src/test/resources/examples/definition-duplicated.csar"));
    }
}
