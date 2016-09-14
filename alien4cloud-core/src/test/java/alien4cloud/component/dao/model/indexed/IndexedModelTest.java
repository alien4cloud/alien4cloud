package alien4cloud.component.dao.model.indexed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.collect.Maps;
import org.junit.Test;

import alien4cloud.model.common.Tag;
import org.alien4cloud.tosca.model.definitions.AttributeDefinition;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import alien4cloud.model.components.IndexedModelUtils;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;

public class IndexedModelTest {

    // root -- child1 -- grandChild1, granChild2
    // |
    // |
    // |------ child2
    // |
    // orphan
    // The sort should give me [root,orphan],[child1,child2],[grandChild1,grandChild2]
    static Map<String, AbstractInheritableToscaType> elementsByIdMap = Maps.newHashMap();

    static AbstractInheritableToscaType root, child1, child2, grandChild1, grandChild2;
    static List<Tag> rootTags, childTags;

    static NodeType parent, sonWith3Tags, sonWithoutTags;

    static {

        rootTags = Lists.newArrayList();
        rootTags.add(new Tag("alienicon", "/usr/local/root-icon.png"));
        rootTags.add(new Tag("prod", "deployment details from team A the 21th JAN 2014"));
        rootTags.add(new Tag("misc-details", "<pathed the 22th...>"));

        childTags = Lists.newArrayList();
        childTags.add(new Tag("alienicon", "/usr/child-icon.png"));
        childTags.add(new Tag("prodChild", "Bad deployment"));
        childTags.add(new Tag("another-tag", "<pathed the 22th...>"));

        parent = new NodeType();
        parent.setElementId("1");
        parent.setArchiveName("tosca.nodes.Root");
        parent.setArchiveVersion("1.0");
        parent.setDerivedFrom(null);
        parent.setDescription("Root description...");
        parent.setTags(rootTags);

        sonWith3Tags = new NodeType();
        sonWith3Tags.setElementId("2");
        sonWith3Tags.setArchiveName("tosca.nodes.Compute");
        sonWith3Tags.setArchiveVersion("1.0");
        sonWith3Tags.setDerivedFrom(Arrays.asList("tosca.nodes.Root"));
        sonWith3Tags.setDescription("Child description...");
        sonWith3Tags.setTags(childTags);

        sonWithoutTags = new NodeType();
        sonWithoutTags.setElementId("3");
        sonWithoutTags.setArchiveName("tosca.nodes.Network");
        sonWithoutTags.setArchiveVersion("1.2");
        sonWithoutTags.setDerivedFrom(Arrays.asList("tosca.nodes.Root"));
        sonWithoutTags.setDescription("Child 2 description...");
        sonWithoutTags.setTags(null);

        root = fabricElement(elementsByIdMap, "root", null, rootTags);
        child1 = fabricElement(elementsByIdMap, "child1", "root", childTags);
        child2 = fabricElement(elementsByIdMap, "child2", "root", null);
        grandChild1 = fabricElement(elementsByIdMap, "grandChild1", "child1", null);
        grandChild2 = fabricElement(elementsByIdMap, "grandChild2", "child1", null);

        fabricElement(elementsByIdMap, "orphan1", null, null);
        fabricElement(elementsByIdMap, "orphan2", "toto", null);
        fabricElement(elementsByIdMap, "orphan3", "tata", null);
        fabricElement(elementsByIdMap, "orphan4", "titi", null);

    }

    @Test
    public void testOrderInheritableElements() {
        List<AbstractInheritableToscaType> sorted = IndexedModelUtils.orderByDerivedFromHierarchy(elementsByIdMap);

        for (AbstractInheritableToscaType el : sorted) {
            System.out.println(el.getElementId() + " " + el.getDerivedFrom());
        }

        assertTrue(sorted.indexOf(root) < sorted.indexOf(child1));
        assertTrue(sorted.indexOf(root) < sorted.indexOf(child2));
        assertTrue(sorted.indexOf(child1) < sorted.indexOf(grandChild1));
        assertTrue(sorted.indexOf(child1) < sorted.indexOf(grandChild2));
    }

