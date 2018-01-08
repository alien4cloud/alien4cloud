package alien4cloud.orchestrators.locations.services;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
import alien4cloud.tosca.topology.TemplateBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Location Resource Generator Service provides utilities to generate location resources .
 */
@Component
@Slf4j
public class LocationResourceGeneratorService {
    @Inject
    private TemplateBuilder templateBuilder;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageFlavorContext {
        private List<LocationResourceTemplate> templates;
        private String idPropertyName;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComputeContext {
        private List<NodeType> nodeTypes = Lists.newArrayList();
        private String generatedNamePrefix;
        private String imageIdPropertyName;
        private String flavorIdPropertyName;

    }

    /**
     * Generate resources of type compute given a set of images and flavors
     *
     * @param imageContext
     * @param flavorContext
     * @param linuxComputeContext
     * @param windowsComputeContext
     * @param resourceAccessor
     * @return
     */
    public List<LocationResourceTemplate> generateComputeFromImageAndFlavor(ImageFlavorContext imageContext, ImageFlavorContext flavorContext,
            ComputeContext linuxComputeContext, ComputeContext windowsComputeContext, ILocationResourceAccessor resourceAccessor) {
        List<LocationResourceTemplate> images = imageContext.getTemplates();
        List<LocationResourceTemplate> flavors = flavorContext.getTemplates();
        Set<CSARDependency> dependencies = resourceAccessor.getDependencies();

        List<LocationResourceTemplate> generated = Lists.newArrayList();

        for (LocationResourceTemplate image : images) {
            for (LocationResourceTemplate flavor : flavors) {
                String defaultComputeName = generateDefaultName(image, flavor);
                int count = 0;
                ComputeContext computeContext = isWindowsImage(image) && windowsComputeContext != null ? windowsComputeContext : linuxComputeContext;
                for (NodeType indexedNodeType : computeContext.getNodeTypes()) {
                    String name = StringUtils.isNotBlank(computeContext.getGeneratedNamePrefix()) ? computeContext.getGeneratedNamePrefix()
                            : defaultComputeName;
                    if (count > 0) {
                        name = name + "_" + count;
                    }
                    NodeTemplate node = templateBuilder.buildNodeTemplate(dependencies, indexedNodeType);
                    // set the imageId
                    node.getProperties().put(computeContext.getImageIdPropertyName(),
                            image.getTemplate().getProperties().get(imageContext.getIdPropertyName()));
                    // set the flavorId
                    node.getProperties().put(computeContext.getFlavorIdPropertyName(),
                            flavor.getTemplate().getProperties().get(flavorContext.getIdPropertyName()));

                    // copy os and host capabilities properties
                    copyCapabilityBasedOnTheType(image.getTemplate(), node, "os");
                    copyCapabilityBasedOnTheType(flavor.getTemplate(), node, "host");

                    LocationResourceTemplate resource = new LocationResourceTemplate();
                    resource.setService(false);
                    resource.setTemplate(node);
                    resource.setName(name);
                    count++;

                    generated.add(resource);
                }
            }
        }

        return generated;
    }

    /**
     * Find if an image is windows image, defaults to linux if property is not configured.
     * 
     * @param image The image to check.
     * @return True if the image is windows.
     */
    private boolean isWindowsImage(LocationResourceTemplate image) {

        if (image.getTemplate() == null || safe(image.getTemplate().getCapabilities()).get(NormativeComputeConstants.OS_CAPABILITY) == null
                || safe(image.getTemplate().getCapabilities().get(NormativeComputeConstants.OS_CAPABILITY).getProperties())
                        .get(NormativeComputeConstants.OS_TYPE) == null) {
            return false;
        }

        return "windows".equals(((ScalarPropertyValue) image.getTemplate().getCapabilities().get(NormativeComputeConstants.OS_CAPABILITY).getProperties()
                .get(NormativeComputeConstants.OS_TYPE)).getValue().toLowerCase());
    }

    private String generateDefaultName(LocationResourceTemplate image, LocationResourceTemplate flavor) {
        return flavor.getName() + "_" + image.getName();
    }

    /**
     * Copy a capability based on its type from a node to another
     *
     * @param from
     * @param to
     * @param capabilityName
     */
    private void copyCapabilityBasedOnTheType(NodeTemplate from, NodeTemplate to, String capabilityName) {
        if (MapUtils.isEmpty(from.getCapabilities()) || MapUtils.isEmpty(to.getCapabilities())) {
            return;
        }
        Capability capa = to.getCapabilities().get(capabilityName);
        if (capa != null) {
            // copy the first found and exit
            for (Capability capaToCheck : from.getCapabilities().values()) {
                if (Objects.equal(capaToCheck.getType(), capa.getType())) {
                    to.getCapabilities().put(capabilityName, capaToCheck);
                    return;
                }
            }
        }
    }

    public ImageFlavorContext buildContext(String elementType, String idPropertyName, ILocationResourceAccessor resourceAccessor) {
        return new ImageFlavorContext(resourceAccessor.getResources(elementType), idPropertyName);
    }

    public ComputeContext buildComputeContext(String elementType, String namePefix, String imageIdPropertyName, String flavorIdPropertyName,
            ILocationResourceAccessor resourceAccessor) {
        ComputeContext context = new ComputeContext();
        context.setFlavorIdPropertyName(flavorIdPropertyName);
        context.setImageIdPropertyName(imageIdPropertyName);
        context.setGeneratedNamePrefix(namePefix);
        try {
            NodeType nodeType = resourceAccessor.getIndexedToscaElement(elementType);
            context.getNodeTypes().add(nodeType);
        } catch (NotFoundException e) {
            log.warn("No compute found with the element id: " + elementType, e);
        }
        return context;
    }
}
