package alien4cloud.cloud;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.model.cloud.ActivableComputeTemplate;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.cloud.ComputeTemplate;
import alien4cloud.model.cloud.Network;
import alien4cloud.tosca.ToscaUtils;
import alien4cloud.tosca.container.model.NormativeComputeConstants;
import alien4cloud.tosca.container.model.NormativeNetworkConstants;
import alien4cloud.tosca.container.model.topology.NodeTemplate;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.Version;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Slf4j
@Service
public class CloudResourceMatcherService {

    @Resource
    private CloudImageService cloudImageService;

    /**
     * Match a topology to cloud resources and return cloud's matched resources each matchable resource of the topology
     *
     * @param topology the topology to check
     * @param cloud the cloud
     * @param cloudResourceMatcherConfig the matcher configuration
     * @param types the tosca types of all node of the topology
     * @return match result which contains the images that can be used, the flavors that can be used and their possible association
     */
    public CloudResourceTopologyMatchResult matchTopology(Topology topology, Cloud cloud, CloudResourceMatcherConfig cloudResourceMatcherConfig,
            Map<String, IndexedNodeType> types) {
        Map<ComputeTemplate, String> computeTemplateMapping = null;
        if (cloudResourceMatcherConfig != null) {
            computeTemplateMapping = cloudResourceMatcherConfig.getComputeTemplateMapping();
        }
        MatchableTemplates matchableNodes = getMatchableTemplates(topology, types);
        Map<String, List<ComputeTemplate>> computeMatchResult = Maps.newHashMap();
        Set<String> imageIds = Sets.newHashSet();
        Map<String, CloudImageFlavor> flavorMap = Maps.newHashMap();
        for (Map.Entry<String, NodeTemplate> computeTemplateEntry : matchableNodes.computeTemplates.entrySet()) {
            List<ComputeTemplate> computeTemplates = Lists.newArrayList();
            List<CloudImage> images = getAvailableImagesForCompute(cloud, computeTemplateEntry.getValue(), types.get(computeTemplateEntry.getKey()));
            for (CloudImage image : images) {
                List<CloudImageFlavor> flavors = getAvailableFlavorForCompute(cloud, computeTemplateEntry.getValue(), image);
                boolean templateAdded = false;
                for (CloudImageFlavor flavor : flavors) {
                    ComputeTemplate template = new ComputeTemplate(image.getId(), flavor.getId());
                    if (computeTemplateMapping != null && (!computeTemplateMapping.containsKey(template) || computeTemplateMapping.get(template) == null)) {
                        // Filter this template from matcher configuration because it's not configured
                        continue;
                    }
                    computeTemplates.add(template);
                    templateAdded = true;
                    flavorMap.put(flavor.getId(), flavor);
                }
                if (templateAdded) {
                    imageIds.add(image.getId());
                }
            }
            computeMatchResult.put(computeTemplateEntry.getKey(), computeTemplates);
        }
        Map<String, List<Network>> networkMatchResult = Maps.newHashMap();
        for (Map.Entry<String, NodeTemplate> networkEntry : matchableNodes.networkTemplates.entrySet()) {
            networkMatchResult.put(networkEntry.getKey(), getAvailableNetworks(cloud, networkEntry.getValue()));
        }
        Map<String, CloudImage> imageMap = cloudImageService.getMultiple(imageIds);
        return new CloudResourceTopologyMatchResult(imageMap, flavorMap, computeMatchResult, networkMatchResult);
    }

    private List<Network> getAvailableNetworks(Cloud cloud, NodeTemplate nodeTemplate) {
        Map<String, String> networkProperties = nodeTemplate.getProperties();
        Set<Network> existingNetworks = cloud.getNetworks();
        List<Network> eligibleNetworks = Lists.newArrayList();
        for (Network network : existingNetworks) {
            if (!match(networkProperties, NormativeNetworkConstants.CIDR, network.getCidr(), new TextValueParser(), new EqualMatcher<String>())) {
                continue;
            }
            if (!match(networkProperties, NormativeNetworkConstants.IP_VERSION, network.getIpVersion(), new IntegerValueParser(), new EqualMatcher<Integer>())) {
                continue;
            }
            if (!match(networkProperties, NormativeNetworkConstants.GATEWAY_IP, network.getGatewayIp(), new TextValueParser(), new EqualMatcher<String>())) {
                continue;
            }
            if (!match(networkProperties, NormativeNetworkConstants.NETWORK_NAME, network.getNetworkName(), new TextValueParser(), new EqualMatcher<String>())) {
                continue;
            }
            eligibleNetworks.add(network);
        }
        return eligibleNetworks;
    }

    private static class MatchableTemplates {

        private Map<String, NodeTemplate> computeTemplates = Maps.newHashMap();

        private Map<String, NodeTemplate> networkTemplates = Maps.newHashMap();
    }

