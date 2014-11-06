package alien4cloud.component.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.collect.Maps;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.utils.CollectionUtils;

import com.google.common.collect.Lists;

/**
 * Utils class for Indexed(DAO Object Types) Model.
 */
public final class IndexedModelUtils {

    public static final String TOSCA_INDEXED_SUMMARY_KEY = "summary";

    // make sure this class is not instantiated
    private IndexedModelUtils() {
    }

    /**
     * This utility method returns an ordered {@link ToscaInheritableElement} collection. The parent elements will be before the children elements
     * 
     * @param elementsByIdMap map of {@link ToscaInheritableElement} by id
     * @return
     */
    public static List<IndexedInheritableToscaElement> orderForIndex(final Map<String, IndexedInheritableToscaElement> elementsByIdMap) {
        List<IndexedInheritableToscaElement> orderedElements = new ArrayList<IndexedInheritableToscaElement>(elementsByIdMap.values());
        final Map<String, Integer> elementsLevelMap = Maps.newHashMap();
        for (IndexedInheritableToscaElement element : orderedElements) {
            IndexedInheritableToscaElement parent = element;
            int levelCount = 0;
            while (true) {
                if (parent.getDerivedFrom() == null) {
                    break;
                }
                IndexedInheritableToscaElement oldParent = parent;
                parent = elementsByIdMap.get(parent.getDerivedFrom());
                if (parent == null) {
                    break;
                }
                if (oldParent.equals(parent)) {
                    // this elements is inheriting from it-self --> error
                    // This error must have been normally detected in the validation phase and so here it means that it's a bug in our code of validation
                    throw new IndexingServiceException(parent.getId() + " is parent of it-self, bug in csar validation service");
                }
                levelCount++;
            }
            elementsLevelMap.put(element.getId(), levelCount);
        }
        Collections.sort(orderedElements, new Comparator<IndexedInheritableToscaElement>() {
            @Override
            public int compare(IndexedInheritableToscaElement left, IndexedInheritableToscaElement right) {
                return elementsLevelMap.get(left.getId()).compareTo(elementsLevelMap.get(right.getId()));
            }
        });
        return orderedElements;
    }

    public static void mergeInheritableIndex(IndexedInheritableToscaElement from, IndexedInheritableToscaElement to) {
        if (from.getDerivedFrom() != null) {
            if (to.getDerivedFrom() == null) {
                to.setDerivedFrom(new HashSet<String>());
            }
            to.getDerivedFrom().addAll(from.getDerivedFrom());
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

        mergeProtertiesAndAttributes(from, to);

        if (from instanceof IndexedNodeType && to instanceof IndexedNodeType) {
            mergeNodeType((IndexedNodeType) from, (IndexedNodeType) to);
        }

        if (from instanceof IndexedRelationshipType && to instanceof IndexedRelationshipType) {
            mergeRelationshipType((IndexedRelationshipType) from, (IndexedRelationshipType) to);
        }
    }

    private static void mergeProtertiesAndAttributes(IndexedInheritableToscaElement from, IndexedInheritableToscaElement to) {
        if (from.getProperties() != null) {
            to.setProperties(CollectionUtils.merge(from.getProperties(), to.getProperties(), false));
        }
    }

    private static void mergeNodeType(IndexedNodeType from, IndexedNodeType to) {
        to.setCapabilities(CollectionUtils.merge(from.getCapabilities(), to.getCapabilities()));
        to.setRequirements(CollectionUtils.merge(from.getRequirements(), to.getRequirements()));
        to.setInterfaces(CollectionUtils.merge(from.getInterfaces(), to.getInterfaces(), false));
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

        to.setInterfaces(CollectionUtils.merge(from.getInterfaces(), to.getInterfaces(), false));
        to.setArtifacts(CollectionUtils.merge(from.getArtifacts(), to.getArtifacts(), false));
    }
}