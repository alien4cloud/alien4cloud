package alien4cloud.paas.wf.util;

import java.util.Comparator;
import java.util.Map;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.RelationshipWorkflowStep;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.declarative.RelationshipOperationHost;

public class WorkflowStepWeightComparator implements Comparator<WorkflowStep> {

    private Map<String, Integer> nodesWeights;

    private Topology topology;

    public WorkflowStepWeightComparator(Map<String, Integer> nodesWeights, Topology topology) {
        this.nodesWeights = nodesWeights;
        this.topology = topology;
    }

    public static int compareWorkflowSteps(Topology topology, WorkflowStep l, WorkflowStep r) {
        if (l instanceof RelationshipWorkflowStep) {
            if (r instanceof RelationshipWorkflowStep) {
                RelationshipWorkflowStep lr = (RelationshipWorkflowStep) l;
                RelationshipWorkflowStep rr = (RelationshipWorkflowStep) r;
                if (lr.getOperationHost().equals(RelationshipOperationHost.TARGET.toString())) {
                    if (rr.getOperationHost().equals(RelationshipOperationHost.TARGET.toString())) {
                        return 0;
                    } else {
                        // l is target operation, r is source operation, execute for source before target so put l after r
                        return 1;
                    }
                } else {
                    if (rr.getOperationHost().equals(RelationshipOperationHost.TARGET.toString())) {
                        // l is source operation, r is target operation, execute for source before target so put l before r
                        return -1;
                    } else {
                        // l and r are source operations
                        if (lr.getTarget().equals(rr.getTarget())) {
                            // l and r are operation concerning the same target
                            NodeTemplate nodeTemplate = topology.getNodeTemplates().get(lr.getTarget());
                            Integer lri = 0;
                            Integer rri = 0;
                            int counter = 0;
                            for (Map.Entry<String, RelationshipTemplate> relationshipTemplateEntry : nodeTemplate.getRelationships().entrySet()) {
                                // Hope that's linked hash map here so order is preserved
                                if (relationshipTemplateEntry.getKey().equals(lr.getTargetRelationship())) {
                                    lri = counter;
                                }
                                if (relationshipTemplateEntry.getKey().equals(rr.getTargetRelationship())) {
                                    rri = counter;
                                }
                                counter++;
                            }
                            // Execute operation coming from relationship in the same order at the source side
                            return lri.compareTo(rri);
                        } else {
                            // not the same target so don't care about order
                            return 0;
                        }
                    }
                }
            } else {
                // execute node step before relationship step
                return -1;
            }
        } else {
            // l is node step
            if (r instanceof RelationshipWorkflowStep) {
                // execute node step before relationship step
                return 1;
            } else {
                // both are node steps then don't care about order
                return 0;
            }
        }
    }

    @Override
    public int compare(WorkflowStep l, WorkflowStep r) {
        Integer lw = nodesWeights.get(l.getName());
        Integer rw = nodesWeights.get(r.getName());
        int wc = lw.compareTo(rw);
        if (wc != 0) {
            return wc;
        } else {
            // If two steps have the same weight then take a closer look
            return compareWorkflowSteps(topology, l, r);
        }
    }
}
