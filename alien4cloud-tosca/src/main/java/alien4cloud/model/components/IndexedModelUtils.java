package alien4cloud.model.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.common.Tag;
import alien4cloud.utils.CollectionUtils;

/**
 * Utils class for Indexed(DAO Object Types) Model.
 */
public final class IndexedModelUtils {

    public static final String TOSCA_INDEXED_SUMMARY_KEY = "summary";

    // make sure this class is not instantiated
    private IndexedModelUtils() {
    }

    /**
     * This utility method returns an ordered {@link alien4cloud.model.components.IndexedInheritableToscaElement} collection. The parent elements will be before
     * the children elements
     * This utility method returns an ordered {@link IndexedInheritableToscaElement} collection. The parent elements will be before the children elements
     *
     * @param elementsByIdMap map of {@link IndexedInheritableToscaElement} by id
     * @return
     */
    public static <T extends IndexedInheritableToscaElement> List<T> orderByDerivedFromHierarchy(final Map<String, T> elementsByIdMap) {
        if (elementsByIdMap == null) {
            return null;
        }
        List<T> orderedElements = new ArrayList<T>(elementsByIdMap.values());
        final Map<String, Integer> elementsLevelMap = Maps.newHashMap();
        for (IndexedInheritableToscaElement element : orderedElements) {
            IndexedInheritableToscaElement parent = element;
            int levelCount = 0;
            while (true) {
                if (parent.getDerivedFrom() == null || parent.getDerivedFrom().isEmpty()) {
                    break;
                }
                IndexedInheritableToscaElement oldParent = parent;
                parent = elementsByIdMap.get(parent.getDerivedFrom().get(0));
                if (parent == null) {
                    break;
                }
                if (oldParent.equals(parent)) {
                    // this elements is inheriting from it-self --> error
                    // This error must have been normally detected in the validation phase and so here it means that it's a bug in our code of validation
                    throw new IndexingServiceException(parent.getElementId() + " is parent of it-self, bug in csar validation service");
                }
                levelCount++;
            }
            elementsLevelMap.put(element.getElementId(), levelCount);
        }
        Collections.sort(orderedElements, (left, right) -> elementsLevelMap.get(left.getElementId()).compareTo(elementsLevelMap.get(right.getElementId())));
        return orderedElements;
    }

    public static void mergeInheritableIndex(IndexedInheritableToscaElement from, IndexedInheritableToscaElement to) {
        if (from.getDerivedFrom() != null) {
            // use a linked HashSet so we don't add multiple elements more than once.
            LinkedHashSet<String> derivedFromSet = new LinkedHashSet<String>();
            if (to.getDerivedFrom() != null) {
                derivedFromSet.addAll(to.getDerivedFrom());
            }
            if (from.getDerivedFrom() != null) {
                derivedFromSet.addAll(from.getDerivedFrom());
            }
            to.setDerivedFrom(new ArrayList<String>(derivedFromSet));
        }
        if (from.getTags() != null) {
            if (to.getTags() == null) {
                to.setTags(Lists.<Tag> newArrayList());
                to.getTags().addAll(from.getTags());
            } else {
                // copy non existing tags from the parent "from"
                for (Tag tag : from.getTags()) {
                    if (!to.getTags().contains(tag)) {
                        to.getTags().add(tag);
                    }
                }
            }
        }

        mergePropertiesAndAttributes(from, to);

        if (from instanceof IndexedNodeType && to instanceof IndexedNodeType) {
            mergeNodeType((IndexedNodeType) from, (IndexedNodeType) to);
        }

        if (from instanceof IndexedRelationshipType && to instanceof IndexedRelationshipType) {
            mergeRelationshipType((IndexedRelationshipType) from, (IndexedRelationshipType) to);
        }
    }

    private static void mergePropertiesAndAttributes(IndexedInheritableToscaElement from, IndexedInheritableToscaElement to) {
        if (from.getProperties() != null) {
            to.setProperties(CollectionUtils.merge(from.getProperties(), to.getProperties(), false));
        }
    }

    private static void mergeNodeType(IndexedNodeType from, IndexedNodeType to) {
        to.setCapabilities(CollectionUtils.merge(from.getCapabilities(), to.getCapabilities()));
        to.setRequirements(CollectionUtils.merge(from.getRequirements(), to.getRequirements()));
        to.setInterfaces(mergeInterfaces(from.getInterfaces(), to.getInterfaces()));
        to.setArtifacts(CollectionUtils.merge(from.getArtifacts(), to.getArtifacts(), false));
        if (from.getAttributes() != null) {
            to.setAttributes(CollectionUtils.merge(from.getAttributes(), to.getAttributes(), false));
        }
    }

    private static void mergeRelationshipType(IndexedRelationshipType from, IndexedRelationshipType to) {
        if (to.getValidSources() == null) {
            to.setValidSources(from.getValidSources());
        }

        if (to.getValidTargets() == null) {
            to.setValidTargets(from.getValidTargets());
        }

        if (from.getAttributes() != null) {
            to.setAttributes(CollectionUtils.merge(from.getAttributes(), to.getAttributes(), false));
        }

        to.setInterfaces(mergeInterfaces(from.getInterfaces(), to.getInterfaces()));
        to.setArtifacts(CollectionUtils.merge(from.getArtifacts(), to.getArtifacts(), false));
    }

    /**
     * Merge interface & operations: all 'from' interfaces will be merged into 'to' interfaces.
     * <p>
     * 'from' entries are added to 'to' entries if not exist (so entries in 'to' are preserved).
     */
    public static Map<String, Interface> mergeInterfaces(Map<String, Interface> from, Map<String, Interface> to) {
        Map<String, Interface> target = to;
        if (target == null) {
            return from;
        }
        if (from == null) {
            return target;
        }
        for (Entry<String, Interface> fromEntry : from.entrySet()) {
            Interface toInterface = target.get(fromEntry.getKey());
            Interface fromInterface = fromEntry.getValue();
            if (toInterface == null) {
                // the target doesn't contain this key, just put it
                target.put(fromEntry.getKey(), fromEntry.getValue());
            } else {
                // the target already have this entry, so we'll compare operations in detail
                Map<String, Operation> toOperations = toInterface.getOperations();
                if (toOperations == null) {
                    toInterface.setOperations(fromInterface.getOperations());
                } else if (fromInterface.getOperations() != null) {
                    for (Entry<String, Operation> fromOperationEntry : fromInterface.getOperations().entrySet()) {
                        if (!toOperations.containsKey(fromOperationEntry.getKey())) {
                            toOperations.put(fromOperationEntry.getKey(), fromOperationEntry.getValue());
                        }
                    }
                }
            }
        }
        return target;
    }

    public static CapabilityDefinition getCapabilityDefinitionById(List<CapabilityDefinition> list, String id) {
        if (list != null) {
            for (CapabilityDefinition cd : list) {
                if (cd.getId().equals(id)) {
                    return cd;
                }
            }
        }
        return null;
    }

    public static RequirementDefinition getRequirementDefinitionById(List<RequirementDefinition> list, String id) {
        for (RequirementDefinition cd : list) {
            if (cd.getId().equals(id)) {
                return cd;
            }
        }
        return null;
    }

}