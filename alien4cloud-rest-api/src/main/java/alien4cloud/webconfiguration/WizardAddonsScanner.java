package alien4cloud.webconfiguration;

import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.rest.wizard.model.WizardAddon;
import alien4cloud.security.model.Role;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scan all wizard_addon.json found in /wizard_addons war folder in order to define a list of wizard addons.
 * Addons can be added to wizard and appear in wizard home page.
 */
@Slf4j
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "wizard_addons", ignoreInvalidFields = true, ignoreUnknownFields = true)
public class WizardAddonsScanner {

    @Getter
    private Map<String, WizardAddon> addons;

    @Setter
    @Getter
    private String[] disabled;

    @PostConstruct
    private void initAddons() {
        this.addons = Maps.newHashMap();
        Set<String> disabledAddons = (disabled != null) ? Sets.newHashSet(disabled) : Sets.newHashSet();
        log.info("Following addons will be disabled : {}", disabledAddons);

        Pattern addonPathDetectionPattern = Pattern.compile(".*\\/wizard_addons\\/(.+)\\/wizard_addon\\.json");
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            log.info("Exploring wizard_addons");
            Resource[] resources = resolver.getResources("wizard_addons/*/wizard_addon.json");
            for (Resource resource : resources) {
                log.info("Wizard addon detected in resource {}", resource.getURL());
                WizardAddon addon = null;

                try {
                    // parse the file to get the addon description
                    addon = JsonUtil.readObject(resource.getInputStream(), WizardAddon.class);

                    // The context path is defined from the resource URL, it's the parent folder of the wizard_addon.json file
                    Matcher m = addonPathDetectionPattern.matcher(resource.getURL().toString());
                    if (m.matches() && m.group(1) != null && StringUtils.hasText(m.group(1))) {
                        String contextPath = m.group(1);
                        addon.setContextPath(contextPath);
                        addon.setDisabled(disabledAddons.contains(addon.getId()));
                        populateRoles(addon);
                        this.addons.put(contextPath, addon);
                        log.info("Wizard addon with id <{}> added for context path <{}>", addon.getId(), addon.getContextPath());
                    } else {
                        log.warn("Not able to define wizard addon context path from resource {}, addon is ignored !", resource.getURL());
                    }

                } catch (IOException parseException) {
                    log.error("Not able to parse wizard_addon.json, addon will be ignored", parseException);
                }
            }
        } catch(FileNotFoundException fnfe) {
            log.info("No wizard addons found");
        } catch(IOException ioe) {
            log.error("Exception while exploring wizard_addons folder in classpath", ioe);
        }
    }

    /**
     * Parse roles names and add them as Role[] to addon.
     */
    private void populateRoles(WizardAddon addon) {
        Set<Role> rolesSet = Sets.newHashSet();
        for (String roleName : addon.getRoles()) {
            Role role = Role.valueOf(roleName);
            if (role != null) {
                rolesSet.add(role);
            } else {
                log.warn("Role {} has not be recognized for addon {}, please review it's wizard_addon.json", roleName, addon.getContextPath());
            }
        }
        Role[] roles = new Role[rolesSet.size()];
        roles = rolesSet.toArray(roles);
        addon.setAuthorizedRoles(roles);
    }
}