    @Test
    public void mergeInheritableIndexWithTags() {
        IndexedModelUtils.mergeInheritableIndex(parent, sonWith3Tags);

        // son should have 5 tags : 3 from himself 2 from his parent
        assertEquals(sonWith3Tags.getTags().size(), 5);

        // son should'nt have parent icon
        ;
        for (Tag tag : sonWith3Tags.getTags()) {
            if (tag.getName().equals("alienicon")) {
                assertNotEquals(tag.getValue(), "/usr/local/root-icon.png");
                assertEquals(tag.getValue(), "/usr/child-icon.png");
            }
        }

        // son has all parent's tags
        assertTrue(sonWith3Tags.getTags().containsAll(parent.getTags()));

        // Parent witht 3 tags merged with son without tags => son with 3 tags
        IndexedModelUtils.mergeInheritableIndex(parent, sonWithoutTags);

        assertEquals(sonWithoutTags.getTags().size(), parent.getTags().size());
        assertEquals(sonWithoutTags.getTags(), parent.getTags());
    }

    @Test
    public void mergeInheritableIndexMaps() {
        NodeType son = new NodeType();
        son.setElementId("son");
        son.setArchiveVersion("1");

        PropertyDefinition propDef = new PropertyDefinition();
        AttributeDefinition attrDef = new AttributeDefinition();

        propDef.setType("string");
        propDef.setDefault(new ScalarPropertyValue("default_parent"));
        attrDef.setType("string");
        parent.setProperties(MapUtil.newHashMap(new String[] { "prop1" }, new PropertyDefinition[] { propDef }));
        parent.setAttributes(Maps.<String, IValue> newHashMap());
        parent.getAttributes().put("attr1", attrDef);

        propDef = new PropertyDefinition();
        propDef.setDefault(new ScalarPropertyValue("default_parent2"));
        propDef.setType("string");
        attrDef = new AttributeDefinition();
        attrDef.setType("version");
        parent.getProperties().put("prop2", propDef);
        parent.setAttributes(Maps.<String, IValue> newHashMap());
        parent.getAttributes().put("attr2", attrDef);

        propDef = new PropertyDefinition();
        propDef.setDefault(new ScalarPropertyValue("default_son"));
        propDef.setType("string");
        attrDef = new AttributeDefinition();
        attrDef.setType("integer");
        son.setProperties(MapUtil.newHashMap(new String[] { "prop1" }, new PropertyDefinition[] { propDef }));
        // son.setAttributes(MapUtil.newHashMap(new String[] { "attr1" }, new AttributeDefinition[] { attrDef }));
        son.setAttributes(Maps.<String, IValue> newHashMap());
        son.getAttributes().put("attr1", attrDef);

        propDef = new PropertyDefinition();
        propDef.setDefault(new ScalarPropertyValue("default_son2"));
        propDef.setType("integer");
        attrDef = new AttributeDefinition();
        attrDef.setType("boolean");
        son.getProperties().put("prop3", propDef);
        son.getAttributes().put("attr3", attrDef);

        IndexedModelUtils.mergeInheritableIndex(parent, son);

        // son should have 3 : 1 from himself, 1 from his parent, and one that he overrides from the parent
        assertEquals(3, son.getProperties().size());
        assertEquals(3, son.getAttributes().size());

        // son should'nt have parent's one when both defined the same
        PropertyDefinition propDefSon = son.getProperties().get("prop1");
        assertNotNull(propDefSon);
        assertEquals(new ScalarPropertyValue("default_son"), propDefSon.getDefault());
        AttributeDefinition attrDefSon = (AttributeDefinition) son.getAttributes().get("attr1");
        assertEquals("integer", attrDefSon.getType());

        // son has all parent's
        for (String key : parent.getProperties().keySet()) {
            assertTrue(son.getProperties().containsKey(key));
        }
        for (String key : parent.getAttributes().keySet()) {
            assertTrue(son.getAttributes().containsKey(key));
        }

    }

    private static AbstractInheritableToscaType fabricElement(Map<String, AbstractInheritableToscaType> map, String elementId, String derivedFrom,
                                                              List<Tag> tags) {
        AbstractInheritableToscaType element = new AbstractInheritableToscaType();
        element.setElementId(elementId);
        element.setArchiveVersion("1.0");
        element.setDerivedFrom(Arrays.asList(derivedFrom));
        map.put(element.getElementId(), element);
        return element;
    }

