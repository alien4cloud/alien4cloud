package org.alien4cloud.tosca.editor.exception;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.RecoverTopologyOperation;

/**
 * Exception to be thrown when the topology needs to be recovered.
 */
@Getter
@Setter
public class RecoverTopologyException extends RuntimeException {
    RecoverTopologyOperation operation;

    public RecoverTopologyException(String message) {
        super(message);
    }

    public RecoverTopologyException(String message, RecoverTopologyOperation recoveryOperation) {
        this(message);
        this.operation = recoveryOperation;
    }
}