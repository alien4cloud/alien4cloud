package alien4cloud.it.provider.util;

import alien4cloud.exception.InvalidArgumentException;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Module;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.keystone.v2_0.config.CredentialTypes;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.extensions.FloatingIPApi;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;

public class OpenStackClient {

    private CinderApi cinderApi;

    private NovaApi novaApi;

    private NeutronApi neutronApi;

    private VolumeApi volumeApi;

    private ServerApi serverApi;

    private FloatingIPApi floatingIPApi;

    private org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi novaFloatingIPApi;

    private NetworkApi networkApi;

    public OpenStackClient(String user, String password, String tenant, String url, String region) {
        Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());
        Properties overrides = new Properties();
        overrides.setProperty(KeystoneProperties.CREDENTIAL_TYPE, CredentialTypes.PASSWORD_CREDENTIALS);
        overrides.setProperty(Constants.PROPERTY_API_VERSION, "2");
        this.cinderApi = ContextBuilder.newBuilder("openstack-cinder").endpoint(url).credentials(tenant + ":" + user, password).modules(modules)
                .overrides(overrides).buildApi(CinderApi.class);
        if (!this.cinderApi.getConfiguredRegions().contains(region)) {
            throw new InvalidArgumentException("Region " + region + " do not exist, available regions are " + cinderApi.getConfiguredRegions());
        }
        this.volumeApi = this.cinderApi.getVolumeApi(region);
        this.novaApi = ContextBuilder.newBuilder("openstack-nova").endpoint(url).credentials(tenant + ":" + user, password).modules(modules)
                .overrides(overrides).buildApi(NovaApi.class);
        if (!this.novaApi.getConfiguredRegions().contains(region)) {
            throw new InvalidArgumentException("Region " + region + " do not exist, available regions are " + novaApi.getConfiguredRegions());
        }
        this.serverApi = this.novaApi.getServerApi(region);
        this.neutronApi = ContextBuilder.newBuilder("openstack-neutron").endpoint(url).credentials(tenant + ":" + user, password).modules(modules)
                .overrides(overrides).buildApi(NeutronApi.class);
        if (!this.neutronApi.getConfiguredRegions().contains(region)) {
            throw new InvalidArgumentException("Region " + region + " do not exist, available regions are " + neutronApi.getConfiguredRegions());
        }
        this.floatingIPApi = this.neutronApi.getFloatingIPApi(region).get();
        this.novaFloatingIPApi = this.novaApi.getFloatingIPApi(region).get();
        this.networkApi = this.neutronApi.getNetworkApi(region);
    }

    public Volume getVolume(String id) {
        return this.volumeApi.get(id);
    }

    public boolean deleteVolume(String id) {
        return this.volumeApi.delete(id);
    }

    private List<Server> listServers() {
        List<Server> servers = Lists.newArrayList(this.serverApi.listInDetail().concat());
        return servers;
    }

    public Server findServerByIp(String ip) {
        for (Server server : listServers()) {
            for (Address address : server.getAddresses().values()) {
                if (Objects.equals(address.getAddr(), ip)) {
                    return server;
                }
            }
        }
        return null;
    }

    public Server getServer(String serverId) {
        return serverApi.get(serverId);
    }

    public boolean deleteCompute(String computeId) {
        return serverApi.delete(computeId);
    }

    public ServerCreated create(String name, String imageRef, String flavorRef, CreateServerOptions... options) {
        return serverApi.create(name, imageRef, flavorRef, options);
    }

    public Set<String> getServerNetworksNames(Server server) {
        if (server != null) {
            if (server.getAddresses() != null) {
                return server.getAddresses().keySet();
            }
        }
        return null;
    }

    public Network findNetworkByName(String name) {
        FluentIterable<Network> networks = networkApi.list().concat();
        for (Network network : networks) {
            if (Objects.equals(network.getName(), name)) {
                return network;
            }
        }
        return null;
    }

    public org.jclouds.openstack.nova.v2_0.domain.FloatingIP associateFloationgIpToServer(String serverId, String poolName) {
        org.jclouds.openstack.nova.v2_0.domain.FloatingIP floatingIp = novaFloatingIPApi.allocateFromPool(poolName);
        novaFloatingIPApi.addToServer(floatingIp.getIp(), serverId);
        return floatingIp;
    }

    public void deleteFloatingIp(String id) {
        floatingIPApi.delete(id);
    }
}
