package alien4cloud.tosca.container.validation;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Path;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import alien4cloud.tosca.container.model.CSARDependency;
import alien4cloud.tosca.container.model.Definitions;
import alien4cloud.tosca.container.model.type.NodeType;
import alien4cloud.tosca.container.validation.CSARError;
import alien4cloud.tosca.container.validation.CSARErrorCode;
import alien4cloud.tosca.container.validation.CSARErrorFactory;
import alien4cloud.tosca.container.validation.CSARValidationResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CSARValidationResultTest {

    @Test
    public void testSerializationDuplicatedRoundTrip() throws IOException {
        Map<String, Set<CSARError>> errors = Maps.newHashMap();
        Set<CSARError> fileErrors = Sets.newHashSet();
        fileErrors.add(CSARErrorFactory.createDuplicatedTypeError(CSARErrorCode.DUPLICATED_ELEMENT_DECLARATION, "bad.element"));
        errors.put("file", fileErrors);
        CSARValidationResult result = new CSARValidationResult(errors);
        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(result);
        System.out.println(serialized);
        CSARValidationResult resultRoundTripped = mapper.readValue(serialized, CSARValidationResult.class);
        Assert.assertEquals(result.getErrors().size(), resultRoundTripped.getErrors().size());
        Assert.assertEquals(result.getErrors().get("file").size(), resultRoundTripped.getErrors().get("file").size());
    }

    @Test
    public void testSerializationParsingErrorRoundTrip() throws IOException {
        Map<String, Set<CSARError>> errors = Maps.newHashMap();
        Set<CSARError> fileErrors = Sets.newHashSet();
        fileErrors.add(CSARErrorFactory.createParsingError(CSARErrorCode.ERRONEOUS_ARCHIVE_FILE, 0, 0, "Archive is bad"));
        errors.put("Archive", fileErrors);
        CSARValidationResult result = new CSARValidationResult(errors);
        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(result);
        System.out.println(serialized);
        CSARValidationResult resultRoundTripped = mapper.readValue(serialized, CSARValidationResult.class);
        Assert.assertEquals(result.getErrors().size(), resultRoundTripped.getErrors().size());
        Assert.assertEquals(result.getErrors().get("Archive").size(), resultRoundTripped.getErrors().get("Archive").size());
    }

    @Test
    public void testSerializationValidationCompilationErrorRoundTrip() throws IOException {
        Map<String, Set<CSARError>> errors = Maps.newHashMap();

        Set<CSARError> fileErrors = Sets.newHashSet();
        NodeType nodeType = new NodeType();
        nodeType.setId("node1");
        fileErrors.add(CSARErrorFactory.createTypeNotFoundError(CSARErrorCode.SUPER_TYPE_NOT_FOUND, nodeType.getId(), "toto"));
        fileErrors.add(CSARErrorFactory.createTypeNotFoundError(CSARErrorCode.TYPE_NOT_FOUND, nodeType.getId(), "tata"));
        @SuppressWarnings("unchecked")
        ConstraintViolation<Definitions> violation = Mockito.mock(ConstraintViolation.class);
        Mockito.when(violation.getMessage()).thenReturn("cannot be null");
        Path violationPath = Mockito.mock(Path.class);
        Mockito.when(violationPath.toString()).thenReturn("property.path");
        Mockito.when(violation.getPropertyPath()).thenReturn(violationPath);
        Mockito.when(violation.getLeafBean()).thenReturn(new CSARDependency());
        fileErrors.add(CSARErrorFactory.createValidationError(violation));
        errors.put("file1", fileErrors);
        fileErrors = Sets.newHashSet();
        fileErrors.clear();
        nodeType = new NodeType();
        nodeType.setId("node2");
        fileErrors.add(CSARErrorFactory.createTypeNotFoundError(CSARErrorCode.SUPER_TYPE_NOT_FOUND, nodeType.getId(), "tata"));
        fileErrors.add(CSARErrorFactory.createTypeNotFoundError(CSARErrorCode.TYPE_NOT_FOUND, nodeType.getId(), "titi"));
        errors.put("file2", fileErrors);

        CSARValidationResult result = new CSARValidationResult(errors);
        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(result);
        CSARValidationResult resultRoundTripped = mapper.readValue(serialized, CSARValidationResult.class);
        Assert.assertEquals(result.getErrors().size(), resultRoundTripped.getErrors().size());
        Assert.assertEquals(result.getErrors().get("file1").size(), resultRoundTripped.getErrors().get("file1").size());
        Assert.assertEquals(result.getErrors().get("file2").size(), resultRoundTripped.getErrors().get("file2").size());
    }
}
