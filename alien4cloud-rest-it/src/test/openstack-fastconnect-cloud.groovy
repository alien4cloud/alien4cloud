/***************
 * Cloud configuration file for the Openstack cloud. *
 */
cloud {
    // Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
    name = "openstack"

    /********
     * General configuration information about the cloud driver implementation.
     */
    configuration {
        // Optional. The cloud implementation class. Defaults to the build in jclouds-based provisioning driver.
        className "org.cloudifysource.esc.driver.provisioning.openstack.OpenStackCloudifyDriver"
        networkDriverClassName "org.cloudifysource.esc.driver.provisioning.network.openstack.OpenstackNetworkDriver"

        // Optional. The template name for the management machines. Defaults to the first template in the templates section below.
        managementMachineTemplate "ubuntu-12.04-cfy-networks-jdk"
        // Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
        connectToPrivateIp true
        bootstrapManagementOnPublicIp true

        //for storage
        storageClassName  "org.cloudifysource.esc.driver.provisioning.storage.openstack.OpenstackStorageDriver"
    }

    /*************
     * Provider specific information.
     */
    provider {
        // Mandatory. The name of the provider.
        // When using the default cloud driver, maps to the Compute Service Context provider name.
        provider "openstack-nova"


        // Optional. The HTTP/S URL where cloudify can be downloaded from by newly started machines. Defaults to downloading the
        // cloudify version matching that of the client from the cloudify CDN.
        // Change this if your compute nodes do not have access to an internet connection, or if you prefer to use a
        // different HTTP server instead.
        // IMPORTANT: the default linux bootstrap script appends '.tar.gz' to the url whereas the default windows script appends '.zip'.
        // Therefore, if setting a custom URL, make sure to leave out the suffix.

        // Mandatory. The prefix for new machines started for servies.
        machineNamePrefix "cloudify-ggdemo-agent-"
        // Optional. Defaults to true. Specifies whether cloudify should try to deploy services on the management machine.
        // Do not change this unless you know EXACTLY what you are doing.

        //
        managementOnlyFiles ([])

        // Optional. Logging level for the intenal cloud provider logger. Defaults to INFO.
        sshLoggingLevel "WARNING"

        // Mandatory. Name of the new machine/s started as cloudify management machines. Names are case-insensitive.
        managementGroup "cloudify-ggdemo-manager-"
        // Mandatory. Number of management machines to start on bootstrap-cloud. In production, should be 2. Can be 1 for dev.
        numberOfManagementMachines 1

        reservedMemoryCapacityPerMachineInMB 1024

    }

    /*************
     * Cloud authentication information
     */
    user {
        // Optional. Identity used to access cloud.
        // When used with the default driver, maps to the identity used to create the ComputeServiceContext.
        user "${tenant}:${user}"

        // Optional. Key used to access cloud.
        // When used with the default driver, maps to the credential used to create the ComputeServiceContext.
        apiKey apiKey
    }

    /********************
     * Cloud networking configuration.
     */
    cloudNetwork {
        // Details of the management network, which is shared among all instances of the Cloudify Cluster.
        management {
            networkConfiguration {
                // The network name
                name "Cloudify-Management-Network"
                // Subnets
                subnets ([
                    subnet {
                        name "Cloudify-Management-Subnet"
                        range "177.86.0.0/24"
                        options ([
                            "gateway" : "177.86.0.111",
                            "dnsNameServers" : "8.8.8.8"
                        ])
                    }
                ])
                custom ([ "associateFloatingIpOnBootstrap" : "true" ])
            }
        }
        templates ([
            "APPLICATION_NET" : networkConfiguration {
                // The network name
                name "Cloudify-Application-Network"
                // Subnets
                subnets ([
                    subnet {
                        name "Cloudify-Application-Subnet"
                        range "149.80.0.0/24"
                        options ([
                            "gateway" : "null",
                            "dnsNameServers" : "8.8.8.8"
                        ])
                    }
                ])
                custom ([ "associateFloatingIpOnBootstrap" : "true" ])
            }
        ])
    }

   /********************
		 * Cloud storage configuration.
		 */
    cloudStorage {
				templates ([
					SMALL_BLOCK : storageTemplate{
									deleteOnExit false
									partitioningRequired false
									size 1
									path "/mountedStorage"
									namePrefix "a4c-SMALL_BLOCK-storage-gauvain"
									deviceName "/dev/vdb"
									fileSystemType "ext4"
                  custom (["openstack.storage.volume.zone":availabilityZone])
					},
          MEDIUM_BLOCK : storageTemplate{
									deleteOnExit false
									partitioningRequired false
									size 2
									path "/mountedStorage"
									namePrefix "a4c-MEDIUM_BLOCK-storage-gauvain"
									deviceName "/dev/vdb"
									fileSystemType "ext4"
                  custom (["openstack.storage.volume.zone":availabilityZone])
					}
          LARGE_BLOCK : storageTemplate{
                  deleteOnExit false
                  partitioningRequired false
                  size 10
                  path "/mountedStorage"
                  namePrefix "a4c-LARGE_BLOCK-storage-gauvain"
                  deviceName "/dev/vdb"
                  fileSystemType "ext4"
                  custom (["openstack.storage.volume.zone":availabilityZone])
          }
			])
		}


    cloudCompute {

        /***********
         * Cloud machine templates available with this cloud.
         */
        templates ([
            "ubuntu-12.04-cfy-networks-jdk" : computeTemplate{
                // Mandatory. Image ID.
                imageId "RegionOne/cac5bf41-6249-4511-b0c9-a167b48b3f1d"
                // Mandatory. Files from the local directory will be copied to this directory on the remote machine.
                remoteDirectory remoteDirectory
                // Mandatory. Amount of RAM available to machine.
                machineMemoryMB 1600
                // Mandatory. Hardware ID.
                hardwareId "RegionOne/9"
                // Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
                localDirectory "upload"
                // Optional. Name of key file to use for authenticating to the remot machine. Remove this line if key files
                // are not used.
                keyFile keyFile
                // file transfer protocol
                fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
                javaUrl javaUrl

                username username
                // Additional template options.
                // When used with the default driver, the option names are considered
                // method names invoked on the TemplateOptions object with the value as the parameter.
                options ([
                    "KILL_VM" : "false", // Avoid deleting VM. For testing purpose only.
                    "skipExternalNetworking" : "false",
                    "securityGroups" : ["openbar"] as String[],
                    "keyPairName" : keyPair
                ])

                // when set to 'true', agent will automatically start after reboot.
                autoRestartAgent false

                // Optional. Overrides to default cloud driver behavior.
                // When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
                overrides ([
                    "openstack.endpoint": openstackUrl
                ])

                env ([
                    "JAVA_HOME" : "/root/java"
                ])
                privileged true
                // optional. A native command line to be executed before the cloudify agent is started.
                initializationCommand "#!/bin/sh\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts"

		custom (["openstack.compute.zone":availabilityZone])
            },
            "ubuntu_12.04-updated-source-list-3" : computeTemplate{
                // Mandatory. Image ID.
                imageId "RegionOne/e1a52797-a594-41c6-bd79-b18c4495eb6e"
                // Mandatory. Files from the local directory will be copied to this directory on the remote machine.
                remoteDirectory remoteDirectory
                // Mandatory. Amount of RAM available to machine.
                machineMemoryMB 1600
                // Mandatory. Hardware ID.
                hardwareId "RegionOne/9"
                // Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
                localDirectory "upload"
                // Optional. Name of key file to use for authenticating to the remot machine. Remove this line if key files
                // are not used.
                keyFile keyFile
                // file transfer protocol
                fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
                javaUrl javaUrl

                username username
                // Additional template options.
                // When used with the default driver, the option names are considered
                // method names invoked on the TemplateOptions object with the value as the parameter.
                options ([
                    "KILL_VM" : "false", // Avoid deleting VM. For testing purpose only.
                    "skipExternalNetworking" : "false",
                    "securityGroups" : ["openbar"] as String[],
                    "keyPairName" : keyPair
                ])

                // when set to 'true', agent will automatically start after reboot.
                autoRestartAgent false

                // Optional. Overrides to default cloud driver behavior.
                // When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
                overrides ([
                    "openstack.endpoint": openstackUrl
                ])

                env ([
                    "JAVA_HOME" : "/root/java"
                ])
                privileged true
                // optional. A native command line to be executed before the cloudify agent is started.
                initializationCommand "#!/bin/sh\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts"

		custom (["openstack.compute.zone":availabilityZone])
            },
            "Windows-2012_64R2-FRA-Puppet-Cygwin" : computeTemplate{
                // Mandatory. Image ID.
                imageId "RegionOne/88da4bf3-5d7a-425a-b7b3-f775cb19d385"

                remoteDirectory "/cygdrive/c/cygwin64/home/root/gs-files"
                // Mandatory. Amount of RAM available to machine.
                machineMemoryMB 1600
                // Mandatory. Hardware ID.
                hardwareId "RegionOne/4"
                // Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
                localDirectory "upload"

                // file transfer protocol
                fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP

                javaUrl "https://s3-eu-west-1.amazonaws.com/cloudify-eu/jdk1.6.0_25_x64.zip"

                username "root"
                password "clouD?B"

                // Additional template options.
                // When used with the default driver, the option names are considered
                // method names invoked on the TemplateOptions object with the value as the parameter.
                options ([
                    "KILL_VM" : "true", // Avoid deleting VM. For testing purpose only.
                    "skipExternalNetworking" : "false",
                    "securityGroups" : ["openbar"] as String[],
                    "keyPairName" : keyPair
                ])

                // when set to 'true', agent will automatically start after reboot.
                autoRestartAgent false

                // Optional. Overrides to default cloud driver behavior.
                // When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
                overrides ([
                    "openstack.endpoint": openstackUrl
                ])
                privileged true
                env ([
                    "INSTALL_ENV" : "CYGWIN",
                    "JAVA_HOME" : "C:\\cygwin64\\home\\root\\java",
                ])
		custom (["openstack.compute.zone":availabilityZone])
            },
            "centos-6_4-64-hdp-1.3" : computeTemplate{
                // Mandatory. Image ID.
                imageId "RegionOne/1ac2f8bb-1510-4743-a78f-03e3063ed9a7"
                // Mandatory. Files from the local directory will be copied to this directory on the remote machine.
                remoteDirectory remoteDirectory
                // Mandatory. Amount of RAM available to machine.
                machineMemoryMB 1600
                // Mandatory. Hardware ID.
                hardwareId "RegionOne/9"
                // Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
                localDirectory "upload"
                // Optional. Name of key file to use for authenticating to the remot machine. Remove this line if key files
                // are not used.
                keyFile keyFile
                // file transfer protocol
                fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
                //javaUrl javaUrl

                username username
                // Additional template options.
                // When used with the default driver, the option names are considered
                // method names invoked on the TemplateOptions object with the value as the parameter.
                options ([
                    "KILL_VM" : "false", // Avoid deleting VM. For testing purpose only.
                    "skipExternalNetworking" : "false",
                    "securityGroups" : ["openbar"] as String[],
                    "keyPairName" : keyPair
                ])

                // when set to 'true', agent will automatically start after reboot.
                autoRestartAgent false

                // Optional. Overrides to default cloud driver behavior.
                // When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
                overrides ([
                    "openstack.endpoint": openstackUrl
                ])

                env ([
                    "JAVA_HOME" : "/root/java"
                ])
                privileged true
                // optional. A native command line to be executed before the cloudify agent is started.
                // initializationCommand "#!/bin/sh\ncp /etc/resolv.conf /tmp/resolv.conf\necho nameserver 8.8.8.8 > /etc/resolv.conf\ncat  /tmp/resolv.conf >> /etc/resolv.conf\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts"
                initializationCommand "#!/bin/sh\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts \n service iptables stop"

		custom (["openstack.compute.zone":availabilityZone])
            },
            "CentOS-6.4-Postgres91-MINEFI" : computeTemplate{
                // Mandatory. Image ID.
                imageId "RegionOne/fce3ac7d-db77-413a-9fe1-69fc1950036c"
                // Mandatory. Files from the local directory will be copied to this directory on the remote machine.
                remoteDirectory remoteDirectory
                // Mandatory. Amount of RAM available to machine.
                machineMemoryMB 1600
                // Mandatory. Hardware ID.
                hardwareId "RegionOne/9"
                // Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
                localDirectory "upload"
                // Optional. Name of key file to use for authenticating to the remot machine. Remove this line if key files
                // are not used.
                keyFile keyFile
                // file transfer protocol
                fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
                //javaUrl javaUrl

                username username
                // Additional template options.
                // When used with the default driver, the option names are considered
                // method names invoked on the TemplateOptions object with the value as the parameter.
                options ([
                    "KILL_VM" : "false", // Avoid deleting VM. For testing purpose only.
                    "skipExternalNetworking" : "false",
                    "securityGroups" : ["openbar"] as String[],
                    "keyPairName" : keyPair
                ])

                // when set to 'true', agent will automatically start after reboot.
                autoRestartAgent false

                // Optional. Overrides to default cloud driver behavior.
                // When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
                overrides ([
                    "openstack.endpoint": openstackUrl
                ])

                env ([
                    "JAVA_HOME" : "/root/java"
                ])
                privileged true
                // optional. A native command line to be executed before the cloudify agent is started.
                // initializationCommand "#!/bin/sh\ncp /etc/resolv.conf /tmp/resolv.conf\necho nameserver 8.8.8.8 > /etc/resolv.conf\ncat  /tmp/resolv.conf >> /etc/resolv.conf\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts"
                initializationCommand "#!/bin/sh\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts \n service iptables stop"

		custom (["openstack.compute.zone":availabilityZone])
            },
            "CentOS-6.4 x86_64" : computeTemplate{
                // Mandatory. Image ID.
                imageId "RegionOne/3cfde3be-247e-4927-9920-fd7317f8a223"
                // Mandatory. Files from the local directory will be copied to this directory on the remote machine.
                remoteDirectory remoteDirectory
                // Mandatory. Amount of RAM available to machine.
                machineMemoryMB 1600
                // Mandatory. Hardware ID.
                hardwareId "RegionOne/9"
                // Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
                localDirectory "upload"
                // Optional. Name of key file to use for authenticating to the remot machine. Remove this line if key files
                // are not used.
                keyFile keyFile
                // file transfer protocol
                fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
                //javaUrl javaUrl

                username username
                // Additional template options.
                // When used with the default driver, the option names are considered
                // method names invoked on the TemplateOptions object with the value as the parameter.
                options ([
                    "KILL_VM" : "false", // Avoid deleting VM. For testing purpose only.
                    "skipExternalNetworking" : "false",
                    "securityGroups" : ["openbar"] as String[],
                    "keyPairName" : keyPair
                ])

                // when set to 'true', agent will automatically start after reboot.
                autoRestartAgent false

                // Optional. Overrides to default cloud driver behavior.
                // When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
                overrides ([
                    "openstack.endpoint": openstackUrl
                ])

                env ([
                    "JAVA_HOME" : "/root/java"
                ])
                privileged true
                // optional. A native command line to be executed before the cloudify agent is started.
                // initializationCommand "#!/bin/sh\ncp /etc/resolv.conf /tmp/resolv.conf\necho nameserver 8.8.8.8 > /etc/resolv.conf\ncat  /tmp/resolv.conf >> /etc/resolv.conf\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts"
                initializationCommand "#!/bin/sh\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts \n service iptables stop"

    custom (["openstack.compute.zone":availabilityZone])
            },
            "OPENVPN-SERVER-CENTOS" : computeTemplate{
                // Mandatory. Image ID.
                imageId "RegionOne/730d3833-e8d0-44a0-b24e-8636c6786eec"
                // Mandatory. Files from the local directory will be copied to this directory on the remote machine.
                remoteDirectory remoteDirectory
                // Mandatory. Amount of RAM available to machine.
                machineMemoryMB 1600
                // Mandatory. Hardware ID.
                hardwareId "RegionOne/9"
                // Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
                localDirectory "upload"
                // Optional. Name of key file to use for authenticating to the remot machine. Remove this line if key files
                // are not used.
                keyFile keyFile
                // file transfer protocol
                fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
                //javaUrl javaUrl

                username username
                // Additional template options.
                // When used with the default driver, the option names are considered
                // method names invoked on the TemplateOptions object with the value as the parameter.
                options ([
                    "KILL_VM" : "false", // Avoid deleting VM. For testing purpose only.
                    "skipExternalNetworking" : "false",
                    "securityGroups" : ["openbar"] as String[],
                    "keyPairName" : keyPair
                ])

                // when set to 'true', agent will automatically start after reboot.
                autoRestartAgent false

                // Optional. Overrides to default cloud driver behavior.
                // When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
                overrides ([
                    "openstack.endpoint": openstackUrl
                ])

                env ([
                    "JAVA_HOME" : "/root/java"
                ])
                privileged true
                // optional. A native command line to be executed before the cloudify agent is started.
                // initializationCommand "#!/bin/sh\ncp /etc/resolv.conf /tmp/resolv.conf\necho nameserver 8.8.8.8 > /etc/resolv.conf\ncat  /tmp/resolv.conf >> /etc/resolv.conf\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts"
                initializationCommand "#!/bin/sh\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts \n service iptables stop"

		custom (["openstack.compute.zone":availabilityZone])
            }
        ])
    }

    /*****************
     * Optional. Custom properties used to extend existing drivers or create new ones.
     */
    custom ([:])
}
