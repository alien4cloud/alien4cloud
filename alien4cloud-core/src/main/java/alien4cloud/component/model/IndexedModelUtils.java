package alien4cloud.component.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.elasticsearch.common.collect.Maps;

import alien4cloud.component.repository.CsarFileRepository;
import alien4cloud.exception.IndexingNotSupportedException;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.tosca.container.model.ToscaInheritableElement;
import alien4cloud.tosca.container.model.type.ArtifactType;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.container.model.type.CapabilityType;
import alien4cloud.tosca.container.model.type.NodeType;
import alien4cloud.tosca.container.model.type.RelationshipType;
import alien4cloud.tosca.container.model.type.RequirementDefinition;
import alien4cloud.utils.CollectionUtils;

import com.google.common.collect.Lists;

/**
 * Utils class for Indexed(DAO Object Types) Model.
 * 
 * @author 'Igor Ngouagna'
 */
public final class IndexedModelUtils {

    public static final String TOSCA_INDEXED_SUMMARY_KEY = "summary";

    private static final Map<Class<? extends ToscaInheritableElement>, Class<? extends IndexedInheritableToscaElement>> TOSCA_INHERITABLE_TO_INDEX_MODEL_MAPPING;

    private static final Map<Class<? extends ToscaElement>, Class<? extends IndexedToscaElement>> TOSCA_NON_INHERITABLE_TO_INDEX_MODEL_MAPPING;

    // make sure this class is not instantiated
    private IndexedModelUtils() {
    }

    static {
        TOSCA_INHERITABLE_TO_INDEX_MODEL_MAPPING = new HashMap<Class<? extends ToscaInheritableElement>, Class<? extends IndexedInheritableToscaElement>>();
        TOSCA_INHERITABLE_TO_INDEX_MODEL_MAPPING.put(NodeType.class, IndexedNodeType.class);
        TOSCA_INHERITABLE_TO_INDEX_MODEL_MAPPING.put(CapabilityType.class, IndexedCapabilityType.class);
        TOSCA_INHERITABLE_TO_INDEX_MODEL_MAPPING.put(RelationshipType.class, IndexedRelationshipType.class);
        TOSCA_INHERITABLE_TO_INDEX_MODEL_MAPPING.put(ArtifactType.class, IndexedArtifactType.class);
        TOSCA_NON_INHERITABLE_TO_INDEX_MODEL_MAPPING = new HashMap<Class<? extends ToscaElement>, Class<? extends IndexedToscaElement>>();
    }

    /**
     * Get all inheritable classes that are indexed
     * 
     * @return all classes that are indexed
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends IndexedInheritableToscaElement>[] getInheritableIndexClasses() {
        return TOSCA_INHERITABLE_TO_INDEX_MODEL_MAPPING.values().toArray(new Class[TOSCA_INHERITABLE_TO_INDEX_MODEL_MAPPING.size()]);
    }

    /**
     * Get all non inheritable classes that are indexed
     * 
     * @return all classes that are indexed
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends IndexedToscaElement>[] getNonInheritableIndexClasses() {
        return TOSCA_NON_INHERITABLE_TO_INDEX_MODEL_MAPPING.values().toArray(new Class[TOSCA_NON_INHERITABLE_TO_INDEX_MODEL_MAPPING.size()]);
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends IndexedToscaElement>[] getAllIndexClasses() {
        Class<? extends IndexedToscaElement>[] inheritables = getInheritableIndexClasses();
        Class<? extends IndexedToscaElement>[] nonInheritables = getNonInheritableIndexClasses();
        Class<? extends IndexedToscaElement>[] all = new Class[inheritables.length + nonInheritables.length];
        System.arraycopy(inheritables, 0, all, 0, inheritables.length);
        System.arraycopy(nonInheritables, 0, all, inheritables.length, nonInheritables.length);
        return all;
    }

    public static Class<? extends IndexedToscaElement> getIndexClass(Class<? extends ToscaElement> toscaClass) {
        Class<? extends IndexedToscaElement> indexedClass = TOSCA_INHERITABLE_TO_INDEX_MODEL_MAPPING.get(toscaClass);
        if (indexedClass == null) {
            indexedClass = TOSCA_NON_INHERITABLE_TO_INDEX_MODEL_MAPPING.get(toscaClass);
        }
        return indexedClass;
    }

    /**
     * Map from tosca model to elastic search model
     * 
     * @param toscaClass tosca model class to be mapped to index model
     * @return the mapped index model class
     */
    public static Class<? extends IndexedInheritableToscaElement> getInheritableIndexClass(Class<? extends ToscaInheritableElement> toscaClass) {
        return TOSCA_INHERITABLE_TO_INDEX_MODEL_MAPPING.get(toscaClass);
    }

