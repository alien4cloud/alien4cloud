package alien4cloud.tosca.container.model.topology;

import java.util.HashSet;

import org.alien4cloud.tosca.catalog.index.ICsarDependencyLoader;
import org.alien4cloud.tosca.model.CSARDependency;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Sets;

import alien4cloud.tosca.container.ToscaTypeLoader;

public class ToscaTypeLoaderTest {

    private ICsarDependencyLoader dependencyLoader;

    private ToscaTypeLoader loader;

    private CSARDependency baseTypes = new CSARDependency("tosca-base-types", "1.0");

    private CSARDependency baseTypesV2 = new CSARDependency("tosca-base-types", "2.0");

    private CSARDependency javaTypes = new CSARDependency("java-types", "1.0");

    private CSARDependency javaTypesV2 = new CSARDependency("java-types", "2.0");

    @Before
    public void before() {
        dependencyLoader = Mockito.mock(ICsarDependencyLoader.class);
        Mockito.when(dependencyLoader.getDependencies("tosca-base-types", "1.0")).thenReturn(new HashSet<CSARDependency>());
        Mockito.when(dependencyLoader.getDependencies("java-types", "1.0")).thenReturn(Sets.newHashSet(baseTypes));
        Mockito.when(dependencyLoader.getDependencies("java-types", "2.0")).thenReturn(Sets.newHashSet(baseTypesV2));
        loader = new ToscaTypeLoader(dependencyLoader);
    }

    private void loadTomcatGroup() {
        // All nodes are placed first
        loader.loadType("tosca.nodes.Compute", baseTypes);
        loader.loadType("tosca.nodes.Java", javaTypes);
        loader.loadType("tosca.nodes.Tomcat", javaTypes);
        loader.loadType("tosca.nodes.War", javaTypes);
        // Java hosted on compute
        loader.loadType("tosca.relationships.HostedOn", baseTypes);
        // Tomcat hosted on compute
        loader.loadType("tosca.relationships.HostedOn", baseTypes);
        // War hosted on tomcat
        loader.loadType("tosca.relationships.WarDeployedOn", javaTypes);
        // Tomcat depends on Java
        loader.loadType("tosca.relationships.DependsOn", baseTypes);
    }

    private void loadGigaSpacesGroup() {
        // All gigaspaces group nodes are placed first
        loader.loadType("tosca.nodes.Compute", baseTypesV2);
        loader.loadType("tosca.nodes.Java", javaTypesV2);
        loader.loadType("tosca.nodes.GigaSpaces", javaTypesV2);
        // Java hosted on compute
        loader.loadType("tosca.relationships.HostedOn", baseTypesV2);
        // GigaSpaces hosted on compute
        loader.loadType("tosca.relationships.HostedOn", baseTypes);
        // GigaSpaces depends on Java
        loader.loadType("tosca.relationships.DependsOn", baseTypes);
        // War connect to gigaspaces
        loader.loadType("tosca.relationships.ConnectedTo", baseTypes);
    }

    private void unloadTomcatGroup() {
        // _______________________________________________________________
        // Delete compute will trigger following actions on type loader
        loader.unloadType("tosca.nodes.Compute");
        // Java hosted on compute
        loader.unloadType("tosca.relationships.HostedOn");

        // _______________________________________________________________
        // Delete relationship Tomcat depends on Java
        loader.unloadType("tosca.relationships.DependsOn");

        // _______________________________________________________________
        // Delete java
        loader.unloadType("tosca.nodes.Java");

        // _______________________________________________________________
        // Delete tomcat will trigger following actions on type loader
        loader.unloadType("tosca.nodes.Tomcat");
        // Tomcat hosted on compute
        loader.unloadType("tosca.relationships.HostedOn");
        // War hosted on tomcat
        loader.unloadType("tosca.relationships.WarDeployedOn");

        // _______________________________________________________________
        loader.unloadType("tosca.nodes.War");
    }

