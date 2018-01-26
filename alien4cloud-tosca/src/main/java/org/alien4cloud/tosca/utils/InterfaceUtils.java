package org.alien4cloud.tosca.utils;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;

import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.apache.commons.lang3.StringUtils;

public final class InterfaceUtils {
    private InterfaceUtils() {
    }

    public static Operation getOperationIfArtifactDefined(Map<String, Interface> interfaceMap, String interfaceName, String operationName) {
        Interface interfaz = safe(interfaceMap).get(interfaceName);
        if (interfaz == null) {
            return null;
        }
        Operation operation = safe(interfaz.getOperations()).get(operationName);
        if (operation == null || operation.getImplementationArtifact() == null || StringUtils.isBlank(operation.getImplementationArtifact().getArtifactRef())) {
            return null;
        }
        return operation;
    }
}