    /**
     * Map from tosca model to elastic search model
     * 
     * @param toscaClass tosca model class to be mapped to index model
     * @return the mapped index model class
     */
    public static Class<? extends IndexedToscaElement> getNonInheritableIndexClass(Class<? extends ToscaElement> toscaClass) {
        return TOSCA_NON_INHERITABLE_TO_INDEX_MODEL_MAPPING.get(toscaClass);
    }

    /**
     * Get corresponded index model from the tosca meta-model
     * 
     * @param tobeIndexed tosca inheritable model
     * @param archiveName the name of the archive
     * @param archiveVersion the version of the archive
     * @return the indexed instance filled up with tosca element informations
     */
    public static IndexedInheritableToscaElement getInheritableIndexedModel(ToscaInheritableElement tobeIndexed, String archiveName, String archiveVersion,
            Date creationDate) {
        Class<? extends IndexedInheritableToscaElement> indexClass = getInheritableIndexClass(tobeIndexed.getClass());
        // NodeType
        if (IndexedNodeType.class.equals(indexClass)) {
            return getIndexedNodeType((NodeType) tobeIndexed, archiveName, archiveVersion, creationDate);
        } else if (IndexedRelationshipType.class.equals(indexClass)) {
            return getIndexedRelationshipType((RelationshipType) tobeIndexed, archiveName, archiveVersion);
        } else if (indexClass != null) {
            try {
                IndexedInheritableToscaElement element = indexClass.newInstance();
                fillIndexedInheritableToscaElementProperties(tobeIndexed, element, archiveName, archiveVersion, creationDate);
                return element;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new IndexingNotSupportedException("Could not instantiate indexed class [" + indexClass.getName() + "]", e);
            }
        } else {
            throw new IndexingNotSupportedException(tobeIndexed.getClass());
        }
    }

    /**
     * Get corresponded index model from the tosca meta-model
     * 
     * @param tobeIndexed tosca inheritable model
     * @param archiveName the name of the archive
     * @param archiveVersion the version of the archive
     * @return the indexed instance filled up with tosca element informations
     * @throws IndexingNotSupportedException
     */
    public static IndexedToscaElement getNonInheritableIndexedModel(ToscaElement tobeIndexed, String archiveName, String archiveVersion) {
        Class<? extends IndexedToscaElement> indexClass = getNonInheritableIndexClass(tobeIndexed.getClass());
        if (indexClass != null) {
            try {
                IndexedToscaElement element = indexClass.newInstance();
                fillIndexedToscaElementProperties(tobeIndexed, element, archiveName, archiveVersion, null);
                return element;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new IndexingNotSupportedException("Could not instantiate indexed class [" + indexClass.getName() + "]", e);
            }
        } else {
            throw new IndexingNotSupportedException(tobeIndexed.getClass());
        }
    }

