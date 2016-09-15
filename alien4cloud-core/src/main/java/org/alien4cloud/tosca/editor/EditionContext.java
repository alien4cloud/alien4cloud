package org.alien4cloud.tosca.editor;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.DirectoryJSonWalker;
import alien4cloud.utils.TreeNode;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.RecoverTopologyOperation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Topology edition context is related to a specific topology that is currently under edition.
 * 
 * Edition context is closed automatically when no users are currently editing it or when inactive after 5 minutes.
 */
@Getter
@Setter
@NoArgsConstructor
public class EditionContext {
    /** The archive under edition. Note that we don't allow updates to this object in the editor. */
    private Csar csar;

    // TODO add node types and other elements we can get from a CSAR to enable full archive edition rather than just topology edition.

    /** The topology as processed after applying all operations on the saved topology. */
    private Topology topology;

    /** The tosca context associated with the topology context. It caches the types loaded from ElasticSearch. */
    private ToscaContext.Context toscaContext;
    /** Path to the topology's local git repository. */
    private Path localGitPath;
    /** The operation under processing if any or null. */
    private AbstractEditorOperation currentOperation;
    /** The index of the operation considered as the last operation (may be in the middle based on undo/redo) */
    private int lastOperationIndex = -1;
    /** The index of the last operation that has been saved (in ES and commit). */
    private int lastSavedOperationIndex = -1;
    /** List of commands that have been applied to the topology from the last-saved version. */
    private List<AbstractEditorOperation> operations = Lists.newArrayList();
    /** Root of the file hierarchy. */
    private TreeNode archiveContentTree;
    /** List of the operations generated to recover the topology */
    private RecoverTopologyOperation recoveryOperation;

    /**
     * Create a new instance of a topology edition context from an existing topology.
     *
     * @param csar The archive under edition.
     * @param topology The topology for which to create the context.
     * @param localGitPath The git location associated with the topology.
     */
    public EditionContext(Csar csar, Topology topology, Path localGitPath) throws IOException {
        this.csar = csar;
        this.topology = topology;
        this.toscaContext = new ToscaContext.Context(topology.getDependencies());
        this.localGitPath = localGitPath;
        // initialize the file tree based on the git repository location
        this.archiveContentTree = DirectoryJSonWalker.getDirectoryTree(this.localGitPath);
    }

    /**
     * Reset the topology context to it's initial state.
     * 
     * @param editionClone The clone of the initial topology. // TODO better use an inner java cloning.
     * @throws IOException In case we wait to initialize the archive content tree.
     */
    public void reset(Topology editionClone) throws IOException {
        this.topology = editionClone;
        this.toscaContext = new ToscaContext.Context(topology.getDependencies());
        this.archiveContentTree = DirectoryJSonWalker.getDirectoryTree(this.localGitPath);
    }
}