    /**
     * This method browse topology's node templates and return those that need to be matched to cloud's resources
     *
     * @param topology the topology to check
     * @return all node template that must be matched
     */
    private MatchableTemplates getMatchableTemplates(Topology topology, Map<String, IndexedNodeType> types) {
        Map<String, NodeTemplate> allNodeTemplates = topology.getNodeTemplates();
        MatchableTemplates matchableNodeTemplates = new MatchableTemplates();
        if (allNodeTemplates == null) {
            return matchableNodeTemplates;
        }
        for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : allNodeTemplates.entrySet()) {
            if (ToscaUtils.isFromType(NormativeComputeConstants.COMPUTE_TYPE, types.get(nodeTemplateEntry.getKey()))) {
                matchableNodeTemplates.computeTemplates.put(nodeTemplateEntry.getKey(), nodeTemplateEntry.getValue());
            }
            if (ToscaUtils.isFromType(NormativeNetworkConstants.NETWORK_TYPE, types.get(nodeTemplateEntry.getKey()))) {
                matchableNodeTemplates.networkTemplates.put(nodeTemplateEntry.getKey(), nodeTemplateEntry.getValue());
            }
        }
        return matchableNodeTemplates;
    }

    /**
     * Get all available image for a compute by filtering with its properties on OS information
     *
     * @param cloud the cloud
     * @param nodeTemplate the compute to search for images
     * @return the available images on the cloud
     */
    private List<CloudImage> getAvailableImagesForCompute(Cloud cloud, NodeTemplate nodeTemplate, IndexedNodeType nodeType) {
        Map<String, String> computeTemplateProperties = nodeTemplate.getProperties();
        // Only get active templates
        Set<ActivableComputeTemplate> templates = cloud.getComputeTemplates();
        Set<String> availableImageIds = Sets.newHashSet();
        for (ActivableComputeTemplate template : templates) {
            if (template.isEnabled()) {
                availableImageIds.add(template.getCloudImageId());
            }
        }
        Map<String, CloudImage> availableImages = cloudImageService.getMultiple(availableImageIds);
        List<CloudImage> matchedImages = Lists.newArrayList();
        for (CloudImage cloudImage : availableImages.values()) {
            if (!match(computeTemplateProperties, NormativeComputeConstants.OS_ARCH, cloudImage.getOsArch(), new TextValueParser(), new EqualMatcher<String>())) {
                continue;
            }
            if (!match(computeTemplateProperties, NormativeComputeConstants.OS_DISTRIBUTION, cloudImage.getOsDistribution(), new TextValueParser(),
                    new EqualMatcher<String>())) {
                continue;
            }
            if (!match(computeTemplateProperties, NormativeComputeConstants.OS_TYPE, cloudImage.getOsType(), new TextValueParser(), new EqualMatcher<String>())) {
                continue;
            }
            if (!match(computeTemplateProperties, NormativeComputeConstants.OS_VERSION, VersionUtil.parseVersion(cloudImage.getOsVersion()),
                    new VersionValueParser(), new GreaterOrEqualValueMatcher())) {
                continue;
            }
            matchedImages.add(cloudImage);
        }

        Collections.sort(matchedImages, new Comparator<CloudImage>() {
            @Override
            public int compare(CloudImage left, CloudImage right) {
                return VersionUtil.compare(left.getOsVersion(), right.getOsVersion());
            }
        });

        return matchedImages;
    }

    /**
     * Get all available flavors for the given compute and the given image on the given cloud
     *
     * @param cloud the cloud
     * @param nodeTemplate the compute to search for flavors
     * @param cloudImage the image
     * @return the available flavors for the compute and the image on the given cloud
     */
    private List<CloudImageFlavor> getAvailableFlavorForCompute(Cloud cloud, NodeTemplate nodeTemplate, CloudImage cloudImage) {
        Map<String, CloudImageFlavor> allFlavors = Maps.newHashMap();
        for (CloudImageFlavor flavor : cloud.getFlavors()) {
            allFlavors.put(flavor.getId(), flavor);
        }
        Set<CloudImageFlavor> availableFlavors = Sets.newHashSet();
        Set<ActivableComputeTemplate> allTemplates = cloud.getComputeTemplates();
        for (ActivableComputeTemplate template : allTemplates) {
            if (template.isEnabled() && template.getCloudImageId().equals(cloudImage.getId())) {
                // get flavors that correspond to the given cloud image from all active compute template
                availableFlavors.add(allFlavors.get(template.getCloudImageFlavorId()));
            }
        }
        List<CloudImageFlavor> matchedFlavors = Lists.newArrayList();
        Map<String, String> computeTemplateProperties = nodeTemplate.getProperties();
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
        Collections.sort(matchedFlavors);
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
            if (properties == null) {
                return true;
            }
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
