package alien4cloud.orchestrators.plugin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.tosca.model.ArchiveRoot;

import java.nio.file.Path;

/**
 * Represents the TOSCA archive wrapper that a plugin can provide to A4C when a configuration is created.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class PluginArchive {
    /** The TOSCA archive - it may be parsed from a file or just generated from the plugin code. */
    private ArchiveRoot archive;
    /**
     * Optional path to the archive path that was used to load the TOSCA archive.
     *
     * In case it exists Alien will use it to save the data of the archive so it is available for browsing as well as for importing icons.
     */
    private Path archiveFilePath;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PluginArchive) {
            return archive.getArchive().equals(((PluginArchive) obj).archive.getArchive());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return archive.getArchive().hashCode();
    }
}
