package org.alien4cloud.tosca.normative.constants;

/**
 * Constants for tosca.nodes.Compute normative type.
 */
public final class NormativeComputeConstants {
    public static final String COMPUTE_TYPE = "tosca.nodes.Compute";

    public static final String HOST_CAPABILITY = "host";
    public static final String NUM_CPUS = "num_cpus";
    public static final String DISK_SIZE = "disk_size";
    public static final String MEM_SIZE = "mem_size";

    public static final String OS_CAPABILITY = "os";
    public static final String OS_ARCH = "arch";
    public static final String OS_TYPE = "type";
    public static final String OS_DISTRIBUTION = "distribution";
    public static final String OS_VERSION = "version";

    public static final String IP_ADDRESS = "ip_address";

    public static final String SCALABLE = "scalable";
    public static final String SCALABLE_MIN_INSTANCES = "min_instances";
    public static final String SCALABLE_MAX_INSTANCES = "max_instances";
    public static final String SCALABLE_DEFAULT_INSTANCES = "default_instances";

    private NormativeComputeConstants() {
    }
}