    @Test
    public void testMergeInterfacesNull() {
        assertNull(IndexedModelUtils.mergeInterfaces(null, null));
        Map<String, Interface> from = Maps.newHashMap();
        Map<String, Interface> merged = IndexedModelUtils.mergeInterfaces(from, null);
        assertNotNull(merged);
        assertSame(from, merged);
        Map<String, Interface> to = from;
        merged = IndexedModelUtils.mergeInterfaces(null, to);
        assertNotNull(merged);
        assertSame(to, merged);
    }

    @Test
    public void testMergeInterfacesBasic() {
        Map<String, Interface> from = Maps.newHashMap();
        Interface i1 = new Interface();
        from.put("i1", i1);
        Map<String, Interface> to = Maps.newHashMap();
        Interface i2 = new Interface();
        to.put("i2", i2);
        Map<String, Interface> merged = IndexedModelUtils.mergeInterfaces(from, to);
        assertEquals(2, merged.size());
        assertSame(merged.get("i1"), i1);
        assertSame(merged.get("i2"), i2);
    }

    @Test
    public void testMergeInterfaceOperationsKeepTo() {
        // TO keeps it's own operation
        Map<String, Interface> from = Maps.newHashMap();
        Interface i1 = new Interface();
        Map<String, Operation> ios1 = Maps.newHashMap();
        Operation o1 = new Operation();
        ios1.put("o1", o1);
        i1.setOperations(ios1);
        from.put("i1", i1);
        Map<String, Interface> to = Maps.newHashMap();
        Interface i2 = new Interface();
        Map<String, Operation> ios2 = Maps.newHashMap();
        Operation o2 = new Operation();
        ios2.put("o1", o2);
        i2.setOperations(ios2);
        to.put("i1", i2);
        Map<String, Interface> merged = IndexedModelUtils.mergeInterfaces(from, to);
        assertEquals(1, merged.size());
        assertSame(merged.get("i1").getOperations().get("o1"), o2);
    }

    @Test
    public void testMergeInterfaceOperationsToNull() {
        // TO's operation is null
        Map<String, Interface> from = Maps.newHashMap();
        Interface i1 = new Interface();
        Map<String, Operation> ios1 = Maps.newHashMap();
        Operation o1 = new Operation();
        ios1.put("o1", o1);
        i1.setOperations(ios1);
        from.put("i1", i1);
        Map<String, Interface> to = Maps.newHashMap();
        Interface i2 = new Interface();
        to.put("i1", i2);
        Map<String, Interface> merged = IndexedModelUtils.mergeInterfaces(from, to);
        assertEquals(1, merged.size());
        assertSame(merged.get("i1").getOperations().get("o1"), o1);
    }

    @Test
    public void testMergeInterfaceOperationsFromNull() {
        // FROM's operation is null
        Map<String, Interface> from = Maps.newHashMap();
        Interface i1 = new Interface();
        from.put("i1", i1);
        Map<String, Interface> to = Maps.newHashMap();
        Interface i2 = new Interface();
        Map<String, Operation> ios2 = Maps.newHashMap();
        Operation o2 = new Operation();
        ios2.put("o1", o2);
        i2.setOperations(ios2);
        to.put("i1", i2);
        Map<String, Interface> merged = IndexedModelUtils.mergeInterfaces(from, to);
        assertEquals(1, merged.size());
        assertSame(merged.get("i1").getOperations().get("o1"), o2);
    }

    @Test
    public void testMergeInterfaceOperationsMerge() {
        // both are merged
        Map<String, Interface> from = Maps.newHashMap();
        Interface i1 = new Interface();
        Map<String, Operation> ios1 = Maps.newHashMap();
        Operation o1 = new Operation();
        ios1.put("o1", o1);
        i1.setOperations(ios1);
        from.put("i1", i1);
        Map<String, Interface> to = Maps.newHashMap();
        Interface i2 = new Interface();
        Map<String, Operation> ios2 = Maps.newHashMap();
        Operation o2 = new Operation();
        ios2.put("o2", o2);
        i2.setOperations(ios2);
        to.put("i1", i2);
        Map<String, Interface> merged = IndexedModelUtils.mergeInterfaces(from, to);
        assertEquals(1, merged.size());
        assertEquals(2, merged.get("i1").getOperations().size());
        assertSame(merged.get("i1").getOperations().get("o1"), o1);
        assertSame(merged.get("i1").getOperations().get("o2"), o2);
    }

}