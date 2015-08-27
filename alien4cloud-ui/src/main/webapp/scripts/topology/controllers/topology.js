// Topology editor controller
define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('d3');
  require('toaster');
  require('bower_components/jquery-ui/ui/resizable');

  require('scripts/common/directives/drag_drop');
  require('scripts/common/directives/property_display');
  require('scripts/common/directives/simple_modal');
  require('scripts/common/services/resize_services');
  require('scripts/components/services/component_services');
  require('scripts/tosca/services/tosca_service');
  require('scripts/tosca/services/node_template_service');
  require('scripts/tosca/services/tosca_cardinalities_service');

  require('scripts/topology/controllers/topology_editor_artifacts');
  require('scripts/topology/controllers/topology_editor_display');
  require('scripts/topology/controllers/topology_editor_groups');
  require('scripts/topology/controllers/topology_editor_inputs');
  require('scripts/topology/controllers/topology_editor_nodes');
  require('scripts/topology/controllers/topology_editor_nodesswap');
  require('scripts/topology/controllers/topology_editor_outputs');
  require('scripts/topology/controllers/topology_editor_properties');
  require('scripts/topology/controllers/topology_editor_relationships');
  require('scripts/topology/controllers/topology_editor_substitution');
  require('scripts/topology/controllers/topology_editor_versions');
  require('scripts/topology/controllers/topology_editor_yaml');

  require('scripts/topology/controllers/search_relationship');
  require('scripts/topology/services/topology_json_processor');
  require('scripts/topology/services/topology_services');
  require('scripts/topology/directives/plan_rendering');
  require('scripts/topology/directives/topology_rendering');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.bootstrap', 'a4c-tosca', 'a4c-styles']).controller('TopologyCtrl',
    ['$scope', '$modal', '$timeout', 'topologyJsonProcessor', 'topologyServices', 'componentService', 'nodeTemplateService', 'appVersions', 'preselectedVersion', 'topologyId', 'toscaService', 'toscaCardinalitiesService',
    'topoEditArtifacts',
    'topoEditDisplay',
    'topoEditGroups',
    'topoEditInputs',
    'topoEditNodes',
    'topoEditNodesSwap',
    'topoEditOutputs',
    'topoEditProperties',
    'topoEditRelationships',
    'topoEditSubstitution',
    'topoEditVersions',
    'topoEditYaml',
    function($scope, $modal, $timeout, topologyJsonProcessor, topologyServices, componentService, nodeTemplateService, appVersions, preselectedVersion, topologyId, toscaService, toscaCardinalitiesService,
    topoEditArtifacts,
    topoEditDisplay,
    topoEditGroups,
    topoEditInputs,
    topoEditNodes,
    topoEditNodesSwap,
    topoEditOutputs,
    topoEditProperties,
    topoEditRelationships,
    topoEditSubstitution,
    topoEditVersions,
    topoEditYaml) {

      topoEditArtifacts($scope);
      topoEditDisplay($scope);
      topoEditGroups($scope);
      topoEditInputs($scope);
      topoEditNodes($scope);
      topoEditNodesSwap($scope);
      topoEditOutputs($scope);
      topoEditProperties($scope);
      topoEditRelationships($scope);
      topoEditSubstitution($scope);
      topoEditVersions($scope);
      topoEditYaml($scope);

      // TODO : when topology templates edition with use also version, remove this IF statement
      if (_.defined(appVersions) && _.defined(appVersions.data)) {
        // default version loading
        $scope.appVersions = appVersions.data;
        $scope.selectedVersion = $scope.appVersions[0];
        $scope.topologyId = $scope.selectedVersion.topologyId;
        if (_.defined(preselectedVersion)) {
          $scope.version.setName(preselectedVersion);
        }
      } else {
        // TODO : remove this part when apVersion will be given in state 'topologytemplates.detail.topology'
        $scope.topologyId = topologyId;
      }

      $scope.refreshTopology = function(topologyDTO, selectedNodeTemplate) {
        for (var nodeId in topologyDTO.topology.nodeTemplates) {
          if (topologyDTO.topology.nodeTemplates.hasOwnProperty(nodeId)) {
            topologyDTO.topology.nodeTemplates[nodeId].name = nodeId;
          }
        }
        $scope.topology = topologyDTO;
        $scope.isTopologyTemplate = ($scope.topology.topology.delegateType === 'topologytemplate');

        // enrich objects to add maps for the fields that are currently mapped as array of map entries.
        topologyJsonProcessor.process($scope.topology);

        fillBounds($scope.topology.topology);
        $scope.outputs.init($scope.topology.topology);
        var topologyInputs = $scope.topology.topology.inputs;
        if (_.defined(topologyInputs)) {
          for (var inputId in topologyInputs) {
            if (topologyInputs.hasOwnProperty(inputId)) {
              topologyInputs[inputId].inputId = inputId;
            }
          }
        }
        $scope.yaml.update($scope.topology.yaml);

        if (_.defined(selectedNodeTemplate)) {
          fillNodeSelectionVars($scope.topology.topology.nodeTemplates[selectedNodeTemplate]);
        } else {
          $scope.selectedNodeTemplate = null;
        }

        // we need an ordered nodeGroup array (on index property)
        $scope.orderedNodeGroups = [];
        angular.forEach($scope.topology.topology.groups, function(value) {
          $scope.orderedNodeGroups.push(value);
        });
        $scope.orderedNodeGroups.sort(function(a, b){
          return a.index - b.index;
        });

        $scope.substitution.refresh();
      };

      // Topology can comes from application OR topology template
      topologyServices.dao.get({
        topologyId: $scope.topologyId
      }, function(successResult) {
        $scope.refreshTopology(successResult.data);
        // init the group collapse indicators
        $scope.groupCollapsed = {};
        angular.forEach($scope.topology.topology.groups, function(value, key) {
          $scope.groupCollapsed[key] = { main: false, members: true, policies: true };
        });

      });

      $scope.checkMapSize = function(map) {
        return angular.isDefined(map) && map !== null && Object.keys(map).length > 0;
      };

      $scope.nodeNameObj = {}; // added to support <tabset> "childscope issue"
      function fillNodeSelectionVars(nodeTemplate) {
        $scope.selectedNodeTemplate = nodeTemplate;
        $scope.nodeNameObj.val = nodeTemplate.name;
        $scope.selectionabstract = $scope.topology.nodeTypes[nodeTemplate.type].abstract;
      }

      $scope.updateInputArtifactList = function(artifactName) {
        var nodeTemplateName = $scope.selectedNodeTemplate.name;
        var topology = $scope.topology.topology;
        var inputIndex = topology.inputArtifacts[nodeTemplateName].indexOf(artifactName);
        var artifactInput = {
          topologyId: $scope.topology.topology.id,
          nodeTemplateName: nodeTemplateName,
          artifactName: artifactName
        };

        if (inputIndex < 0) {
          // add input artifact
          topologyServices.nodeTemplate.artifactInput.add(
            artifactInput,
            function(successResult) {
              if (!successResult.error) {
                $scope.refreshTopology(successResult.data, $scope.selectedNodeTemplate ? $scope.selectedNodeTemplate.name : undefined);
              }
            },
            function(errorResult) {
              console.debug(errorResult);
            }
          );
        } else {
          // remove input artifact
          topologyServices.nodeTemplate.artifactInput.remove(
            artifactInput,
            function(successResult) {
              if (!successResult.error) {
                $scope.refreshTopology(successResult.data, $scope.selectedNodeTemplate ? $scope.selectedNodeTemplate.name : undefined);
              } else {
                console.debug(successResult.error);
              }
            },
            function(errorResult) {
              console.debug(errorResult);
            }
          );
        }

      };

      $scope.getIcon = toscaService.getIcon;
      $scope.getShortName = toscaService.simpleName;
      // check if compute type
      $scope.isComputeType = function(nodeTemplate) {
        if (_.undefined($scope.topology) || _.undefined(nodeTemplate)) {
          return false;
        }
        return toscaService.isComputeType(nodeTemplate.type, $scope.topology.nodeTypes);
      };

      var fillBounds = function(topology) {
        if (!topology.nodeTemplates) {
          return;
        }
        Object.keys(topology.nodeTemplates).forEach(function(nodeTemplateName) {
          var nodeTemplate = topology.nodeTemplates[nodeTemplateName];
          toscaCardinalitiesService.fillRequirementBounds($scope.topology.nodeTypes, nodeTemplate);
          toscaCardinalitiesService.fillCapabilityBounds($scope.topology.nodeTypes, $scope.topology.topology.nodeTemplates, nodeTemplate);
        });
      };


      /**
       * Capabilities and Requirements docs
       */
       // FIXME this is actually to get Requirements and Capabilities but their version may not be the same as the actual node template
       // may be from different archives..
      $scope.getComponent = function(nodeTemplate, type) {
        var nodeType = $scope.topology.nodeTypes[nodeTemplate.type];
        var componentId = type + ':' + nodeType.archiveVersion;

        return componentService.get({
          componentId: componentId
        }, function(successResult) {
          if (successResult.error === null) {
            if (successResult.data !== null) {
              return successResult.data.description;
            }
          }
        });
      };

      $scope.editorCallback = {
        autoOpenRelationshipModal: $scope.relationships.autoOpenRelationshipModal,

        addRelationship: function(sourceId, requirementName, requirementType, targetId, capabilityName, relationship) {
          if(_.defined(relationship)) {
            // generate relationship name
            var name = toscaService.generateRelationshipName(relationship.elementId, targetId);
            // add the relationship
            $scope.relationships.doAddRelationship(sourceId, {
              name: name,
              target: targetId,
              targetedCapabilityName: capabilityName,
              relationship: relationship
            }, requirementName, requirementType);
          } else {
            // open the modal so user can choose the target relationship.
            $scope.relationships.openSearchRelationshipModal(sourceId, requirementName, targetId, capabilityName);
          }
        },
        selectNodeTemplate: function(newSelectedName, oldSelectedName) {
          // select the "Properties" <TAB> to see selected node details
          document.getElementById('nodetemplate-details').click();

          $timeout(function() {
            if (oldSelectedName) {
              var oldSelected = $scope.topology.topology.nodeTemplates[oldSelectedName];
              if (oldSelected) {
                oldSelected.selected = false;
              }
            }

            var newSelected = $scope.topology.topology.nodeTemplates[newSelectedName];
            newSelected.selected = true;

            fillNodeSelectionVars(newSelected);
            $scope.display.set('component', true);
            $scope.$apply();
          });
        }
      };
    }
  ]);
}); // define
