package alien4cloud.it.provider.util;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.keystone.v2_0.config.CredentialTypes;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.FloatingIP;
import org.jclouds.openstack.neutron.v2.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;

import alien4cloud.exception.InvalidArgumentException;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Module;

public class OpenStackClient {

    private CinderApi cinderApi;

    private NovaApi novaApi;

    private NeutronApi neutronApi;

    private VolumeApi volumeApi;

    private ServerApi serverApi;

    private FloatingIPApi floatingIPApi;

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
    }

    public Volume getVolume(String id) {
        return this.volumeApi.get(id);
    }

    public boolean deleteVolume(String id) {
        return this.volumeApi.delete(id);
    }

    public List<Server> listServers() {
        List<Server> servers = Lists.newArrayList(this.serverApi.listInDetail().concat());
        return servers;
    }

    public Server findServerByName(String name) {
        for (Server server : this.serverApi.listInDetail().concat()) {
            if (server.getName().equals(name)) {
                return server;
            }
        }
        return null;
    }

    public List<FloatingIP> listFloatingIPs() {
        List<FloatingIP> ips = Lists.newArrayList(this.floatingIPApi.list().concat());
        return ips;
    }

    public FloatingIP getServerFloatingIP(Server server) {
        List<FloatingIP> ips = listFloatingIPs();
        Map<String, FloatingIP> floatingIPs = Maps.newHashMap();
        for (FloatingIP ip : ips) {
            floatingIPs.put(ip.getFloatingIpAddress(), ip);
        }
        for (Address address : server.getAddresses().values()) {
            if (floatingIPs.containsKey(address.getAddr())) {
                return floatingIPs.get(address.getAddr());
            }
        }
        return null;
    }
}
