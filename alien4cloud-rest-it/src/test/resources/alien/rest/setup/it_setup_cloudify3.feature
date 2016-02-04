Feature: This is not a test, it reuses integration test step to set up Alien with all data

  Scenario: Setup Alien
    Given I am authenticated with "ADMIN" role

    # Archives
	And I checkout the git archive from url "https://github.com/alien4cloud/tosca-normative-types.git" branch "master"
    And I upload the git archive "tosca-normative-types"
    And I checkout the git archive from url "https://github.com/alien4cloud/alien4cloud-extended-types.git" branch "master"
    And I upload the git archive "alien4cloud-extended-types/alien-base-types-1.0-SNAPSHOT"
    And I upload the git archive "alien4cloud-extended-types/alien-extended-storage-types-1.0-SNAPSHOT"
    And I checkout the git archive from url "https://github.com/alien4cloud/samples.git" branch "master"
    And I upload the git archive "samples/apache-load-balancer"
    And I upload the git archive "samples/tomcat-war"
    And I upload the git archive "samples/apache"
    And I upload the git archive "samples/mysql"
    And I upload the git archive "samples/php"
    And I upload the git archive "samples/wordpress"
    And I upload the git archive "samples/topology-wordpress"
    And I upload the git archive "samples/topology-load-balancer-tomcat"

    # Cloudify 3
   # And I upload a plugin from maven artifact "alien4cloud:alien4cloud-cloudify3-provider"
    And I upload a plugin "alien4cloud-cloudify3-provider" from "../../a4c-cdfy3"

#    # Orchestrator and location amazon
#    And I create an orchestrator named "Cloudify3-orchestrator" and plugin name "alien-cloudify-3-orchestrator" and bean name "cloudify-orchestrator"
#    And I get configuration for orchestrator "Cloudify3-orchestrator"
#    And I update cloudify 3 manager's url to value defined in environment variable "AWS_CLOUDIFY3_MANAGER_URL" for orchestrator with name "Cloudify3-orchestrator"
#    And I enable the orchestrator "Cloudify3-orchestrator"
#    And I create a location named "Amazon" and infrastructure type "amazon" to the orchestrator "Cloudify3-orchestrator"
#    And I create a resource of type "alien.cloudify.aws.nodes.InstanceType" named "Small" related to the location "Cloudify3-orchestrator"/"Amazon"
#    And I update the property "id" to "t2.small" for the resource named "Small" related to the location "Cloudify3-orchestrator"/"Amazon"
#    And I create a resource of type "alien.cloudify.aws.nodes.Image" named "Ubuntu" related to the location "Cloudify3-orchestrator"/"Amazon"
#    And I update the property "id" to "ami-47a23a30" for the resource named "Ubuntu" related to the location "Cloudify3-orchestrator"/"Amazon"
#    And I autogenerate the on-demand resources for the location "Cloudify3-orchestrator"/"Amazon"
#    And I create a resource of type "alien.nodes.aws.PublicNetwork" named "Internet" related to the location "Cloudify3-orchestrator"/"Amazon"
#
    # Orchestrator and location openstack
    And I create an orchestrator named "Cloudify3-orchestrator-openstack" and plugin name "alien-cloudify-3-orchestrator" and bean name "cloudify-orchestrator"
    And I get configuration for orchestrator "Cloudify3-orchestrator-openstack"
    And I update cloudify 3 manager's url to value defined in environment variable "OPENSTACK_CLOUDIFY3_MANAGER_URL" for orchestrator with name "Cloudify3-orchestrator-openstack"
    And I enable the orchestrator "Cloudify3-orchestrator-openstack"
    And I create a location named "OpenStack" and infrastructure type "openstack" to the orchestrator "Cloudify3-orchestrator-openstack"
    And I create a resource of type "alien.nodes.openstack.Flavor" named "Small" related to the location "Cloudify3-orchestrator-openstack"/"OpenStack"
    And I update the property "id" to "2" for the resource named "Small" related to the location "Cloudify3-orchestrator-openstack"/"OpenStack"
    And I create a resource of type "alien.nodes.openstack.Image" named "Ubuntu" related to the location "Cloudify3-orchestrator-openstack"/"OpenStack"
    And I update the property "id" to "02ddfcbb-9534-44d7-974d-5cfd36dfbcab" for the resource named "Ubuntu" related to the location "Cloudify3-orchestrator-openstack"/"OpenStack"
    And I autogenerate the on-demand resources for the location "Cloudify3-orchestrator-openstack"/"OpenStack"
    And I create a resource of type "alien.nodes.openstack.PublicNetwork" named "Internet" related to the location "Cloudify3-orchestrator-openstack"/"OpenStack"
    And I update the complex property "floatingip" to """{"floating_network_name": "net-pub"}""" for the resource named "Internet" related to the location "Cloudify3-orchestrator-openstack"/"OpenStack"
    And I update the complex property "server" to """{"security_groups": ["openbar"]}""" for the resource named "Small_Ubuntu" related to the location "Cloudify3-orchestrator-openstack"/"OpenStack"

    And I create a new application with name "wordpress-cfy3" and description "Wordpress with CFY 3" based on the template with name "wordpress-template"
