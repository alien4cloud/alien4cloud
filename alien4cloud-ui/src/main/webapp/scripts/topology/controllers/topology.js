// Topology editor controller
define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('d3');
  require('toaster');

  require('scripts/common/directives/drag_drop');
  require('scripts/common/directives/property_display');
  require('scripts/common/directives/secret_display');
  require('scripts/common/directives/simple_modal');
  require('scripts/components/services/component_services');
  require('scripts/tosca/services/tosca_service');
  require('scripts/tosca/services/node_template_service');

  require('scripts/topology/controllers/topology_editor_artifacts');
  require('scripts/topology/controllers/topology_editor_display');
  require('scripts/topology/controllers/topology_editor_groups');
  require('scripts/topology/controllers/topology_editor_inputs');
  require('scripts/topology/controllers/topology_editor_nodes');
  require('scripts/topology/controllers/topology_editor_nodesswap');
  require('scripts/topology/controllers/topology_editor_outputs');
  require('scripts/topology/controllers/topology_editor_policies');
  require('scripts/topology/controllers/topology_editor_properties');
  require('scripts/topology/controllers/topology_editor_secrets');
  require('scripts/topology/controllers/topology_editor_relationships');
  require('scripts/topology/controllers/topology_editor_substitution');
  require('scripts/topology/controllers/topology_editor_dependencies');

  require('scripts/topology/controllers/search_relationship');

  require('scripts/topology/services/topology_editor_events_services');

  require('scripts/common/controllers/confirm_modal');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.bootstrap', 'a4c-tosca', 'a4c-styles']).controller('TopologyCtrl',
    ['$scope', '$uibModal', '$timeout', 'componentService', 'nodeTemplateService', 'toscaService','hotkeys', '$translate',
    'defaultFilters',
    'badges',
    'topoEditArtifacts',
    'topoEditDisplay',
    'topoEditGroups',
    'topoEditInputs',
    'topoEditNodes',
    'topoEditNodesSwap',
    'topoEditOutputs',
    'topoEditPolicies',
    'topoEditProperties',
    'topoEditSecrets',
    'topoEditRelationships',
    'topoEditSubstitution',
    'topoEditDependencies',
    function($scope, $uibModal, $timeout, componentService, nodeTemplateService, toscaService, hotkeys, $translate,
    defaultFilters,
    badges,
    topoEditArtifacts,
    topoEditDisplay,
    topoEditGroups,
    topoEditInputs,
    topoEditNodes,
    topoEditNodesSwap,
    topoEditOutputs,
    topoEditPolicies,
    topoEditProperties,
    topoEditSecrets,
    topoEditRelationships,
    topoEditSubstitution,
    topoEditDependencies) {
      $scope.defaultFilters = defaultFilters;
      $scope.badges = badges;
      $scope.isRuntime = false;

      $scope.isNodeTemplateCollapsed = false;
      $scope.isPropertiesCollapsed = false;
      $scope.isRelationshipsCollapsed = false;
      $scope.isRelationshipCollapsed = false;
      $scope.isArtifactsCollapsed = false;
      $scope.isArtifactCollapsed = false;
      $scope.isRequirementsCollapsed = false;
      $scope.isCapabilitiesCollapsed = false;
      $scope.displays = {
        catalog: { active: true, size: 500, selector: '#catalog-box', only: ['catalog'] },
        dependencies: { active: false, size: 400, selector: '#dependencies-box', only: ['dependencies'] },
        inputs: { active: false, size: 400, selector: '#inputs-box', only: ['inputs'], keep: ['nodetemplate'] },
        policies: { active: false, size: 400, selector: '#policies-box', only: ['policies'], keep: ['policiescatalog']},
        policiescatalog: { active: false, size: 500, selector: '#policiescatalog-box', only: ['policiescatalog'], keep: ['policies']},
        groups: { active: false, size: 400, selector: '#groups-box', only: ['groups'], keep: ['nodetemplate'] },
        substitutions: { active: false, size: 400, selector: '#substitutions-box', only: ['substitutions'], keep: ['nodetemplate'] },
        nodetemplate: { active: false, size: 550, selector: '#nodetemplate-box', only: ['nodetemplate'], keep: ['inputs'] }
      };

      topoEditDisplay($scope, '#topology-editor');
      topoEditArtifacts($scope);
      topoEditGroups($scope);
      topoEditInputs($scope);
      topoEditNodes($scope);
      topoEditNodesSwap($scope);
      topoEditOutputs($scope);
      topoEditPolicies($scope);
      topoEditProperties($scope);
      topoEditSecrets($scope);
      topoEditRelationships($scope);
      topoEditSubstitution($scope);
      topoEditDependencies($scope);

      $scope.initializeWorkspacesFilters = function () {
        // if there is workspaces in the scope application add them to the scope
        if(_.defined($scope.workspaces) && $scope.workspaces.length > 0) {
          $scope.staticFacets = {workspace: []};
          _.each($scope.workspaces, function(workspace) {
            $scope.staticFacets.workspace.push({facetValue: workspace, count: ''});
          });
          $scope.staticFacets.workspace[0].staticFilter = $scope.workspaces;
          $scope.defaultFilters.workspace =  $scope.workspaces;
        }
      };

      var refresh = function(selectedNodeTemplateName) {

        $scope.initializeWorkspacesFilters();

        if(_.undefined($scope.groupCollapsed)) { // we perform this only at init time.
          $scope.groupCollapsed = {};
          _.each($scope.topology.topology.groups, function(value, key) {
            $scope.groupCollapsed[key] = { main: false, members: true, policies: true };
          });
        }

        $scope.outputs.init($scope.topology.topology);
        // TODO trigger yaml update ?

        function reselectNodeTemplate(name) {
          $scope.selectedNodeTemplate = $scope.topology.topology.nodeTemplates[name];
          if(_.defined($scope.selectedNodeTemplate)) {
            $scope.selectedNodeTemplate.selected = true;
            fillNodeSelectionVars($scope.selectedNodeTemplate);
          } else {
            $scope.selectedNodeTemplate = null;
          }
        }

        if(_.defined(selectedNodeTemplateName)) {
          reselectNodeTemplate(selectedNodeTemplateName);
        } else if(_.defined($scope.selectedNodeTemplate)) {
          reselectNodeTemplate($scope.selectedNodeTemplate.name);
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

      $scope.$on('topologyRefreshedEvent', function(event, param) {
        var selectedNodeTemplateName = param.selectedNodeTemplateName;
        refresh(selectedNodeTemplateName);
      });

      if(_.defined($scope.topology)) {
        refresh();
      }

      $scope.checkMapSize = function(map) {
        return angular.isDefined(map) && map !== null && Object.keys(map).length > 0;
      };

      // check if compute type
      $scope.isComputeType = function(nodeTemplate) {
        if (_.undefined($scope.topology) || _.undefined(nodeTemplate)) {
          return false;
        }
        return toscaService.isComputeType(nodeTemplate.type, $scope.topology.nodeTypes);
      };
      $scope.isDockerType = function(nodeTemplate) {
        if (_.undefined($scope.topology) || _.undefined(nodeTemplate)) {
          return false;
        }
        return toscaService.isDockerType(nodeTemplate.type, $scope.topology.nodeTypes);
      };

      $scope.nodeNameObj = {}; // added to support <tabset> "childscope issue"
      function fillNodeSelectionVars(nodeTemplate) {
        $scope.selectedNodeTemplate = nodeTemplate;
        $scope.nodeNameObj.val = nodeTemplate.name;
        if($scope.isDockerType(nodeTemplate)) {
          $scope.dockerImage = { defined: false };
          $scope.dockerImage.value = undefined;
          var nodeType = $scope.topology.nodeTypes[nodeTemplate.type];
          $scope.dockerImage.typeValue = _.get(nodeType, 'interfaces.["tosca.interfaces.node.lifecycle.Standard"].operations.create.implementationArtifact.artifactRef');
          var templateDockerImage = _.get(nodeTemplate, 'interfaces.["tosca.interfaces.node.lifecycle.Standard"].operations.create.implementationArtifact.artifactRef');
          if(_.defined(templateDockerImage)) {
            $scope.dockerImage.value = templateDockerImage;
          } else if(_.defined($scope.dockerImage.typeValue)) {
            $scope.dockerImage.value = $scope.dockerImage.typeValue;
          }
          $scope.dockerImage.defined = _.defined($scope.dockerImage.value);
        }
        $scope.selectionabstract = $scope.topology.nodeTypes[nodeTemplate.type].abstract;
      }

      $scope.getIcon = toscaService.getIcon;
      $scope.getTag = toscaService.getTag;

      /**
       * Capabilities and Requirements docs
       */
       // FIXME this is actually to get Requirements and Capabilities but their version may not be the same as the actual node template
       // may be from different archives..
      $scope.getComponent = function(nodeTemplate, type) {
        var nodeType = $scope.topology.nodeTypes[nodeTemplate.type];
        if(_.defined($scope.topology) && _.defined($scope.topology.capabilityTypes) && _.defined($scope.topology.capabilityTypes[type])) {
          return $scope.topology.capabilityTypes[type];
        }
        var componentId = type + ':' + nodeType.archiveVersion;
        return componentService.get({
          componentId: componentId
        }, function(successResult) {
          if (successResult.error === null) {
            return successResult.data;
          }
        });
      };

      // Object in which the topology svg directive will inject control ops.
      $scope.graphControl = {};

      $scope.editorCallback = {
        addRelationship: function(sourceId, requirementName, requirementType, targetId, capabilityName, relationship) {
          if(_.defined(relationship)) {
            // generate relationship name
            var name = toscaService.generateRelationshipName(relationship.elementId, targetId, capabilityName);
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
        selectNodeTemplate: function(newSelectedName) {
          $scope.display.set('nodetemplate', true);
          if (_.defined($scope.selectedNodeTemplate)) {
            var oldSelected = $scope.topology.topology.nodeTemplates[$scope.selectedNodeTemplate.name];
            if (oldSelected) {
              oldSelected.selected = false;
            }
          }

          var newSelected = $scope.topology.topology.nodeTemplates[newSelectedName];
          newSelected.selected = true;

          fillNodeSelectionVars(newSelected);
          $scope.$broadcast('editorSelectionChangedEvent', { nodeNames: [ newSelectedName ] });
          $scope.$digest();
        },
        updateNodePosition: function(name, x, y) {
          $scope.nodes.updatePosition(name, x, y);
        }
      };

      // key binding
      function duplicateNode(node){
        if(_.defined(node)){
          $scope.nodes.duplicate(node.name);
        }
      }
      function deleteNode(node) {
        if(_.defined(node)){
          var modalInstance = $uibModal.open({
            templateUrl: 'views/common/confirm_modal.html',
            controller: 'ConfirmModalCtrl',
            resolve: {
              title: function() {
                return 'DELETE';
              },
              content: function() {
                return $translate('DELETE_CONFIRM');
              }
            }
          });
          modalInstance.result.then(function () {
            $scope.nodes.delete(node.name);
          });
        }
      }
      hotkeys.bindTo($scope)
        .add ({
          combo: 'mod+d',
          description: 'Duplicate the selected node template with his hostedOn hierarchy.',
          callback: function(e) {
            duplicateNode($scope.selectedNodeTemplate);
            if(e.preventDefault) {
              e.preventDefault();
            } else {
              e.returnValue = false;
            }
          }
        })
        .add ({
          combo: 'del',
          description: 'Delete the selected node template.',
          callback: function(e) {
            deleteNode($scope.selectedNodeTemplate);
            if(e.preventDefault) {
              e.preventDefault();
            } else {
              e.returnValue = false;
            }
          }
        });
    }
  ]);
}); // define
