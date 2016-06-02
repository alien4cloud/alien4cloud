package org.alien4cloud.tosca.editor;

import java.nio.file.Path;
import java.util.List;

import alien4cloud.model.topology.Topology;
import com.google.common.collect.Lists;
import org.alien4cloud.tosca.editor.commands.ICommand;

import alien4cloud.tosca.context.ToscaContext;

/**
 * Topology edition context is related to a specific topology that is currently under edition.
 * 
 * Edition context is closed automatically when no users are currently editing it or when inactive after 5 minutes.
 */
public class TopologyEditionContext {
    /** The topology under edition. */
    private Topology topology;
    /** The tosca context associated with the topology context. */
    private ToscaContext toscaContext;
    /** Path to the topology's local git repository. */
    private Path topologyLocalGit;
    /** List of commands that have been applied to the topology from the last-saved version. */
    private List<ICommand> commands = Lists.newArrayList();
}