package alien4cloud.topology.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alien4cloud.tosca.model.definitions.ImplementationArtifact;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.topology.warning.IllegalOperationWarning;
import alien4cloud.tosca.context.ToscaContext;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TopologyServiceInterfaceOverrideCheckerService {

    private boolean isService(NodeTemplate nodeTemplate){
        return ServiceNodeTemplate.class.isAssignableFrom(nodeTemplate.getClass());
    }

    public List<IllegalOperationWarning> findWarnings(Topology topology) {
        Set<IllegalOperationWarning> warnings = Sets.newHashSet();
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();

        for (Entry<String, NodeTemplate> nodeTempEntry : nodeTemplates.entrySet()) {
            NodeTemplate nodeTemplate = nodeTempEntry.getValue();

            Map<String, RelationshipTemplate> relationships = nodeTemplate.getRelationships();
            if(relationships != null){
                for(Entry<String, RelationshipTemplate> entry : relationships.entrySet() ){

                    RelationshipTemplate relationshipTemplate = entry.getValue();

                    String target = relationshipTemplate.getTarget();
                    NodeTemplate targetNodeTemplate = nodeTemplates.get(target);
                    boolean serviceIsSource = isService(nodeTemplate);
                    boolean serviceIsTarget = isService(targetNodeTemplate);
                    if(serviceIsSource || serviceIsTarget){
                        RelationshipType relationshipType = ToscaContext.get(RelationshipType.class, relationshipTemplate.getType());
                        if(relationshipType != null){
                            Map<String, Interface> interfaces = relationshipType.getInterfaces();
                            if(interfaces != null){
                                interfaces.forEach((relationshipName,relationshipInterface) -> {
                                    Map<String, Operation> operations = relationshipInterface.getOperations();
                                    if(operations != null){
                                        operations.forEach((operationName,operation) -> {
                                            String serviceName;
                                            if(serviceIsTarget){
                                                serviceName = nodeTemplate.getName();
                                                switch (operationName.toLowerCase()){
                                                    case "add_source":
                                                    case "remove_source":
                                                    case "source_changed":
                                                    case "post_configure_target":
                                                    case "pre_configure_target":
                                                        ImplementationArtifact artifact = operation.getImplementationArtifact();
                                                        boolean stepDoSomething = artifact != null;
                                                        if(stepDoSomething) {
                                                            addWarning(warnings, nodeTemplate, relationshipInterface, operationName, serviceName, relationshipTemplate.getType());
                                                        }
                                                        break;
                                                }
                                            }

                                            if(serviceIsSource){
                                                serviceName = targetNodeTemplate.getName();
                                                switch (operationName.toLowerCase()){
                                                    case "add_target":
                                                    case "remove_target":
                                                    case "target_changed":
                                                    case "pre_configure_source":
                                                    case "post_configure_source":
                                                        ImplementationArtifact artifact = operation.getImplementationArtifact();
                                                        boolean stepDoSomething = artifact != null;
                                                        if(stepDoSomething) {
                                                            addWarning(warnings, nodeTemplate, relationshipInterface, operationName, serviceName, relationshipTemplate.getType());
                                                        }
                                                        break;
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }

        return warnings.isEmpty() ? null : new ArrayList<>(warnings);
    }

    private void addWarning(Set<IllegalOperationWarning> warnings, NodeTemplate nodeTemplate, Interface toscaInterface, String operationName, String serviceName, String relationshipType) {
        IllegalOperationWarning illegalOperationWarning = new IllegalOperationWarning();
        illegalOperationWarning.setNodeTemplateName(nodeTemplate.getName());
        illegalOperationWarning.setOperationName(operationName);
        illegalOperationWarning.setServiceName(serviceName);
        illegalOperationWarning.setRelationshipType(relationshipType);
        illegalOperationWarning.setInterfaceName(toscaInterface.getType());
        warnings.add(illegalOperationWarning);
    }
}