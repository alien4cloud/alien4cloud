package alien4cloud.tosca.serializer;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * A velocity Util class
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VelocityUtil {

    private static final VelocityEngine VELOCITY_ENGINE;

    static {
        // ThreadSafe so can be shared
        VELOCITY_ENGINE = new VelocityEngine();
        VELOCITY_ENGINE.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        // here we use a classpath resource loader
        VELOCITY_ENGINE.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        // we want template resources to be cached
        VELOCITY_ENGINE.setProperty("classpath.resource.loader.cache", true);
        VELOCITY_ENGINE.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
        VELOCITY_ENGINE.init();
    }

    public static void generate(String path, Writer outputWriter, Map<String, ?> properties) throws IOException {
        Template template = VELOCITY_ENGINE.getTemplate(path, "UTF-8");
        VelocityContext context = new VelocityContext();

        for (Entry<String, ?> contextEntry : properties.entrySet()) {
            context.put(contextEntry.getKey(), contextEntry.getValue());
        }
        putIfAbsent(context, "utils", new ToscaSerializerUtils());
        putIfAbsent(context, "propertyUtils", new ToscaPropertySerializerUtils());
        putIfAbsent(context, "importsUtils", new ToscaImportsUtils());

        try {
            template.merge(context, outputWriter);
        } finally {
            outputWriter.close();
        }
    }

    private static void putIfAbsent(VelocityContext context, String key, Object value) {
        if (!context.containsKey(key)) {
            context.put(key, value);
        }
    }
}
