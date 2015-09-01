package alien4cloud.model.cloud;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum IaaSType {
    AZURE("azure"), OPENSTACK("openstack"), VMWARE("vmware"), AMAZON("amazon"), VIRTUALBOX("virtualbox"), BYON("byon"), OTHER("other");

    private String name;

    @Override
    public String toString() {
        return name;
    }
}