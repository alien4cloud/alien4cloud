package alien4cloud.configuration;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.exception.MissingPlugingDescriptorFileException;
import alien4cloud.plugin.exception.PluginLoadingException;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.component.repository.CsarFileRepository;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.utils.FileUtil;

/** Load archives and plugins at bootstrap to initialize alien 4 cloud repository. */
@Slf4j
@Component
public class InitialLoader {
    private static final String ARCHIVES_FOLDER = "archives";
    private static final String PLUGINS_FOLDER = "plugins";

    @Inject
    private ArchiveUploadService csarUploadService;
    @Inject
    private PluginManager pluginManager;

    @Value("${directories.alien_init:}")
    private String alienInitDirectory;
    private Path alienPluginsInitPath;

    @PostConstruct
    public void initialLoad() throws IOException {
        // Components
        if (alienInitDirectory == null || alienInitDirectory.isEmpty()) {
            log.debug("No init directory is configured - skipping initial loading of archives and plugins.");
            return;
        }
        Path alienInitDirectoryPath = Paths.get(alienInitDirectory);
        if (!Files.exists(alienInitDirectoryPath)) {
            log.warn("Specified initial directory <{}> cannot be found - skipping initial loading of archives and plugins.", alienInitDirectoryPath.toString());
            return;
        }

        Path alienArchivesInitPath = alienInitDirectoryPath.resolve(ARCHIVES_FOLDER);
        loadArchives(alienArchivesInitPath);

        alienPluginsInitPath = alienInitDirectoryPath.resolve(PLUGINS_FOLDER);
    }

    private void loadArchives(Path rootDirectory) {
        if (!Files.exists(rootDirectory) || !Files.isDirectory(rootDirectory)) {
            log.warn("Skipping archives' initial loading: directory cannot be found {}.", rootDirectory.toString());
            return;
        }
        // this operation is performed using admin role
        SecurityContextImpl adminContext = new SecurityContextImpl();
        Set<SimpleGrantedAuthority> authorities = Sets.newHashSet();
        authorities.add(new SimpleGrantedAuthority(Role.ADMIN.name()));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("alien4cloud_init_loader", "alien4cloud_init_loader", authorities);
        adminContext.setAuthentication(auth);
        SecurityContextHolder.setContext(adminContext);
        // archives must be in zip format and placed in the actual folder
        try {
            List<Path> archives = FileUtil.listFiles(rootDirectory, ".+\\.(zip|csar)");
            Collections.sort(archives);
            for (Path archive : archives) {
                try {
                    log.debug("Initial load of archives from <{}>.", archive.toString());
                    csarUploadService.upload(archive);
                } catch (CSARVersionAlreadyExistsException e) {
                    log.debug("Skipping initial upload of archive <{}>. Archive has already been loaded.", archive.toString(), e);
                } catch (ParsingException e) {
                    log.error("Initial upload of archive <{}> has failed.", archive.toString(), e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to load initial archives", e);
        } finally {
            // clear the security context
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Load plugins from the initialiaztion plugins folder.
     */
    public void loadPlugins() {
        if (alienInitDirectory == null || alienInitDirectory.isEmpty() || alienPluginsInitPath == null) {
            log.debug("No init directory is configured - skipping initial loading of plugins.");
            return;
        }

        if (!Files.exists(alienPluginsInitPath) || !Files.isDirectory(alienPluginsInitPath)) {
            log.warn("Skipping plugins' initial loading: directory cannot be found {}.", alienPluginsInitPath.toString());
            return;
        }

        try {
            List<Path> plugins = FileUtil.listFiles(alienPluginsInitPath, ".+\\.(zip)");
            for (Path plugin : plugins) {
                try {
                    pluginManager.uploadPlugin(plugin);
                } catch (AlreadyExistException e) {
                    log.debug("Plugin {} has already been uploaded.", plugin.toString());
                } catch (PluginLoadingException | MissingPlugingDescriptorFileException e) {
                    log.error("Failed to load plugin from init folder. File {}", plugin.toString(), e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to load initial archives", e);
        }
    }
}
