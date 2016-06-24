package org.alien4cloud.tosca.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

import com.google.common.collect.Lists;

import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.DirectoryJSonWalker;
import alien4cloud.utils.TreeNode;
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
public class TopologyEditionContext {
    // FIXME add node types and other elements we can get from a CSAR to enable full archive edition rather than just topology edition.
    /** The topology under edition in it's last saved state. */
    private Topology savedTopology;
    /** The topology as processed after applying all operations on the saved topology. */
    private Topology currentTopology;
    /** The tosca context associated with the topology context. It caches the types loaded from ElasticSearch. */
    private ToscaContext.Context toscaContext;
    /** Path to the topology's local git repository. */
    private Path localGitPath;
    /** The operation under processing */
    private AbstractEditorOperation currentOperation;
    /** List of commands that have been applied to the topology from the last-saved version. */
    private List<AbstractEditorOperation> operations = Lists.newArrayList();
    /** Root of the file hierarchy. */
    private TreeNode archiveContentTree;

    /**
     * Create a new instance of a topology edition context from an existing topology.
     * 
     * @param initial The topology for which to create the context.
     * @param editionClone A clone of the initial topology that will be updated to represent the current state of the topology.
     * @param localGitPath The git location associated with the topology.
     */
    public TopologyEditionContext(Topology initial, Topology editionClone, Path localGitPath) throws IOException {
        //
        this.savedTopology = initial;
        this.currentTopology = editionClone;
        this.toscaContext = new ToscaContext.Context(this.savedTopology.getDependencies());
        this.localGitPath = localGitPath;
        // initialize the file tree based on the git repository location
        this.archiveContentTree = DirectoryJSonWalker.getDirectoryTree(this.localGitPath);
    }
}