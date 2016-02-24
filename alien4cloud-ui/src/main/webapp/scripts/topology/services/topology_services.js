// define the rest api elements to work with topology edition.
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-editor', ['ngResource']).factory('topologyServices', ['$resource',
    function($resource) {
      // Service that gives access to create topology
      var topologyScalingPoliciesDAO = $resource('rest/v1/topologies/:topologyId/scalingPolicies/:nodeTemplateName', {}, {});

      var topologyDAO = $resource('rest/v1/topologies/:topologyId', {}, {
        'create': {
          method: 'POST'
        },
        'get': {
          method: 'GET'
        }
      });

      var addNodeTemplate = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName', {}, {
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

      var setInputToProperty = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/property/:propertyId/input', {}, {
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

      var setInputToRelationshipProperty = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/relationship/:relationshipId/property/:propertyId/input', {}, {
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

      var setInputToCapabilityProperty = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/capability/:capabilityId/property/:propertyId/input', {}, {
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

      var getPropertyInputCandidates = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/property/:propertyId/inputcandidats', {}, {
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

      var artifactInputCandidates = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/artifacts/:artifactId/inputcandidates');

      var getRelationshipPropertyInputCandidates = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/relationship/:relationshipId/property/:propertyId/inputcandidats', {}, {
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

      var getCapabilityPropertyInputCandidates = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/capability/:capabilityId/property/:propertyId/inputcandidats', {}, {
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

      var updateInput = $resource('rest/v1/topologies/:topologyId/inputs/:inputId', {}, {
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

      var updateOutputProperty = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/property/:propertyName/isOutput', {}, {
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

      var artifacts = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/artifacts/:artifactId/reset', {}, {
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

      var inputArtifact = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/artifacts/:artifactId/:inputArtifactId');

      var inputArtifacts = $resource('rest/v1/topologies/:topologyId/inputArtifacts/:inputArtifactId', {}, {
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

      var updateNodeTemplateName = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/updateName/:newName', {}, {
        'updateName': {
          method: 'PUT',
          params: {
            topologyId: '@topologyId',
            nodeTemplateName: '@nodeTemplateName',
            newName: '@newName'
          }
        }
      });

      var updateNodeProperty = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/properties', {}, {
        'update': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var relationshipDAO = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/relationships/:relationshipName', {}, {
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

      var updateRelationshipName = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/relationships/:relationshipName/updateName', {}, {
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

      var updateRelationshipProperty = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/relationships/:relationshipName/updateProperty', {}, {
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

      var updateCapabilityProperty = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/capability/:capabilityId/updateProperty', {}, {
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

      var updateCapabilityOutputProperty = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/capability/:capabilityId/property/:propertyId/isOutput', {}, {
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

      var isValid = $resource('rest/v1/topologies/:topologyId/isvalid', {}, {
        method: 'GET'
      });

      var yaml = $resource('rest/v1/topologies/:topologyId/yaml', {}, {
        method: 'GET'
      });

      var replacements = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/replace', {}, {
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

      var cloudResource = $resource('rest/v1/applications/:applicationId/cloud', {}, {
        'set': {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var updateOutputAttribute = $resource('rest/v1/topologies/:topologyId/nodetemplates/:nodeTemplateName/attributes/:attributeName/output', {}, {
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

      var nodeGroupsResource = $resource('rest/v1/topologies/:topologyId/nodeGroups/:groupId', {}, {
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

      var nodeGroupMembersResource = $resource('rest/v1/topologies/:topologyId/nodeGroups/:groupId/members/:nodeTemplateName');

      var substitutionTypeResource = $resource('rest/v1/topologies/:topologyId/substitutions/type', {}, {
        'set': {
          method: 'PUT',
          params: {
            elementId: '@elementId'
          }
        },
        'remove': {
          method: 'DELETE'
        }
      });

      var capabilitySubstitutionResource = $resource('rest/v1/topologies/:topologyId/substitutions/capabilities/:substitutionCapabilityId', {}, {
        'add': {
          method: 'PUT',
          params: {
            nodeTemplateName: '@nodeTemplateName',
            capabilityId: '@capabilityId'
          }
        },
        'remove': {
          method: 'DELETE'
        },
        'update': {
          method: 'POST',
          params: {
            newCapabilityId: '@newCapabilityId'
          }
        }
      });

      var requirementSubstitutionResource = $resource('rest/v1/topologies/:topologyId/substitutions/requirements/:substitutionRequirementId', {}, {
        'add': {
          method: 'PUT',
          params: {
            nodeTemplateName: '@nodeTemplateName',
            requirementId: '@requirementId'
          }
        },
        'remove': {
          method: 'DELETE'
        },
        'update': {
          method: 'POST',
          params: {
            newRequirementId: '@newRequirementId'
          }        
        }
      });

      var topologyVersionResource = $resource('rest/v1/topologies/:topologyId/version');      

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
        'substitutionType': substitutionTypeResource,
        'capabilitySubstitution' : capabilitySubstitutionResource,
        'requirementSubstitution' : requirementSubstitutionResource,
        'isValid': isValid.get,
        'getYaml': yaml.get,
        'cloud': cloudResource,
        'getTopologyVersion' : topologyVersionResource.get
      };
    }
  ]);
}); // define
