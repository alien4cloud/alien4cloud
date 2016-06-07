package org.alien4cloud.tosca.editor;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.commands.IEditorOperation;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

/**
 * This service manages command execution on the TOSCA topology template editor.
 */
@Controller
public class TopologyEditorService {
    @Resource
    private TopologyEditionContextManager topologyEditionContextManager;

    // trigger editor operation
    @MessageMapping("/topology-editor/{topologyId}")
    public void execute(@DestinationVariable String topologyId, IEditorOperation operation) {
        // get the topology context.
        TopologyEditionContext editionContext = topologyEditionContextManager.get(topologyId);
        // check that commands can be executed
        // editionContext.
        // no other command has been executed concurrently

        // find the handler for the command

        //

    }

    // upload file in the archive

    // save
    public void save() {
        // save is updating the topology on elasticsearch and also performs a local commit

    }

    /**
     * Performs a git pull.
     */
    public void pull() {
        // pull can be done only if there is no unsaved commands

        // This operation just fails in case of conflicts

        // The topology is updated

    }

    /**
     * Performs a git push.
     */
    public void push() {

    }

    /**
     * Upload a file in the archive.
     *
     * Note, if the file updates the YAML we have to process it fully.
     */
    public void uploadFile() {

    }

    /**
     * Upload a full tosca-archive to override a given topology.
     */
    public void uploadArchive() {

    }
}