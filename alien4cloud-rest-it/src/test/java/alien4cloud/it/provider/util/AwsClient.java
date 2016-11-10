package alien4cloud.it.provider.util;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.AWSEC2Api;
import org.jclouds.ec2.domain.Reservation;
import org.jclouds.ec2.domain.RunningInstance;
import org.jclouds.ec2.domain.Volume;
import org.jclouds.ec2.features.ElasticBlockStoreApi;
import org.jclouds.ec2.features.InstanceApi;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.rest.ResourceNotFoundException;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

import alien4cloud.exception.NotFoundException;

public class AwsClient {

    private ElasticBlockStoreApi blockStoreApi;

    private InstanceApi instanceApi;

    public AwsClient() {
        String accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_KEY");
        String region = System.getenv("AWS_REGION");
        if (StringUtils.isBlank(accessKeyId) || StringUtils.isBlank(secretKey) || StringUtils.isBlank(region)) {
            throw new NotFoundException("Aws Client need 3 environment variables set : AWS_ACCESS_KEY_ID, AWS_SECRET_KEY, AWS_REGION");
        }
        AWSEC2Api ec2Api = ContextBuilder.newBuilder("aws-ec2").credentials(accessKeyId, secretKey)
                .modules(ImmutableSet.<Module> of(new SLF4JLoggingModule(), new SshjSshClientModule())).buildApi(AWSEC2Api.class);
        blockStoreApi = ec2Api.getElasticBlockStoreApiForRegion(region).get();
        instanceApi = ec2Api.getInstanceApiForRegion(region).get();
    }

    public Reservation<? extends RunningInstance> getCompute(String id) {
        Set<? extends Reservation<? extends RunningInstance>> instances = instanceApi.describeInstancesInRegion(null, id);
        if (!instances.isEmpty()) {
            return instances.iterator().next();
        } else {
            return null;
        }
    }

    public Volume getVolume(String id) {
        try {
            Set<Volume> volumes = blockStoreApi.describeVolumesInRegion(null, id);
            if (!volumes.isEmpty()) {
                return volumes.iterator().next();
            } else {
                return null;
            }
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    public void deleteVolume(String id) {
        blockStoreApi.deleteVolumeInRegion(null, id);
    }

    public void deleteCompute(String id) {
        instanceApi.terminateInstancesInRegion(null, id);
    }
}
