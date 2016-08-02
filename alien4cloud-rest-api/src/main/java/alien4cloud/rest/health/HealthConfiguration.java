package alien4cloud.rest.health;

import java.io.File;
import java.nio.file.Paths;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicator;
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicatorProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class HealthConfiguration {

    @Resource
    HealthAggregator healthAggregator;

    @Bean
    public HealthIndicator healthIndicator(ElasticSearchClient esClient, @Value("${directories.alien}") String path,
            @Value("${ha.health_disk_space_threshold:#{10 * 1024 * 1024}}") Long threshold) {

        HealthIndicator diskHealthIndicator = createDiskHealthIndicator(path, threshold);
        HealthIndicator esHealthIndicator = createESHealthIndicator(esClient);

        CompositeHealthIndicator healthIndicator = new CompositeHealthIndicator(healthAggregator);
        healthIndicator.addHealthIndicator("diskHealthIndicator", diskHealthIndicator);
        healthIndicator.addHealthIndicator("elasticsearchHealthIndicator", esHealthIndicator);

        return healthIndicator;

    }

    private ElasticsearchHealthIndicator createESHealthIndicator(ElasticSearchClient esClient) {
        return new ElasticsearchHealthIndicator(esClient.getClient());
    }

    private HealthIndicator createDiskHealthIndicator(String path, Long threshold) {
        DiskSpaceHealthIndicatorProperties properties = new DiskSpaceHealthIndicatorProperties();
        File pathAsFile = Paths.get(path).toFile();
        properties.setPath(pathAsFile);
        properties.setThreshold(threshold);

        CompositeHealthIndicator indicator = new CompositeHealthIndicator(this.healthAggregator);
        indicator.addHealthIndicator("diskSpaceHealthIndicator", new DiskSpaceHealthIndicator(properties));
        indicator.addHealthIndicator("diskAccessHealthIndicator", new DiskAccessHealthIndicator(pathAsFile));

        return indicator;
    }

    private class DiskAccessHealthIndicator extends AbstractHealthIndicator {
        File path;

        public DiskAccessHealthIndicator(File path) {
            this.path = path;
        }

        @Override
        protected void doHealthCheck(Builder builder) throws Exception {
            if (this.path.canWrite()) {
                builder.up();
            } else {
                log.warn(String.format("Path %s cannot be written on .", this.path));
                builder.down();
            }
            log.info(builder.toString());
            builder.withDetail("canRead", path.canRead()).withDetail("canWrite", path.canWrite());
        }
    }

    /**
     * This class is copied from spring boot 1.4.0, since we are using 1.2.1 version
     * FIXME: remove it when upgrading our spring boot dependency
     */
    private class ElasticsearchHealthIndicator extends AbstractHealthIndicator {

        private final String[] allIndices = { "_all" };

        private final Client client;
        private long responseTimeout = 100L;

        public ElasticsearchHealthIndicator(Client client) {
            this.client = client;
        }

        @Override
        protected void doHealthCheck(Health.Builder builder) throws Exception {
            ClusterHealthResponse response = this.client.admin().cluster().health(Requests.clusterHealthRequest(allIndices)).actionGet(responseTimeout);

            switch (response.getStatus()) {
            case GREEN:
            case YELLOW:
                builder.up();
                break;
            case RED:
            default:
                builder.down();
                break;
            }
            builder.withDetail("clusterName", response.getClusterName());
            builder.withDetail("numberOfNodes", response.getNumberOfNodes());
            builder.withDetail("numberOfDataNodes", response.getNumberOfDataNodes());
            builder.withDetail("activePrimaryShards", response.getActivePrimaryShards());
            builder.withDetail("activeShards", response.getActiveShards());
            builder.withDetail("relocatingShards", response.getRelocatingShards());
            builder.withDetail("initializingShards", response.getInitializingShards());
            builder.withDetail("unassignedShards", response.getUnassignedShards());
        }
    }
}
