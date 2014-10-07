package alien4cloud.cloud;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.ComputeTemplate;
import alien4cloud.tosca.container.model.NormativeComputeConstants;
import alien4cloud.tosca.container.model.topology.NodeTemplate;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.Version;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Slf4j
@Service
public class CloudResourceMatcherService {

    @Resource
    private CloudImageService cloudImageService;

    public static final String COMPUTE_TYPE = "tosca.nodes.Compute";

    /**
     * This method browse topology's node templates and return those that need to be matched to cloud's resources
     * 
     * @param topology the topology to check
     * @return all node template that must be matched
     */
    public Map<String, NodeTemplate> getMatchableTemplates(Topology topology) {
        Map<String, NodeTemplate> allNodeTemplates = topology.getNodeTemplates();
        Map<String, NodeTemplate> matchableNodeTemplates = Maps.newHashMap();
        if (allNodeTemplates == null) {
            return matchableNodeTemplates;
        }
        for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : allNodeTemplates.entrySet()) {
            if (COMPUTE_TYPE.equals(nodeTemplateEntry.getValue().getType())) {
                // TODO check also everything extending compute, check also network and other cloud related resources ...
                matchableNodeTemplates.put(nodeTemplateEntry.getKey(), nodeTemplateEntry.getValue());
            }
        }
        return matchableNodeTemplates;
    }

    /**
     * Get all available image for a compute by filtering with its properties on OS information
     * 
     * @param cloud the cloud
     * @param computeTemplate the compute to search for images
     * @return the available images on the cloud
     */
    public Set<CloudImage> getAvailableImagesForCompute(Cloud cloud, NodeTemplate computeTemplate) {
        if (!COMPUTE_TYPE.equals(computeTemplate.getType())) {
            throw new InvalidArgumentException("Node is not a compute but of type [" + computeTemplate.getType() + "]");
        }
        Map<String, String> computeTemplateProperties = computeTemplate.getProperties();
        // Only get active templates
        Set<ComputeTemplate> templates = cloud.getComputeTemplates();
        Set<String> availableImageIds = Sets.newHashSet();
        for (ComputeTemplate template : templates) {
            if (template.isEnabled()) {
                availableImageIds.add(template.getCloudImageId());
            }
        }
        Map<String, CloudImage> availableImages = cloudImageService.getMultiple(availableImageIds);
        Set<CloudImage> matchedImages = Sets.newHashSet();
        for (CloudImage cloudImage : availableImages.values()) {
            if (!match(computeTemplateProperties, NormativeComputeConstants.OS_ARCH, cloudImage.getOsArch(), new TextValueParser(), new EqualMatcher<String>())) {
                continue;
            }
            if (!match(computeTemplateProperties, NormativeComputeConstants.OS_DISTRIBUTION, cloudImage.getOsDistribution(), new TextValueParser(),
                    new EqualMatcher<String>())) {
                continue;
            }
            if (!match(computeTemplateProperties, NormativeComputeConstants.OS_TYPE, cloudImage.getOsType(), new TextValueParser(),
                    new EqualMatcher<String>())) {
                continue;
            }
            if (!match(computeTemplateProperties, NormativeComputeConstants.OS_VERSION, VersionUtil.parseVersion(cloudImage.getOsVersion()),
                    new VersionValueParser(),
                    new GreaterOrEqualValueMatcher())) {
                continue;
            }
            matchedImages.add(cloudImage);
        }
        return matchedImages;
    }

    /**
     * Get all available flavors for the given compute and the given image on the given cloud
     * 
     * @param cloud the cloud
     * @param computeTemplate the compute to search for flavors
     * @param cloudImage the image
     * @return the available flavors for the compute and the image on the given cloud
     */
    public Set<CloudImageFlavor> getAvailableFlavorForCompute(Cloud cloud, NodeTemplate computeTemplate, CloudImage cloudImage) {
        if (!COMPUTE_TYPE.equals(computeTemplate.getType())) {
            throw new InvalidArgumentException("Node is not a compute but of type [" + computeTemplate.getType() + "]");
        }
        Map<String, CloudImageFlavor> allFlavors = Maps.newHashMap();
        for (CloudImageFlavor flavor : cloud.getFlavors()) {
            allFlavors.put(flavor.getId(), flavor);
        }
        Set<CloudImageFlavor> availableFlavors = Sets.newHashSet();
        Set<ComputeTemplate> allTemplates = cloud.getComputeTemplates();
        for (ComputeTemplate template : allTemplates) {
            if (template.isEnabled() && template.getCloudImageId().equals(cloudImage.getId())) {
                // get flavors that correspond to the given cloud image from all active compute template
                availableFlavors.add(allFlavors.get(template.getCloudImageFlavorId()));
            }
        }
        Set<CloudImageFlavor> matchedFlavors = Sets.newHashSet();
        Map<String, String> computeTemplateProperties = computeTemplate.getProperties();
        for (CloudImageFlavor flavor : availableFlavors) {
            if (!match(computeTemplateProperties, NormativeComputeConstants.NUM_CPUS, flavor.getNumCPUs(), new IntegerValueParser(),
                    new GreaterOrEqualValueMatcher<Integer>())) {
                continue;
            }
            if (!match(computeTemplateProperties, NormativeComputeConstants.DISK_SIZE, flavor.getDiskSize(), new LongValueParser(),
                    new GreaterOrEqualValueMatcher<Long>())) {
                continue;
            }
            if (!match(computeTemplateProperties, NormativeComputeConstants.MEM_SIZE, flavor.getMemSize(), new LongValueParser(),
                    new GreaterOrEqualValueMatcher<Long>())) {
                continue;
            }
            matchedFlavors.add(flavor);
        }
        return matchedFlavors;
    }

    private static interface ValueParser<T> {

        T parseValue(String textValue);

    }

    private static class IntegerValueParser implements ValueParser<Integer> {
        @Override
        public Integer parseValue(String textValue) {
            return Integer.parseInt(textValue);
        }
    }

    private static class LongValueParser implements ValueParser<Long> {
        @Override
        public Long parseValue(String textValue) {
            return Long.parseLong(textValue);
        }
    }

    private static class TextValueParser implements ValueParser<String> {
        @Override
        public String parseValue(String textValue) {
            return textValue;
        }
    }

    private static class VersionValueParser implements ValueParser<Version> {
        @Override
        public Version parseValue(String textValue) {
            return VersionUtil.parseVersion(textValue);
        }
    }

    private static interface ValueMatcher<T> {

        boolean matchValue(T expected, T actual);

    }

    private static class GreaterOrEqualValueMatcher<T extends Comparable> implements ValueMatcher<T> {
        @Override
        public boolean matchValue(T expected, T actual) {
            // Actual must be greater or equal than expected
            return actual.compareTo(expected) >= 0;
        }
    }

    private static class EqualMatcher<T> implements ValueMatcher<T> {
        @Override
        public boolean matchValue(T expected, T actual) {
            // Actual must be equal strictly as expected
            return expected.equals(actual);
        }
    }

    private <T> boolean match(Map<String, String> properties, String keyToCheck, T actualValue, ValueParser<T> valueParser, ValueMatcher<T> valueMatcher) {
        try {
            String expectedValue = properties.get(keyToCheck);
            if (expectedValue == null || expectedValue.isEmpty()) {
                return true;
            } else {
                return valueMatcher.matchValue(valueParser.parseValue(expectedValue), actualValue);
            }
        } catch (Exception e) {
            log.warn("Error happened while matching key [" + keyToCheck + "], actual value [" + actualValue + "], properties [" + properties + "]", e);
            return false;
        }
    }
}
