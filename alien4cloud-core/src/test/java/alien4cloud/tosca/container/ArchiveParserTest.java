//package alien4cloud.tosca.container;
//
//import java.io.IOException;
//import java.nio.file.FileSystem;
//import java.nio.file.FileSystems;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map.Entry;
//import java.util.Set;
//
//import javax.validation.Validation;
//import javax.validation.Validator;
//
//import lombok.extern.slf4j.Slf4j;
//
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.Mockito;
//
//import alien4cloud.rest.utils.JsonUtil;
//import alien4cloud.test.utils.YamlJsonAssert;
//import alien4cloud.test.utils.YamlJsonAssert.DocumentType;
//import alien4cloud.tosca.ArchiveParser;
//import alien4cloud.tosca.ArchivePostProcessor;
//import alien4cloud.tosca.container.exception.CSARIOException;
//import alien4cloud.tosca.container.exception.CSARParsingException;
//import alien4cloud.tosca.container.services.csar.ICSARRepositorySearchService;
//import alien4cloud.tosca.container.validation.CSARError;
//import alien4cloud.tosca.container.validation.CSARValidationResult;
//import alien4cloud.tosca.parser.ParsingException;
//import alien4cloud.utils.FileUtil;
//import alien4cloud.utils.YamlParserUtil;
//
//import com.google.common.collect.Sets;
//
//@Slf4j
//public class ArchiveParserTest {
//
//    private static final Path CSAR_OUTPUT_FOLDER = Paths.get("./target/csarTests");
//
//    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
//
//    @Before
//    public void cleanup() throws IOException {
//        // Clean up the csar
//        FileUtil.delete(CSAR_OUTPUT_FOLDER);
//    }
//
//    @Test(expected = ParsingException.class)
//    public void loadingDefinitionWithWrongPathShouldThrowIOExeption() throws ParsingException {
//        ArchiveParser parser = new ArchiveParser();
//        parser.parse(Paths.get("file.thatdoesnotexist"));
//    }
//
//    @Test
//    public void loadingJavaArchive() throws IOException, CSARParsingException {
//        Path csarSourceFolder = Paths.get("./src/main/resources/java-types/1.0");
//        testValidToscaArchive(csarSourceFolder, CSAR_OUTPUT_FOLDER, "java.csar");
//    }
//
//    @Test
//    public void loadingWebappToscaArchive() throws IOException, CSARParsingException {
//        Path csarSourceFolder = Paths.get("./src/test/resources/alien/tosca/container/csar/webapp");
//        testValidToscaArchive(csarSourceFolder, CSAR_OUTPUT_FOLDER, "webapp.csar");
//    }
//
//    @Test
//    public void loadingSimpleToscaArchive() throws IOException, CSARParsingException {
//        Path csarSourceFolder = Paths.get("./src/test/resources/alien/tosca/container/csar/valid");
//        testValidToscaArchive(csarSourceFolder, CSAR_OUTPUT_FOLDER, "valid.csar");
//    }
//
//    @SuppressWarnings("unchecked")
//    private void testValidToscaArchive(Path csarSourceFolder, Path csarFolderForTesting, String csarName) throws IOException, CSARParsingException {
//        Path csarFileForTesting = Paths.get(csarFolderForTesting.toString(), csarName);
//        ArchiveParser parser = new ArchiveParser();
//        // Zip the csarSourceFolder and write it to csarFileForTesting
//        FileUtil.zip(csarSourceFolder, csarFileForTesting);
//        // Parse the archive for definitions
//        CloudServiceArchive csa = parser.parseArchive(csarFileForTesting);
//        // Make verification that the parsing process returned some definitions
//        Assert.assertFalse(csa.getDefinitions().isEmpty());
//        Assert.assertFalse(csa.getMeta().getDefinitions().isEmpty());
//        // Post process the archive
//        ArchivePostProcessor postProcessor = new ArchivePostProcessor();
//        postProcessor.postProcessArchive(csa);
//        log.info(JsonUtil.toString(csa.getArchiveInheritableElements()));
//        // Verify that the post processor created some elements
//        Assert.assertFalse(csa.getArchiveInheritableElements().isEmpty());
//        // Mock the search service to return always true
//        ICSARRepositorySearchService searchService = Mockito.mock(ICSARRepositorySearchService.class);
//        Mockito.when(searchService.isElementExistInDependencies(Mockito.any(Class.class), Mockito.any(String.class), Mockito.any(List.class))).thenReturn(true);
//        // Validate the archive
//        ArchiveValidator validator = new ArchiveValidator();
//        validator.setValidator(VALIDATOR);
//        validator.setSearchService(searchService);
//        CSARValidationResult result = validator.validateArchive(csa);
//        Assert.assertTrue("CSAR must be valid <" + result.toString() + ">", result.isValid());
//        assertArchiveDefinitions(csarFileForTesting, csa.getMeta().getDefinitions());
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void loadingInvalidToscaArchive() throws IOException, CSARParsingException {
//        ArchiveParser parser = new ArchiveParser();
//        Path csarFolderForTesting = Paths.get("./target/csarTests");
//        Path csarFileForTesting = Paths.get(csarFolderForTesting.toString(), "invalid.csar");
//        Path csarSourceFolder = Paths.get("./src/test/resources/alien/tosca/container/csar/invalid");
//        // Zip the csarSourceFolder and write it to csarFileForTesting
//        FileUtil.zip(csarSourceFolder, csarFileForTesting);
//        CloudServiceArchive csa = parser.parseArchive(csarFileForTesting);
//        ArchivePostProcessor postProcessor = new ArchivePostProcessor();
//        postProcessor.postProcessArchive(csa);
//        ICSARRepositorySearchService searchService = Mockito.mock(ICSARRepositorySearchService.class);
//        Mockito.when(searchService.isElementExistInDependencies(Mockito.any(Class.class), Mockito.eq("tosca.requirements.Java"), Mockito.any(List.class)))
//                .thenReturn(false);
//        ArchiveValidator validator = new ArchiveValidator();
//        validator.setValidator(VALIDATOR);
//        validator.setSearchService(searchService);
//        CSARValidationResult result = validator.validateArchive(csa);
//        Assert.assertFalse("CSAR must be invalid", result.isValid());
//        Assert.assertEquals(2, result.getErrors().size());
//        Iterator<Entry<String, Set<CSARError>>> validationResultIterator = result.getErrors().entrySet().iterator();
//        Assert.assertEquals(1, validationResultIterator.next().getValue().size());
//        Assert.assertEquals(3, validationResultIterator.next().getValue().size());
//    }
//
//    private static void assertArchiveDefinitions(Path csar, List<String> definitions) throws IOException {
//        FileSystem csarFS;
//        try {
//            csarFS = FileSystems.newFileSystem(csar, null);
//        } catch (IOException e) {
//            throw new CSARIOException("Problem happened while accessing file [" + csar + "]");
//        }
//        for (String definition : definitions) {
//            Path definitionsPath = csarFS.getPath(definition);
//            String expected = FileUtil.readTextFile(definitionsPath);
//            String actual = YamlParserUtil.toYaml(YamlParserUtil.parseFromUTF8File(definitionsPath, Definitions.class));
//            if (log.isDebugEnabled()) {
//                log.debug("Verifying serialization + deserialization process for definition [" + definition + "]");
//                log.debug("_________________________________________________________________________________________________________");
//                log.debug("Expected [" + expected + "]");
//                log.debug("_________________________________________________________________________________________________________");
//                log.debug("Actual: [" + actual + "]");
//                log.debug("_________________________________________________________________________________________________________");
//            }
//            YamlJsonAssert.assertEquals(expected, actual, Sets.newHashSet(".+/abstract", ".+/final", ".+/max_instances", ".+/min_instances", ".+/lower_bound",
//                    ".+/upper_bound", ".+interfaces/lifecycle/operations/.+", ".+/properties/.+/required", ".+/properties/.+/password",
//                    ".+/constraints/.+/range_min_value", ".+/constraints/.+/range_max_value", ".+/input_parameters/.+/required"), DocumentType.YAML);
//        }
//    }
//
//}
