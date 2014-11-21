
name: calm-java-types
namespace: calm
description: Define types for java.

node_types:
  tosca.nodes.Java:
    description: >
      A java installation on a compute.
    derived_from: tosca.nodes.Middleware
    properties:
      version:
        type: string
      update:
        type: string
      vendor:
        type: string
      os_name:
        type: string
      os_arch:
        type: string
    capabilities:
      java:
        type: tosca.capabilities.Java
        upper_bound: unbounded
    requirements:
      compute:
        type: tosca.requirements.Compute
    interfaces:
      lifecycle:
        operations:
          create: /scripts/install.sh
          delete: /scripts/uninstall.sh

requirement_types:
  tosca.requirements.Java:
    required_capability_type: tosca.capabilities.Java
    properties:
      version:
        type: string
      update:
        type: string
      vendor:
        type: string
      os_name:
        type: string
      os_arch:
        type: string

capability_types:
  tosca.capabilities.Java:
    properties:
      version:
        type: string
      update:
        type: string
      vendor:
        type: string
      os_name:
        type: string
      os_arch:
        type: string