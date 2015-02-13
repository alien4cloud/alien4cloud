Feature: Match topology's compute template to cloud resources.

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I enable the cloud "Mount doom cloud"
  And I upload the archive "tosca base types 1.0"
  And There are these users in the system
    | sangoku |
  And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
  And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "Mount doom cloud"
  And I have already created a cloud image with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows" and version "7.0"
  And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"
  And I add the cloud image "Windows 7" to the cloud "Mount doom cloud"
  And I add the cloud image "Ubuntu Trusty" to the cloud "Mount doom cloud"
  And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Mount doom cloud"
  And I add the flavor with name "medium", number of CPUs 4, disk size 64 and memory size 4096 to the cloud "Mount doom cloud"
  And I match the image "Windows 7" of the cloud "Mount doom cloud" to the PaaS resource "WINDOWS"
  And I match the image "Ubuntu Trusty" of the cloud "Mount doom cloud" to the PaaS resource "UBUNTU"
  And I match the flavor "small" of the cloud "Mount doom cloud" to the PaaS resource "2"
  And I match the flavor "medium" of the cloud "Mount doom cloud" to the PaaS resource "3"
  And I am authenticated with user named "sangoku"
  And I have an application "ALIEN" with a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0"
  And I add a node template "Java" related to the "fastconnect.nodes.JavaChef:1.0" node type
  And I assign the cloud with name "Mount doom cloud" for the application

Scenario: Match a topology for computes, compute properties empty
  When I match for resources for my application on the cloud
  Then I should receive a match result with 4 compute templates for the node "Compute":
    | Windows 7     | small  |
    | Windows 7     | medium |
    | Ubuntu Trusty | small  |
    | Ubuntu Trusty | medium |
  And The match result should contain 2 cloud images:
    | Windows 7     | x86_64 | windows | Windows | 7.0     |
    | Ubuntu Trusty | x86_64 | linux   | Ubuntu  | 14.04.1 |
  And The match result should contain 2 flavors:
    | small  | 2 | 32 | 2048 |
    | medium | 4 | 64 | 4096 |

Scenario: Match a topology for computes, with filters
# Update os type to linux --> only linux available
  Given I update the node template "Compute"'s property "os_type" to "linux"
  When I match for resources for my application on the cloud
  Then I should receive a match result with 2 compute templates for the node "Compute":
    | Ubuntu Trusty | small  |
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 2 flavors:
    | small  | 2 | 32 | 2048 |
    | medium | 4 | 64 | 4096 |

    # Update to os arch 32 bits --> empty result
  When I update the node template "Compute"'s property "os_arch" to "x86_32"
  And I match for resources for my application on the cloud
  Then I should receive an empty match result

  # Update to os arch 64 bits --> result available again
  When I update the node template "Compute"'s property "os_arch" to "x86_64"
  And I match for resources for my application on the cloud
  Then I should receive a match result with 2 compute templates for the node "Compute":
    | Ubuntu Trusty | small  |
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 2 flavors:
    | small  | 2 | 32 | 2048 |
    | medium | 4 | 64 | 4096 |

    # Update to os distribution windows --> empty result
  When I update the node template "Compute"'s property "os_distribution" to "Windows"
  And I match for resources for my application on the cloud
  Then I should receive an empty match result

  # Update to os distribution linux --> result available again
  When I update the node template "Compute"'s property "os_distribution" to "Ubuntu"
  And I match for resources for my application on the cloud
  Then I should receive a match result with 2 compute templates for the node "Compute":
    | Ubuntu Trusty | small  |
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 2 flavors:
    | small  | 2 | 32 | 2048 |
    | medium | 4 | 64 | 4096 |

    # Update to os version 15.0 --> empty result
  When I update the node template "Compute"'s property "os_version" to "15.0"
  And I match for resources for my application on the cloud
  Then I should receive an empty match result

  # Update to os version 13.0 --> result available again
  When I update the node template "Compute"'s property "os_version" to "13.0"
  And I match for resources for my application on the cloud
  Then I should receive a match result with 2 compute templates for the node "Compute":
    | Ubuntu Trusty | small  |
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 2 flavors:
    | small  | 2 | 32 | 2048 |
    | medium | 4 | 64 | 4096 |

    # Update to disk size 33 make Alien drop the small flavor
  When I update the node template "Compute"'s property "disk_size" to "33"
  And I match for resources for my application on the cloud
  Then I should receive a match result with 1 compute templates for the node "Compute":
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 1 flavors:
    | medium | 4 | 64 | 4096 |

    # Update to disk size 32 make small flavor available again in matching result
  When I update the node template "Compute"'s property "disk_size" to "32"
  And I match for resources for my application on the cloud
  Then I should receive a match result with 2 compute templates for the node "Compute":
    | Ubuntu Trusty | small  |
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 2 flavors:
    | small  | 2 | 32 | 2048 |
    | medium | 4 | 64 | 4096 |

    # Update to num CPUs 3 make Alien drop the small flavor
  When I update the node template "Compute"'s property "num_cpus" to "3"
  And I match for resources for my application on the cloud
  Then I should receive a match result with 1 compute templates for the node "Compute":
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 1 flavors:
    | medium | 4 | 64 | 4096 |

    # Update to num CPUs 2 make small flavor available again in matching result
  When I update the node template "Compute"'s property "num_cpus" to "2"
  And I match for resources for my application on the cloud
  Then I should receive a match result with 2 compute templates for the node "Compute":
    | Ubuntu Trusty | small  |
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 2 flavors:
    | small  | 2 | 32 | 2048 |
    | medium | 4 | 64 | 4096 |

    # Update to memory 3000 make Alien drop the small flavor
  When I update the node template "Compute"'s property "mem_size" to "3000"
  And I match for resources for my application on the cloud
  Then I should receive a match result with 1 compute templates for the node "Compute":
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 1 flavors:
    | medium | 4 | 64 | 4096 |

    # Update to memory 2000 make small flavor available again in matching result
  When I update the node template "Compute"'s property "mem_size" to "2000"
  And I match for resources for my application on the cloud
  Then I should receive a match result with 2 compute templates for the node "Compute":
    | Ubuntu Trusty | small  |
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 2 flavors:
    | small  | 2 | 32 | 2048 |
    | medium | 4 | 64 | 4096 |

    # Update memory size too big --> empty result
  When I update the node template "Compute"'s property "mem_size" to "9999999999"
  And I match for resources for my application on the cloud
  Then I should receive an empty match result

Scenario: Find a matching resource for a type derived from Compute
  Given I am authenticated with "ADMIN" role
  And I upload the archive "ubuntu types 0.1"
  And I am authenticated with user named "sangoku"
  And I have an application "ALIEN_2" with a topology containing a nodeTemplate "Ubuntu" related to "alien.nodes.Ubuntu:0.1"
  And I assign the cloud with name "Mount doom cloud" for the application
  When I match for resources for my application on the cloud
  Then I should receive a match result with 2 compute templates for the node "Ubuntu":
    | Ubuntu Trusty | small  |
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 2 flavors:
    | small  | 2 | 32 | 2048 |
    | medium | 4 | 64 | 4096 |

  When I update the node template "Ubuntu"'s property "mem_size" to "3000"
  And I match for resources for my application on the cloud
  Then I should receive a match result with 1 compute templates for the node "Ubuntu":
    | Ubuntu Trusty | medium |
  And The match result should contain 1 cloud images:
    | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |
  And The match result should contain 1 flavors:
    | medium | 4 | 64 | 4096 |
