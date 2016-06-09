package org.alien4cloud.tosca.editor;

import java.nio.file.Path;
import java.util.List;

import org.alien4cloud.tosca.editor.commands.IEditorOperation;

import com.google.common.collect.Lists;

import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.context.ToscaContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Topology edition context is related to a specific topology that is currently under edition.
 * 
 * Edition context is closed automatically when no users are currently editing it or when inactive after 5 minutes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopologyEditionContext {
    /** The topology under edition in it's last saved state. */
    private Topology topology;
    /** The tosca context associated with the topology context. */
    private ToscaContext toscaContext;
    /** Path to the topology's local git repository. */
    private Path topologyLocalGit;
    /** List of commands that have been applied to the topology from the last-saved version. */
    private List<IEditorOperation> operations = Lists.newArrayList();
}