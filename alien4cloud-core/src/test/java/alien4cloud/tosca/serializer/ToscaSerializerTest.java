package alien4cloud.tosca.serializer;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.InRangeConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MaxLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MinLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.PatternConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.ValidValuesConstraint;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * This is more a sandbox to play with the TOSCA serializer rather than a Unit Test.
 */
public class ToscaSerializerTest {

    @Ignore
    @Test
    public void simpleTest() throws IOException, URISyntaxException {
        Topology topology = new Topology();
        topology.setDependencies(new HashSet<CSARDependency>());
        topology.getDependencies().add(new CSARDependency("name1", "1.0"));
        topology.getDependencies().add(new CSARDependency("name2", "2.0"));
        
        topology.setInputs(new HashMap<String, PropertyDefinition>());
        PropertyDefinition pd1 = new PropertyDefinition();
        pd1.setType("string");
        pd1.setConstraints(getConstraintList());
        pd1.setDescription("A description");
        topology.getInputs().put("input1", pd1);
        
        PropertyDefinition pd2 = new PropertyDefinition();
        pd2.setType("integer");
        pd2.setRequired(false);
        pd2.setDefault(new ScalarPropertyValue("10"));
        topology.getInputs().put("input2", pd2);

        topology.setNodeTemplates(new HashMap<String, NodeTemplate>());
        topology.getNodeTemplates().put("node1", new NodeTemplate());
        topology.getNodeTemplates().get("node1").setType("the.node.Type");
        topology.getNodeTemplates().get("node1").setProperties(buildSamplePropertyValueMap());

        topology.getNodeTemplates().get("node1").setRelationships(new HashMap<String, RelationshipTemplate>());
        topology.getNodeTemplates().get("node1").getRelationships().put("hostedOn", new RelationshipTemplate());
        topology.getNodeTemplates().get("node1").getRelationships().get("hostedOn").setTarget("compute2");
        topology.getNodeTemplates().get("node1").getRelationships().get("hostedOn").setRequirementType("capabilities.Capa");
        topology.getNodeTemplates().get("node1").getRelationships().get("hostedOn").setRequirementName("host");
        topology.getNodeTemplates().get("node1").getRelationships().get("hostedOn").setType("relationship.Rel");
        topology.getNodeTemplates().get("node1").getRelationships().get("hostedOn").setProperties(buildSamplePropertyValueMap());

        topology.getNodeTemplates().get("node1").setCapabilities(new HashMap<String, Capability>());
        Capability capability = new Capability();
        capability.setProperties(buildSamplePropertyValueMap());
        topology.getNodeTemplates().get("node1").getCapabilities().put("capa1", capability);
        // this capability should not appear
        topology.getNodeTemplates().get("node1").getCapabilities().put("capa2", new Capability());

        topology.getNodeTemplates().get("node1").setArtifacts(new HashMap<String, DeploymentArtifact>());
        DeploymentArtifact da = new DeploymentArtifact();
        da.setArtifactName("artifact.war");
        da.setArtifactRef("010203904872876723");
        da.setArtifactType("artifacttypes.Artifact");
        topology.getNodeTemplates().get("node1").getArtifacts().put("artifact1", da);

        topology.setOutputProperties(new HashMap<String, Set<String>>());
        topology.getOutputProperties().put("node1", Sets.newHashSet("prop1", "prop2"));
        topology.setOutputAttributes(new HashMap<String, Set<String>>());
        topology.getOutputAttributes().put("node1", Sets.newHashSet("att1", "att2"));

        Map<String, Object> velocityCtx = new HashMap<String, Object>();
        velocityCtx.put("topology", topology);
        velocityCtx.put("template_name", "template-id");
        velocityCtx.put("template_version", "1.0.0-SNAPSHOT");
        velocityCtx.put("template_author", "Foo Bar");
        velocityCtx.put("application_description", "Here is a \nmultiline description");

        StringWriter writer = new StringWriter();
        VelocityUtil.generate("org/alien4cloud/tosca/exporter/topology-alien_dsl_1_3_0.yml.vm", writer, velocityCtx);
        System.out.println(writer.toString());
    }

    private Map<String, AbstractPropertyValue> buildSamplePropertyValueMap() {
        Map<String, AbstractPropertyValue> result = new HashMap<String, AbstractPropertyValue>();
        result.put("prop1", new ScalarPropertyValue("value1"));

        FunctionPropertyValue fpv1 = new FunctionPropertyValue();
        fpv1.setFunction("get_property");
        fpv1.setParameters(Lists.newArrayList("p1", "p2"));
        result.put("prop2", fpv1);

        FunctionPropertyValue fpv2 = new FunctionPropertyValue();
        fpv2.setFunction("get_input");
        fpv2.setParameters(Lists.newArrayList("p1"));
        result.put("prop3", fpv2);

        result.put("prop4", null);
        result.put("prop5", new ScalarPropertyValue("a value containing a ["));
        result.put("prop6", new ScalarPropertyValue("a value containing a ]"));
        result.put("prop7", new ScalarPropertyValue("a value containing a {"));
        result.put("prop8", new ScalarPropertyValue("a value containing a }"));
        result.put("prop9", new ScalarPropertyValue("a value containing a :"));
        result.put("prop9", new ScalarPropertyValue("a value containing a \""));
        result.put("prop9", new ScalarPropertyValue("a value containing a : and a \""));
        result.put("prop10", new ScalarPropertyValue(" a value starting with a space"));
        result.put("prop11", new ScalarPropertyValue("a value ending with a space "));
        return result;
    }

    private List<PropertyConstraint> getConstraintList() {
        List<PropertyConstraint> result = new ArrayList<PropertyConstraint>();
        ValidValuesConstraint c1 = new ValidValuesConstraint();
        c1.setValidValues(Lists.newArrayList("one", "two", "tree"));
        result.add(c1);
        
        GreaterOrEqualConstraint c2 = new GreaterOrEqualConstraint();
        c2.setGreaterOrEqual("2");
        result.add(c2);
        GreaterThanConstraint c3 = new GreaterThanConstraint();
        c3.setGreaterThan("3");
        result.add(c3);
        LessOrEqualConstraint c4 = new LessOrEqualConstraint();
        c4.setLessOrEqual("4");
        result.add(c4);
        LessThanConstraint c5 = new LessThanConstraint();
        c5.setLessThan("5");
        result.add(c5);
        LengthConstraint c6 = new LengthConstraint();
        c6.setLength(6);
        result.add(c6);
        MaxLengthConstraint c7 = new MaxLengthConstraint();
        c7.setMaxLength(7);
        result.add(c7);
        MinLengthConstraint c8 = new MinLengthConstraint();
        c8.setMinLength(8);
        result.add(c8);
        PatternConstraint c9 = new PatternConstraint();
        c9.setPattern("9+");
        result.add(c9);
        EqualConstraint c10 = new EqualConstraint();
        c10.setEqual("10");
        result.add(c10);
        InRangeConstraint c11 = new InRangeConstraint();
        c11.setInRange(Lists.newArrayList("11", "12"));
        result.add(c11);
        
        return result;
    }

}
