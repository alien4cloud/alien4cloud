package alien4cloud.rest.health;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import lombok.extern.slf4j.Slf4j;

import com.orbitz.consul.Consul;
import com.orbitz.consul.Consul.Builder;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.health.HealthCheck;
import com.orbitz.consul.model.health.Node;
import com.orbitz.consul.model.health.Service;

@Slf4j
@Deprecated
public class ConsulSandbox2 {

    public static void main(String[] args) throws Exception {

        Builder consulBuilder = Consul.builder();
        Consul consul = null;
        // if (args.length == 2) {
        // String consulIp = args[0];
        // int consulPort = Integer.valueOf(args[1]);
        // consulBuilder = consulBuilder.withHostAndPort(HostAndPort.fromParts(consulIp, consulPort));
        // }

        try {
            KeyManager[] kms = null;

            SSLContext sslContext = SSLContext.getInstance("TLSv1");

            // -Djavax.net.ssl.trustStore=/home/developer/consul/stores/server-truststore.jks -Djavax.net.ssl.trustStorePassword=changeit
            // -Djavax.net.ssl.keyStore=/home/developer/consul/stores/client-keystore.jks -Djavax.net.ssl.keyStorePassword=changeIt
            // -Djavax.net.ssl.keyStoreType=JKS
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(new File("/home/developer/consul/stores/client-keystore.jks")), "changeIt".toCharArray());

            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "changeIt".toCharArray());
            
            final KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(new FileInputStream(new File("/home/developer/consul/stores/server-truststore.jks")), "changeIt".toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            consulBuilder = consulBuilder.withUrl("https://127.0.0.1:8500").withSslContext(sslContext);
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        consul = consulBuilder.build();
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
