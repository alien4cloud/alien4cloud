package org.alien4cloud.tosca.model.workflow.declarative;

import java.io.IOException;

import org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants;
import org.junit.Assert;
import org.junit.Test;

import alien4cloud.utils.YamlParserUtil;

public class DeclarativeWorkflowTest {

    @Test
    public void default_declarative_workflow_could_be_parsed_from_configuration() throws IOException {
        DefaultDeclarativeWorkflows defaultDeclarativeWorkflows = YamlParserUtil.parse(
                DefaultDeclarativeWorkflows.class.getClassLoader().getResourceAsStream("default-declarative-workflows.yml"), DefaultDeclarativeWorkflows.class);

        Assert.assertNotNull(defaultDeclarativeWorkflows.getNodeWorkflows());
        Assert.assertNotNull(defaultDeclarativeWorkflows.getRelationshipWorkflows());
        Assert.assertNotNull(defaultDeclarativeWorkflows.getRelationshipsWeaving());

        Assert.assertTrue(defaultDeclarativeWorkflows.getNodeWorkflows().containsKey(NormativeWorkflowNameConstants.INSTALL));
        Assert.assertTrue(defaultDeclarativeWorkflows.getRelationshipWorkflows().containsKey(NormativeWorkflowNameConstants.INSTALL));
        Assert.assertTrue(defaultDeclarativeWorkflows.getRelationshipsWeaving().containsKey(NormativeWorkflowNameConstants.INSTALL));
    }
}
