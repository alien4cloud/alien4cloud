package alien4cloud.rest.topology;

import java.util.Set;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;

/**
 * Manage topology workflows.
 */
@Slf4j
@RestController
@RequestMapping({"/rest/topologies", "/rest/v1/topologies"})
public class TopologyWorkflowController {

    @Resource
    private TopologyService topologyService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @RequestMapping(value = "/{topologyId}/workflows", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Set<String>> getWorkflows(@PathVariable String topologyId) {
        return new RestResponse<Set<String>>();
    }

    @RequestMapping(value = "/{topologyId}/workflows", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Workflow> createWorkflow(@PathVariable String topologyId) {

        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Workflow wf = workflowBuilderService.ceateWorkflow(topology);
        topologyServiceCore.save(topology);
        return RestResponseBuilder.<Workflow> builder().data(wf).build();
    }

    @RequestMapping(value = "/{topologyId}/workflows/{workflowName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> removeWorkflow(@PathVariable String topologyId, @PathVariable String workflowName) {

        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Workflow wf = topology.getWorkflows().remove(workflowName);
        if (wf.isStandard()) {
            throw new RuntimeException("standard wf can not be removed");
        }
        topologyServiceCore.save(topology);
        return new RestResponse<Void>();
    }

    @RequestMapping(value = "/{topologyId}/workflows/{workflowName}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Workflow> renameWorkflow(@PathVariable String topologyId, @PathVariable String workflowName, @RequestParam String newName) {

        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        if (topology.getWorkflows().containsKey(newName)) {
            throw new AlreadyExistException(String.format("The workflow named '%s' already exists", newName));
        }
        Workflow wf = topology.getWorkflows().remove(workflowName);
        if (wf.isStandard()) {
            throw new RuntimeException("standard wf can not be renamed");
        }
        wf.setName(newName);
        topology.getWorkflows().put(newName, wf);
        topologyServiceCore.save(topology);
        return RestResponseBuilder.<Workflow> builder().data(wf).build();
    }

    @RequestMapping(value = "/{topologyId}/workflows/{workflowName}/init", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Workflow> initWorkflow(@PathVariable String topologyId, @PathVariable String workflowName) {

        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Workflow wf = workflowBuilderService.reinitWorkflow(workflowName, workflowBuilderService.buildTopologyContext(topology));
        topologyServiceCore.save(topology);
        return RestResponseBuilder.<Workflow> builder().data(wf).build();
    }

    @RequestMapping(value = "/{topologyId}/workflows/{workflowName}/edges/{from}/{to}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Workflow> removeEdge(@PathVariable String topologyId, @PathVariable String workflowName, @PathVariable String from,
            @PathVariable String to) {

        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Workflow wf = workflowBuilderService.removeEdge(topology, workflowName, from, to);
        topologyServiceCore.save(topology);
        return RestResponseBuilder.<Workflow> builder().data(wf).build();
    }

    @RequestMapping(value = "/{topologyId}/workflows/{workflowName}/steps/{stepId}/connectFrom", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Workflow> connectStepFrom(@PathVariable String topologyId, @PathVariable String workflowName, @PathVariable String stepId,
            @RequestBody String[] stepNames) {

        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Workflow wf = workflowBuilderService.connectStepFrom(topology, workflowName, stepId, stepNames);
        topologyServiceCore.save(topology);
        return RestResponseBuilder.<Workflow> builder().data(wf).build();
    }

    @RequestMapping(value = "/{topologyId}/workflows/{workflowName}/steps/{stepId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Workflow> renameStep(@PathVariable String topologyId, @PathVariable String workflowName, @PathVariable String stepId,
            @RequestParam String newStepName) {

        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Workflow wf = workflowBuilderService.renameStep(topology, workflowName, stepId, newStepName);
        topologyServiceCore.save(topology);
        return RestResponseBuilder.<Workflow> builder().data(wf).build();
    }

    @RequestMapping(value = "/{topologyId}/workflows/{workflowName}/steps/{stepId}/connectTo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Workflow> connectStepTo(@PathVariable String topologyId, @PathVariable String workflowName, @PathVariable String stepId,
            @RequestBody String[] stepNames) {

        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Workflow wf = workflowBuilderService.connectStepTo(topology, workflowName, stepId, stepNames);
        topologyServiceCore.save(topology);
        return RestResponseBuilder.<Workflow> builder().data(wf).build();
    }

    @RequestMapping(value = "/{topologyId}/workflows/{workflowName}/steps/{stepId}/swap", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Workflow> swap(@PathVariable String topologyId, @PathVariable String workflowName, @PathVariable String stepId,
            @RequestParam String targetId) {

        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Workflow wf = workflowBuilderService.swapSteps(topology, workflowName, stepId, targetId);
        topologyServiceCore.save(topology);
        return RestResponseBuilder.<Workflow> builder().data(wf).build();
    }

    @RequestMapping(value = "/{topologyId}/workflows/{workflowName}/activities", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Workflow> addActivity(@PathVariable String topologyId, @PathVariable String workflowName,
            @RequestBody TopologyWorkflowAddActivityRequest activityRequest) {

        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Workflow wf = workflowBuilderService.addActivity(topology, workflowName, activityRequest.getRelatedStepId(), activityRequest.isBefore(),
                activityRequest.getActivity());
        topologyServiceCore.save(topology);
        return RestResponseBuilder.<Workflow> builder().data(wf).build();
    }

    @RequestMapping(value = "/{topologyId}/workflows/{workflowName}/steps/{stepId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Workflow> removeStep(@PathVariable String topologyId, @PathVariable String workflowName, @PathVariable String stepId) {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Workflow wf = workflowBuilderService.removeStep(topology, workflowName, stepId, false);
        topologyServiceCore.save(topology);
        return RestResponseBuilder.<Workflow> builder().data(wf).build();
    }

}
