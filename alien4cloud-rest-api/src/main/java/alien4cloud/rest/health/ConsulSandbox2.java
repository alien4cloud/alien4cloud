package alien4cloud.rest.health;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.health.HealthCheck;
import com.orbitz.consul.model.health.Node;
import com.orbitz.consul.model.health.Service;

@Slf4j
public class ConsulSandbox2 {

    public static void main(String[] args) {

        Consul consul = null;
        if (args.length == 2) {
            String consulIp = args[0];
            int consulPort = Integer.valueOf(args[1]);
            consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(consulIp, consulPort)).build();
        }

        ConsulResponse<Map<String, List<String>>> consulResponse = consul.catalogClient().getServices();
        Map<String, List<String>> response = consulResponse.getResponse();
        System.out.println(response);

        ConsulResponse<List<Node>> nodeConsulResponse = consul.catalogClient().getNodes();
        List<Node> nodes = nodeConsulResponse.getResponse();
        for (Node node : nodes) {
            log.info("Node found <{}> with address {}", node.getNode(), node.getAddress());
        }

        Map<String, Service> services = consul.agentClient().getServices();
        log.info("Services count : {}", services.size());
        for (Entry<String, Service> serviceEntry : services.entrySet()) {
            log.info("Service found with key <{}> with id <{}>, service <{}>, address <{}>, port <{}>", serviceEntry.getKey(), serviceEntry.getValue().getId(),
                    serviceEntry.getValue().getService(), serviceEntry.getValue().getAddress(), serviceEntry.getValue().getPort());
        }

        Map<String, HealthCheck> checks = consul.agentClient().getChecks();
        log.info("Checks count : {}", checks.size());
        for (Entry<String, HealthCheck> checkEntry : checks.entrySet()) {
            log.info("Check found with key <{}> with id <{}>, serviceName <{}> status <{}>", checkEntry.getKey(), checkEntry.getValue().getCheckId(),
                    checkEntry.getValue().getServiceName(), checkEntry.getValue().getStatus());
        }
    }

}
