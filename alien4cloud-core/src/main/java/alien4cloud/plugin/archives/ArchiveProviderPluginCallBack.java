package alien4cloud.plugin.archives;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import alien4cloud.security.AuthorizationUtil;
import org.alien4cloud.tosca.catalog.index.ArchiveIndexer;
import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.model.Csar;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.component.repository.exception.ToscaTypeAlreadyDefinedInOtherCSAR;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.common.Usage;
import alien4cloud.model.components.CSARSource;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.plugin.IPluginLoadingCallback;
import alien4cloud.plugin.exception.PluginArchiveException;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.rest.utils.JsonUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Load and Index archives if the plugin provided implementations of @{@link IArchiveProviderPlugin}
 */
@Slf4j
@Component
public class ArchiveProviderPluginCallBack implements IPluginLoadingCallback {
    @Inject
    private CsarService csarService;
    @Inject
    private ArchiveIndexer archiveIndexer;

    private void indexArchives(Collection<PluginArchive> archives) {
        for (PluginArchive pluginArchive : safe(archives)) {
            try {
                archiveIndexer.importArchive(pluginArchive.getArchive(), CSARSource.PLUGIN, pluginArchive.getArchiveFilePath(), Lists.newArrayList());
                // TODO push indexed event
                // publishLocationTypeIndexedEvent(pluginArchive.getArchive().getNodeTypes().values(), orchestratorFactory, null);
            } catch (AlreadyExistException e) {
                log.debug("Skipping archive import as the released version already exists in the repository. " + e.getMessage());
            } catch (CSARUsedInActiveDeployment e) {
                log.debug("Skipping archive import as it is used in an active deployment. " + e.getMessage());
            } catch (ToscaTypeAlreadyDefinedInOtherCSAR e) {
                log.debug("Skipping archive import, it's archive contain's a tosca type already defined in an other archive." + e.getMessage());
            }
        }
    }

    @Override
    @SneakyThrows(PluginArchiveException.class)
    public synchronized void onPluginLoaded(ManagedPlugin managedPlugin) {
        Map<String, IArchiveProviderPlugin> archiveProviderBeans = managedPlugin.getPluginContext().getBeansOfType(IArchiveProviderPlugin.class);
        for (Map.Entry<String, IArchiveProviderPlugin> archiveProvider : safe(archiveProviderBeans).entrySet()) {
            try {
                indexArchives(archiveProvider.getValue().getArchives());
            } catch (PluginArchiveException e) {
                log.error("Fail to upload archive from plugin " + managedPlugin.getPlugin().getId() + "/" + archiveProvider.getKey()
                        + ". The plugin will not be enabled.");
                throw e;
            }
        }
    }

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public synchronized void onPluginClosed(ManagedPlugin managedPlugin) {
        if (AuthorizationUtil.getCurrentUser() != null) {
            // Only delete archives when the unload is comming from a connected user
            Map<String, IArchiveProviderPlugin> archiveProviderBeans = managedPlugin.getPluginContext().getBeansOfType(IArchiveProviderPlugin.class);
            for (Map.Entry<String, IArchiveProviderPlugin> archiveProvider : safe(archiveProviderBeans).entrySet()) {
                try {
                    Map<Csar, List<Usage>> usages = deleteArchives(safe(archiveProvider.getValue().getArchives()));
                    for (Map.Entry<Csar, List<Usage>> usage : safe(usages).entrySet()) {
                        log.warn(
                                "Fail to delete archive " + usage.getKey().getId() + " from plugin" + managedPlugin.getPlugin().getId() + "/"
                                        + archiveProvider.getKey() + " as it is used. you should clean it up manually. " + JsonUtil.toString(usage.getValue()),
                                usage.getValue());
                    }
                } catch (PluginArchiveException e) {
                }
            }
        }
    }

    private Map<Csar, List<Usage>> deleteArchives(Collection<PluginArchive> pluginArchives) {
        Map<Csar, List<Usage>> usages = Maps.newHashMap();
        for (PluginArchive pluginArchive : safe(pluginArchives)) {
            Csar csar = pluginArchive.getArchive().getArchive();
            List<Usage> csarUsage = csarService.deleteCsarWithElements(csar);
            // TODO push delete event
            if (CollectionUtils.isNotEmpty(csarUsage)) {
                usages.put(csar, csarUsage);
            }
        }
        return usages.isEmpty() ? null : usages;
    }
}