    private void unloadGigaSpacesGroup() {
        // _______________________________________________________________
        // War connected to GigaSpaces
        loader.unloadType("tosca.relationships.ConnectedTo");
        // _______________________________________________________________
        // Delete compute will trigger following actions on type loader
        loader.unloadType("tosca.nodes.Compute");
        // Java hosted on compute
        loader.unloadType("tosca.relationships.HostedOn");
        // GigaSpaces hosted on compute
        loader.unloadType("tosca.relationships.HostedOn");

        // _______________________________________________________________
        // Delete relationship GigaSpaces depends on Java
        loader.unloadType("tosca.relationships.DependsOn");

        // _______________________________________________________________
        // Delete java
        loader.unloadType("tosca.nodes.Java");

        // _______________________________________________________________
        // Delete GigaSpaces will trigger following actions on type loader
        loader.unloadType("tosca.nodes.GigaSpaces");
    }

    @Test
    public void testStandardUseCase() {
        loadTomcatGroup();
        Assert.assertEquals(2, loader.getDependenciesMap().keySet().size());
        Assert.assertTrue(loader.getDependenciesMap().containsKey(baseTypes));
        Assert.assertTrue(loader.getDependenciesMap().containsKey(javaTypes));

        Assert.assertEquals(Sets.newHashSet("tosca.nodes.Compute", "tosca.nodes.Java", "tosca.relationships.HostedOn", "tosca.relationships.DependsOn",
                "tosca.relationships.WarDeployedOn", "tosca.nodes.War", "tosca.nodes.Tomcat"), loader.getDependenciesMap().get(baseTypes));
        Assert.assertEquals(Sets.newHashSet("tosca.nodes.Java", "tosca.nodes.Tomcat", "tosca.nodes.War", "tosca.relationships.WarDeployedOn"),
                loader.getDependenciesMap().get(javaTypes));

        Assert.assertEquals(1, loader.getTypeUsagesMap().get("tosca.nodes.Compute").intValue());
        Assert.assertEquals(1, loader.getTypeUsagesMap().get("tosca.nodes.Java").intValue());
        Assert.assertEquals(1, loader.getTypeUsagesMap().get("tosca.nodes.Tomcat").intValue());
        Assert.assertEquals(1, loader.getTypeUsagesMap().get("tosca.nodes.War").intValue());

        Assert.assertEquals(2, loader.getTypeUsagesMap().get("tosca.relationships.HostedOn").intValue());
        Assert.assertEquals(1, loader.getTypeUsagesMap().get("tosca.relationships.DependsOn").intValue());
        Assert.assertEquals(1, loader.getTypeUsagesMap().get("tosca.relationships.WarDeployedOn").intValue());

        unloadTomcatGroup();

        Assert.assertTrue(loader.getDependenciesMap().isEmpty());
        Assert.assertTrue(loader.getTypeUsagesMap().isEmpty());
    }

    @Test
    public void testOverride() {

        // All tomcat group nodes are placed first
        loadTomcatGroup();
        loadGigaSpacesGroup();

        Assert.assertEquals(2, loader.getDependenciesMap().keySet().size());
        Assert.assertTrue(loader.getDependenciesMap().containsKey(baseTypesV2));
        Assert.assertTrue(loader.getDependenciesMap().containsKey(javaTypesV2));

        Assert.assertEquals(Sets.newHashSet("tosca.nodes.Compute", "tosca.nodes.Java", "tosca.nodes.Tomcat", "tosca.relationships.HostedOn",
                "tosca.relationships.DependsOn", "tosca.relationships.WarDeployedOn", "tosca.nodes.War", "tosca.nodes.GigaSpaces",
                "tosca.relationships.ConnectedTo"), loader.getDependenciesMap().get(baseTypesV2));
        Assert.assertEquals(
                Sets.newHashSet("tosca.nodes.Java", "tosca.nodes.Tomcat", "tosca.nodes.War", "tosca.nodes.GigaSpaces", "tosca.relationships.WarDeployedOn"),
                loader.getDependenciesMap().get(javaTypesV2));

        unloadTomcatGroup();
        unloadGigaSpacesGroup();

        Assert.assertTrue(loader.getDependenciesMap().isEmpty());
        Assert.assertTrue(loader.getTypeUsagesMap().isEmpty());
    }
}
