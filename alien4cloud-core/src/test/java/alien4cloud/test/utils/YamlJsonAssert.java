package alien4cloud.test.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.utils.YamlParserUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Slf4j
public class YamlJsonAssert {

    public static class YamlJsonNotEqualsException extends RuntimeException {

        private static final long serialVersionUID = -4821764413592118483L;

        public YamlJsonNotEqualsException(String message, Throwable cause) {
            super(message, cause);
        }

        public YamlJsonNotEqualsException(String message, String expected, String actual) {
            super("Expected : [" + expected + "], but actual is [" + actual + "], " + message);
        }

        public YamlJsonNotEqualsException(String message) {
            super(message);
        }
    }

    public static enum DocumentType {
        JSON, YAML
    }

    private static final ObjectMapper YAML_OBJECT_MAPPER = YamlParserUtil.createYamlObjectMapper();

    private static final ObjectMapper JSON_OBJECT_MAPPER = newJsonObjectMapper();

    private static ObjectMapper newJsonObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    public static void assertEquals(String expected, String actual, DocumentType documentType) throws IOException, YamlJsonNotEqualsException {
        assertEquals(expected, actual, null, documentType);
    }

    public static void assertEquals(String expected, String actual, Set<String> ignoredPaths, DocumentType documentType) throws IOException,
            YamlJsonNotEqualsException {
        ObjectMapper mapper;
        switch (documentType) {
        case JSON:
            mapper = JSON_OBJECT_MAPPER;
            break;
        case YAML:
            mapper = YAML_OBJECT_MAPPER;
            break;
        default:
            throw new IllegalArgumentException("Unsupported document type [" + documentType + "]");
        }
        JsonNode expectedNode = mapper.readTree(expected);
        JsonNode actualNode = mapper.readTree(actual);
        LinkedList<String> path = new LinkedList<String>();
        path.add("/");
        assertEquals(expectedNode, actualNode, path, ignoredPaths);
    }

