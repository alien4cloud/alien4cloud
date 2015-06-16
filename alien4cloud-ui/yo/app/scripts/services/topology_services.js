'use strict';

angular.module('alienUiApp').factory('topologyServices', ['$resource',
  function($resource) {
    // Service that gives access to create topology
    var topologyScalingPoliciesDAO = $resource('rest/topologies/:topologyId/scalingPolicies/:nodeTemplateName', {}, {});

    var topologyDAO = $resource('rest/topologies/:topologyId', {}, {
      'create': {
        method: 'POST'
      },
      'get': {
        method: 'GET'
      }
    });

    var addNodeTemplate = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName', {}, {
      'add': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'remove': {
        method: 'DELETE',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var setInputToProperty = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/property/:propertyId/input', {}, {
      'set': {
        method: 'POST',
        params: {
          topologyId: '@topologyId',
          inputId: '@inputId',
          nodeTemplateName: '@nodeTemplateName',
          propertyId: '@propertyId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'unset': {
        method: 'DELETE',
        params: {
          topologyId: '@topologyId',
          inputId: '@inputId',
          nodeTemplateName: '@nodeTemplateName',
          propertyId: '@propertyId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var setInputToRelationshipProperty = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/relationship/:relationshipId/property/:propertyId/input', {}, {
      'set': {
        method: 'POST',
        params: {
          topologyId: '@topologyId',
          inputId: '@inputId',
          nodeTemplateName: '@nodeTemplateName',
          propertyId: '@propertyId',
          relationshipId: '@relationshipId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'unset': {
        method: 'DELETE',
        params: {
          topologyId: '@topologyId',
          inputId: '@inputId',
          nodeTemplateName: '@nodeTemplateName',
          propertyId: '@propertyId',
          relationshipId: '@relationshipId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var setInputToCapabilityProperty = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/capability/:capabilityId/property/:propertyId/input', {}, {
      'set': {
        method: 'POST',
        params: {
          topologyId: '@topologyId',
          inputId: '@inputId',
          nodeTemplateName: '@nodeTemplateName',
          propertyId: '@propertyId',
          capabilityId: '@capabilityId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'unset': {
        method: 'DELETE',
        params: {
          topologyId: '@topologyId',
          inputId: '@inputId',
          nodeTemplateName: '@nodeTemplateName',
          propertyId: '@propertyId',
          capabilityId: '@capabilityId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var getPropertyInputCandidates = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/property/:propertyId/inputcandidats', {}, {
      'getCandidates': {
        method: 'GET',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          propertyId: '@propertyId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var artifactInputCandidates = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/artifacts/:artifactId/inputcandidates');

    var getRelationshipPropertyInputCandidates = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/relationship/:relationshipId/property/:propertyId/inputcandidats', {}, {
      'getCandidates': {
        method: 'GET',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          propertyId: '@propertyId',
          relationshipId: '@relationshipId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var getCapabilityPropertyInputCandidates = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/capability/:capabilityId/property/:propertyId/inputcandidats', {}, {
      'getCandidates': {
        method: 'GET',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          propertyId: '@propertyId',
          capabilityId: '@capabilityId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var updateInput = $resource('rest/topologies/:topologyId/inputs/:inputId', {}, {
      'add': {
        method: 'POST',
        params: {
          topologyId: '@topologyId',
          inputId: '@inputId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'update': {
        method: 'PUT',
        params: {
          topologyId: '@topologyId',
          inputId: '@inputId',
          newInputId: '@newInputId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'remove': {
        method: 'DELETE',
        params: {
          topologyId: '@topologyId',
          inputId: '@inputId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var updateOutputProperty = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/property/:propertyName/isOutput', {}, {
      'add': {
        method: 'POST',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          propertyName: '@propertyName'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'remove': {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var artifacts = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/artifacts/:artifactId/reset', {}, {
      'resetArtifact': {
        method: 'PUT',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          artifactId: '@artifactId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var inputArtifact = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/artifacts/:artifactId/:inputArtifactId');

    var inputArtifacts = $resource('rest/topologies/:topologyId/inputArtifacts/:inputArtifactId', {}, {
      'rename': {
        method: 'POST',
        params: {
          newId: '@newId'
        }
      },
      'remove': {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var updateNodeTemplateName = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/updateName/:newName', {}, {
      'updateName': {
        method: 'PUT',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          newName: '@newName'
        }
      }
    });

    var updateNodeProperty = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/properties', {}, {
      'update': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var relationshipDAO = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/relationships/:relationshipName', {}, {
      'add': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'remove': {
        method: 'DELETE'
      }
    });

    var updateRelationshipName = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/relationships/:relationshipName/updateName', {}, {
      'updateName': {
        method: 'PUT',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          relationshipName: '@relationshipName',
          newName: '@newName'
        }
      }
    });

    var updateRelationshipProperty = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/relationships/:relationshipName/updateProperty', {}, {
      'updateProperty': {
        method: 'POST',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          relationshipName: '@relationshipName',
          updateIndexedTypePropertyRequest: '@updateIndexedTypePropertyRequest'
        }
      }
    });

    var updateCapabilityProperty = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/capability/:capabilityId/updateProperty', {}, {
      'updateProperty': {
        method: 'POST',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          capabilityId: '@capabilityId',
          updateIndexedTypePropertyRequest: '@updateIndexedTypePropertyRequest'
        }
      }
    });

    var updateCapabilityOutputProperty = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/capability/:capabilityId/property/:propertyId/isOutput', {}, {
      'add': {
        method: 'POST',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          propertyId: '@propertyId',
          capabilityId: '@capabilityId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'remove': {
        method: 'DELETE',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          propertyId: '@propertyId',
          capabilityId: '@capabilityId'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var isValid = $resource('rest/topologies/:topologyId/isvalid', {}, {
      method: 'GET'
    });

    var yaml = $resource('rest/topologies/:topologyId/yaml', {}, {
      method: 'GET'
    });

    var replacements = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/replace', {}, {
      'get': {
        method: 'GET'
      },
      'replace': {
        method: 'PUT',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var cloudResource = $resource('rest/applications/:applicationId/cloud', {}, {
      'set': {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var updateOutputAttribute = $resource('rest/topologies/:topologyId/nodetemplates/:nodeTemplateName/attributes/:attributeName/output', {}, {
      'add': {
        method: 'POST',
        params: {
          topologyId: '@topologyId',
          nodeTemplateName: '@nodeTemplateName',
          attributeName: '@attributeName'
        },
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'remove': {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var nodeGroupsResource = $resource('rest/topologies/:topologyId/nodeGroups/:groupId', {}, {
      'rename': {
        method: 'PUT',
        params: {
          newName: '@newName'
        }
      },
      'remove': {
        method: 'DELETE'
      }
    });

    var nodeGroupMembersResource = $resource('rest/topologies/:topologyId/nodeGroups/:groupId/members/:nodeTemplateName');

    return {
      'dao': topologyDAO,
      'inputs': updateInput,
      'nodeTemplate': {
        'add': addNodeTemplate.add,
        'remove': addNodeTemplate.remove,
        'updateName': updateNodeTemplateName.updateName,
        'updateProperty': updateNodeProperty.update,
        'setInputs': setInputToProperty,
        'getInputCandidates': getPropertyInputCandidates,
        'getPossibleReplacements': replacements.get,
        'replace': replacements.replace,
        'outputProperties': updateOutputProperty,
        'outputAttributes': updateOutputAttribute,
        'artifacts': {
          'getInputCandidates': artifactInputCandidates.get,
          'setInput': inputArtifact.save,
          'unsetInput': inputArtifact.remove,
          'resetArtifact': artifacts.resetArtifact
        },
        'relationship': {
          'getInputCandidates': getRelationshipPropertyInputCandidates,
          'setInputs': setInputToRelationshipProperty
        },
        'capability': {
          'getInputCandidates': getCapabilityPropertyInputCandidates,
          'setInputs': setInputToCapabilityProperty,
          'outputProperties': updateCapabilityOutputProperty
        }
      },
      'topologyScalingPoliciesDAO': topologyScalingPoliciesDAO,
      'relationshipDAO': relationshipDAO,
      'capability': {
        'updateProperty': updateCapabilityProperty.updateProperty
      },
      'relationship': {
        'updateName': updateRelationshipName.updateName,
        'updateProperty': updateRelationshipProperty.updateProperty
      },
      'nodeGroups': {
        'rename': nodeGroupsResource.rename,
        'remove': nodeGroupsResource.remove,
        'addMember': nodeGroupMembersResource.save,
        'removeMember': nodeGroupMembersResource.remove
      },
      'inputArtifacts': {
        'rename': inputArtifacts.rename,
        'remove': inputArtifacts.remove
      },
      'isValid': isValid.get,
      'getYaml': yaml.get,
      'cloud': cloudResource
    };
  }
]);
