package alien4cloud.topology.validation;

import alien4cloud.plugin.PluginManager;
import alien4cloud.topology.ITopologyValidatorPlugin;
import alien4cloud.topology.ITopologyValidatorPluginLogger;
import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.topology.TopologyValidatorRegistry;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.topology.task.PluginLogTask;
import alien4cloud.utils.CloneUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TopologyPluginValidationService {

    @Resource
    TopologyValidatorRegistry registry;

    @Resource
    PluginManager pluginManager;

    public void validate(TopologyValidationResult dto,Topology topology) {
        // Clone the topology as it will be processed exernally and we do not want unattended modifications
        Topology clone = CloneUtil.clone(topology);

        for (Map.Entry<String,Map<String,ITopologyValidatorPlugin>> pluginEntries : registry.getInstancesByPlugins().entrySet()) {
            List<AbstractTask> tasks = Lists.newArrayList();
            List<AbstractTask> warns = Lists.newArrayList();
            List<AbstractTask> infos = Lists.newArrayList();

            String pluginName = pluginManager.getPluginDescriptor(pluginEntries.getKey()).getName();

            for (Map.Entry<String,ITopologyValidatorPlugin> validatorEntries : pluginEntries.getValue().entrySet()) {
                validatorEntries.getValue().validate(clone, getLogger(pluginName,tasks,warns,infos));

                dto.addTasks(tasks);
                dto.addWarnings(warns);
                dto.addInfos(infos);
            }
        }
    }

    private ITopologyValidatorPluginLogger getLogger(String pluginName,List<AbstractTask> tasks,List<AbstractTask> warns,List<AbstractTask> infos) {
        return new ITopologyValidatorPluginLogger() {
            @Override
            public void error(String message) {
                PluginLogTask task = new PluginLogTask();
                task.setPlugin(pluginName);
                task.setMessage(message);
                tasks.add(task);
            }

            @Override
            public void warn(String message) {
                PluginLogTask task = new PluginLogTask();
                task.setPlugin(pluginName);
                task.setMessage(message);
                warns.add(task);
            }

            @Override
            public void info(String message) {
                PluginLogTask task = new PluginLogTask();
                task.setPlugin(pluginName);
                task.setMessage(message);
                infos.add(task);
            }
        };
    }
}