    /**
     * This utility method returns an ordered {@link ToscaInheritableElement} collection. The parent elements will be before the children elements
     * 
     * @param elementsByIdMap map of {@link ToscaInheritableElement} by id
     * @return
     */
    public static List<ToscaInheritableElement> orderForIndex(final Map<String, ToscaInheritableElement> elementsByIdMap) {
        List<ToscaInheritableElement> orderedElements = new ArrayList<ToscaInheritableElement>(elementsByIdMap.values());
        final Map<String, Integer> elementsLevelMap = Maps.newHashMap();
        for (ToscaInheritableElement element : orderedElements) {
            ToscaInheritableElement parent = element;
            int levelCount = 0;
            while (true) {
                if (parent.getDerivedFrom() == null) {
                    break;
                }
                ToscaInheritableElement oldParent = parent;
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
        Collections.sort(orderedElements, new Comparator<ToscaInheritableElement>() {

            @Override
            public int compare(ToscaInheritableElement left, ToscaInheritableElement right) {
                return elementsLevelMap.get(left.getId()).compareTo(elementsLevelMap.get(right.getId()));
            }
        });
        return orderedElements;
    }

    private static void fillIndexedToscaElementProperties(ToscaElement tobeIndexed, IndexedToscaElement indexedToscaElement, String archiveName,
            String archiveVersion, Date creationDate) {

        // override if the version contains the snapshot identifier key
        boolean isSnapshot = archiveVersion.toUpperCase().contains(CsarFileRepository.SNAPSHOT_IDENTIFIER);

        indexedToscaElement.setElementId(tobeIndexed.getId());
        indexedToscaElement.setArchiveName(archiveName);
        indexedToscaElement.setArchiveVersion(archiveVersion);
        indexedToscaElement.setDescription(tobeIndexed.getDescription());
        final Date currentDate = new Date();
        if (isSnapshot == true && creationDate != null) {
            indexedToscaElement.setCreationDate(creationDate);
        } else {
            indexedToscaElement.setCreationDate(currentDate);
        }
        indexedToscaElement.setLastUpdateDate(currentDate);
        if (tobeIndexed.getTags() != null && !tobeIndexed.getTags().isEmpty()) {
            indexedToscaElement.setTags(Lists.<Tag> newArrayList());
            for (Entry<String, String> entry : tobeIndexed.getTags().entrySet()) {
                indexedToscaElement.getTags().add(new Tag(entry.getKey(), entry.getValue()));
            }
        }
    }

    private static void fillIndexedInheritableToscaElementProperties(ToscaInheritableElement tobeIndexed,
            IndexedInheritableToscaElement indexedInheritableToscaElement, String archiveName, String archiveVersion, Date creationDate) {
        fillIndexedToscaElementProperties(tobeIndexed, indexedInheritableToscaElement, archiveName, archiveVersion, creationDate);
        if (tobeIndexed.getDerivedFrom() != null) {
            indexedInheritableToscaElement.setDerivedFrom(new HashSet<String>());
            indexedInheritableToscaElement.getDerivedFrom().add(tobeIndexed.getDerivedFrom());
        }
        if (tobeIndexed.getProperties() != null) {
            indexedInheritableToscaElement.setProperties(new HashMap<>(tobeIndexed.getProperties()));
        }
        if (tobeIndexed.getAttributes() != null) {
            indexedInheritableToscaElement.setAttributes(new HashMap<>(tobeIndexed.getAttributes()));
        }
        indexedInheritableToscaElement.setAbstract(tobeIndexed.isAbstract());
    }

    private static IndexedNodeType getIndexedNodeType(NodeType ofThis, String archiveName, String archiveVersion, Date creationDate) {
        IndexedNodeType result = new IndexedNodeType();
        // Fill base properties
        fillIndexedInheritableToscaElementProperties(ofThis, result, archiveName, archiveVersion, creationDate);
        Set<CapabilityDefinition> capa = new HashSet<>();
        Set<RequirementDefinition> req = new HashSet<>();
        if (ofThis.getCapabilities() != null) {
            for (Map.Entry<String, CapabilityDefinition> entry : ofThis.getCapabilities().entrySet()) {
                capa.add(entry.getValue());
            }
        }
        if (ofThis.getRequirements() != null) {
            for (Map.Entry<String, RequirementDefinition> entry : ofThis.getRequirements().entrySet()) {
                req.add(entry.getValue());
            }
        }
        if (ofThis.getInterfaces() != null) {
            result.setInterfaces(new HashMap<>(ofThis.getInterfaces()));
        }
        if (ofThis.getArtifacts() != null) {
            result.setArtifacts(Maps.newHashMap(ofThis.getArtifacts()));
        }
        result.setCapabilities(capa);
        result.setRequirements(req);
        return result;
    }

    private static IndexedRelationshipType getIndexedRelationshipType(RelationshipType ofThis, String archiveName, String archiveVersion) {
        IndexedRelationshipType result = new IndexedRelationshipType();
        // Fill base properties
        fillIndexedInheritableToscaElementProperties(ofThis, result, archiveName, archiveVersion, null);
        result.setValidTargets(ofThis.getValidTargets());
        if (ofThis.getInterfaces() != null) {
            result.setInterfaces(new HashMap<>(ofThis.getInterfaces()));
        }
        if (ofThis.getArtifacts() != null) {
            result.setArtifacts(Maps.newHashMap(ofThis.getArtifacts()));
        }
        return result;
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
        if (from.getAttributes() != null) {
            to.setAttributes(CollectionUtils.merge(from.getAttributes(), to.getAttributes(), false));
        }
    }

    private static void mergeNodeType(IndexedNodeType from, IndexedNodeType to) {
        to.setCapabilities(CollectionUtils.merge(from.getCapabilities(), to.getCapabilities()));
        to.setRequirements(CollectionUtils.merge(from.getRequirements(), to.getRequirements()));
        to.setInterfaces(CollectionUtils.merge(from.getInterfaces(), to.getInterfaces(), false));
        to.setArtifacts(CollectionUtils.merge(from.getArtifacts(), to.getArtifacts(), false));
    }

    private static void mergeRelationshipType(IndexedRelationshipType from, IndexedRelationshipType to) {
        if (to.getValidSources() == null) {
            to.setValidSources(from.getValidSources());
        }

        if (to.getValidTargets() == null) {
            to.setValidTargets(from.getValidTargets());
        }

        to.setInterfaces(CollectionUtils.merge(from.getInterfaces(), to.getInterfaces(), false));
        to.setArtifacts(CollectionUtils.merge(from.getArtifacts(), to.getArtifacts(), false));
    }
}