    private static boolean isIgnored(String path, Set<String> ignoredPaths) {
        if (ignoredPaths != null) {
            for (String ignoredPath : ignoredPaths) {
                if (path.matches(ignoredPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void fail(String message, String currentPath, Set<String> ignoredPaths) {
        if (!isIgnored(currentPath, ignoredPaths)) {
            throw new YamlJsonNotEqualsException(message);
        } else {
            log.warn(message);
        }
    }

    private static void equals(String message, String currentPath, Set<String> ignoredPaths, String expected, String actual) {
        if (!isIgnored(currentPath, ignoredPaths)) {
            if (!Objects.equal(expected, actual)) {
                throw new YamlJsonNotEqualsException(message, expected, actual);
            }
        } else {
            log.warn("Ignoring value comparison for path [" + currentPath + "], expected = [" + expected + "], actual = [" + actual + "]");
        }
    }

    private static void assertArrayEquals(JsonNode expected, JsonNode actual, LinkedList<String> path, Set<String> ignoredPaths) {
        if (expected.isArray()) {
            // Expected is also an array try to compare
            Iterator<JsonNode> expectedElements = expected.elements();
            Iterator<JsonNode> actualElements = actual.elements();
            int arrayIndex = 0;
            // When comparing array must respect the same order, is this too strict ?
            while (actualElements.hasNext()) {
                JsonNode actualElement = actualElements.next();
                path.push("[" + arrayIndex + "]");
                String currentPath = getPrettyPath(path);
                if (expectedElements.hasNext()) {
                    JsonNode expectedElement = expectedElements.next();
                    // Compare array element
                    assertEquals(expectedElement, actualElement, path, ignoredPaths);
                } else {
                    String message = buildErrorMessage(currentPath, "Actual array contains more elements than expected");
                    fail(message, currentPath, ignoredPaths);
                }
                path.pop();
                arrayIndex++;
            }
        } else {
            String currentPath = getPrettyPath(path);
            String message = buildErrorMessage(currentPath, "Not expecting array at this level");
            fail(message, currentPath, ignoredPaths);
        }
    }

    private static void assertValueEquals(JsonNode expected, JsonNode actual, LinkedList<String> path, Set<String> ignoredPaths) {
        if (expected.isValueNode()) {
            String expectedValue = expected.asText();
            String actualValue = actual.asText();
            String currentPath = getPrettyPath(path);
            String message = buildErrorMessage(currentPath, "Value not equals ");
            if (log.isDebugEnabled()) {
                log.debug(currentPath + " : Comparing value expected = [" + expectedValue + "], actual = [" + actualValue + "]");
            }
            equals(message, currentPath, ignoredPaths, expectedValue, actualValue);
        } else {
            throw new YamlJsonNotEqualsException(buildErrorMessage(getPrettyPath(path), "Expecting keys : " + Sets.newHashSet(expected.fieldNames())
                    + " but not found"));
        }
    }

    private static void assertTreeEquals(JsonNode expected, JsonNode actual, LinkedList<String> path, Set<String> ignoredPaths) {
        // If we are here it means it's not an array
        // Getting children name
        Set<String> expectedFieldSet = Sets.newHashSet(expected.fieldNames());
        Set<String> actualFieldSet = Sets.newHashSet(actual.fieldNames());
        for (String actualField : actualFieldSet) {
            path.push(actualField);
            if (!expectedFieldSet.contains(actualField)) {
                String currentPath = getPrettyPath(path);
                String message = buildErrorMessage(currentPath, "Not expecting keys : [" + actualField + "]");
                fail(message, currentPath, ignoredPaths);
            } else {
                assertEquals(expected.get(actualField), actual.get(actualField), path, ignoredPaths);
            }
            path.pop();
        }
        // Remove all actual to check if there's not found children on actual
        expectedFieldSet.removeAll(actualFieldSet);
        if (expectedFieldSet.size() > 0) {
            for (String expectedField : expectedFieldSet) {
                path.push(expectedField);
                String currentPath = getPrettyPath(path);
                String message = buildErrorMessage(getPrettyPath(path), "Expecting keys : " + expectedFieldSet + " but not found");
                fail(message, currentPath, ignoredPaths);
                path.pop();
            }
        }
    }

    private static void failNotValueNode(JsonNode expected, JsonNode actual, LinkedList<String> path, Set<String> ignoredPaths) {
        // Expected is a value node, we check if all actual children is ignored
        if (expected.isNull() || (expected.isTextual() && expected.asText() == null) || (expected.isTextual() && expected.asText().isEmpty())) {
            // If we are here 1. expected is null or 2. expected is text and is null 3. expected is text but empty
            Set<String> actualFieldSet = Sets.newHashSet(actual.fieldNames());
            for (String actualField : actualFieldSet) {
                path.push(actualField);
                String currentPath = getPrettyPath(path);
                String message = buildErrorMessage(getPrettyPath(path), "Not expecting keys : [" + actualField + " ]");
                fail(message, currentPath, ignoredPaths);
                path.pop();
            }
        } else {
            // Expected is not null, it might be a real problem except if the parent path is ignored
            String currentPath = getPrettyPath(path);
            String message = buildErrorMessage(getPrettyPath(path), "Not expecting keys : [" + Sets.newHashSet(actual.fieldNames()) + " ]");
            fail(message, currentPath, ignoredPaths);
        }
    }

    private static void assertEquals(JsonNode expected, JsonNode actual, LinkedList<String> path, Set<String> ignoredPaths) {
        // Check if it's array node
        if (actual.isArray()) {
            // Assert that two array are equals
            assertArrayEquals(expected, actual, path, ignoredPaths);
            return;
        }

        // Check if it's value node
        if (actual.isValueNode()) {
            // Assert that two values are equals
            assertValueEquals(expected, actual, path, ignoredPaths);
            return;
        }

        // Actual is not a value node, may expected be a value node, we must check that
        if (expected.isValueNode()) {
            // Actual is not a value node, expected is a value node it's an erroneous case
            failNotValueNode(expected, actual, path, ignoredPaths);
            return;
        }

        // If we reach here it means both are tree nodes
        assertTreeEquals(expected, actual, path, ignoredPaths);
    }

    private static String getPrettyPath(LinkedList<String> pathStack) {
        StringBuilder buffer = new StringBuilder();
        boolean slashAppended = false;
        for (String path : Lists.reverse(pathStack)) {
            buffer.append(path);
            if (!path.equals("/")) {
                buffer.append('/');
                // Too ugly
                slashAppended = true;
            }
        }
        if (slashAppended) {
            buffer.setLength(buffer.length() - 1);
        }
        return buffer.toString();
    }

    private static String buildErrorMessage(String path, String customMessage) {
        return "Current path where assertion failed '" + path + "' : " + customMessage;
    }
}
