package alien4cloud.plugin;

import java.nio.file.Path;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@AllArgsConstructor
@Getter
public class ManagedPlugin {
    private AnnotationConfigApplicationContext pluginContext;
    private PluginDescriptor descriptor;
    private Path pluginPath;
}
