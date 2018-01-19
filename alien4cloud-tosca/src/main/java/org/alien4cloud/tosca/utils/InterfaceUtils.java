package org.alien4cloud.tosca.utils;

import org.alien4cloud.tosca.model.definitions.ImplementationArtifact;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;

import java.util.Map;
import java.util.Optional;

import static alien4cloud.utils.AlienUtils.safe;

public final class InterfaceUtils {
    private InterfaceUtils() {
    }

    public static ImplementationArtifact getArtifact(Map<String, Interface> interfaceMap, String interfaceName, String operationName) {
        Optional.of(safe(interfaceMap).get(interfaceName)).map()
        Interface interfaz = safe(interfaceMap).get(interfaceName);
        if (interfaz == null) {
            return null;
        }
        Operation operation = safe(interfaz.getOperations()).get(operationName);
        if(operation )
    }

}
