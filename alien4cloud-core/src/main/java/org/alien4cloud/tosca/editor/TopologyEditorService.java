package org.alien4cloud.tosca.editor;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.commands.ICommand;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;

/**
 * This service manages command execution on the TOSCA topology template editor.
 */
public class TopologyEditorService {
    @Resource
    private TopologyEditionContextManager topologyEditionContextManager;

    // trigger editor operation
    @MessageMapping("/topologyeditor/{topologyId}")
    public void execute(@DestinationVariable String topologyId, ICommand command) {
        // get the topology context.
        TopologyEditionContext editionContext = topologyEditionContextManager.get(topologyId);
        // check that commands can be executed
        editionContext.
        // no other command has been executed concurrently

        // find the handler for the command

        //

    }

    // upload file in the archive

    // save
    public void save() {
        // save is updating the topology on elasticsearch and also performs a commit

    }

    /**
     * This operations performs
     */
    public void pull() {
        // pull can be done only if there is no unsaved commands

        // This operation just fails in case of conflicts

        // The topology is updated

    }

    // full upload of template (or yaml edition)

}