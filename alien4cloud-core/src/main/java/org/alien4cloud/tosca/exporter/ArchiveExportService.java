package org.alien4cloud.tosca.exporter;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationService;
import alien4cloud.model.application.Application;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.User;
import alien4cloud.tosca.serializer.VelocityUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Tosca exporter contains methods to generate TOSCA from alien indexed model.
 */
@Service
@Slf4j
public class ArchiveExportService {
    @Inject
    private ApplicationService applicationService;

    /**
     * Get the yaml string out of a cloud service archive and topology.
     *
     * @param csar The csar that contains archive meta-data.
     * @param topology The topology template within the archive.
     * @return The TOSCA yaml file that describe the topology.
     */
    public String getYaml(Csar csar, Topology topology) {
        Map<String, Object> velocityCtx = new HashMap<>();
        velocityCtx.put("topology", topology);
        velocityCtx.put("template_name", csar.getName());
        velocityCtx.put("template_version", csar.getVersion());
        if(csar.getDescription() == null) {
            velocityCtx.put("template_description", "");
        } else {
            velocityCtx.put("template_description", csar.getDescription());
        }
        User loggedUser = AuthorizationUtil.getCurrentUser();
        String author = csar.getTemplateAuthor();
        if (author == null) {
            author = loggedUser != null ? loggedUser.getUsername() : null;
        }
        velocityCtx.put("template_author", author);

        velocityCtx.put("topology_description", topology.getDescription());

        if (topology.getDescription() == null && ArchiveDelegateType.APPLICATION.toString().equals(csar.getDelegateType())) {
            // if the archive has no description let's use the one of the application
            Application application = applicationService.getOrFail(csar.getDelegateId());
            velocityCtx.put("topology_description", application.getDescription());
        }

        try {
            StringWriter writer = new StringWriter();
            VelocityUtil.generate("org/alien4cloud/tosca/exporter/topology-alien_dsl_1_3_0.yml.vm", writer, velocityCtx);
            return writer.toString();
        } catch (Exception e) {
            log.error("Exception while templating YAML for topology " + topology.getId(), e);
            return ExceptionUtils.getFullStackTrace(e);
        }
    }
}