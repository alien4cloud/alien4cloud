/**
* Main controller for topology edition.
* It loads topology and manage common server communication and topology refresh through common parent scope.
*/
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/topology/controllers/topology');
  require('scripts/topology/controllers/editor_browser');
  require('scripts/topology/controllers/editor_workflow');
  require('scripts/topology/controllers/editor_history');
  require('scripts/topology/controllers/editor_git_modal');
  require('scripts/topology/controllers/editor_validation');

  require('scripts/tosca/services/tosca_cardinalities_service');
  require('scripts/topology/services/topology_json_processor');
  require('scripts/topology/services/topology_recovery_service');
  require('scripts/topology/services/topology_services');
  require('scripts/topology/controllers/topology_editor_versions');

  // manage websockets for topology editor
  require('scripts/topology/services/topology_editor_events_services');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.bootstrap', 'a4c-tosca', 'a4c-styles', 'cfp.hotkeys']).controller('TopologyEditorCtrl',
    ['$scope', 'menu', 'layoutService', 'context', 'archiveVersions', 'topologyServices', 'topologyJsonProcessor', 'toscaService', 'toscaCardinalitiesService', 'topoEditVersions', '$alresource',
    'hotkeys','topologyRecoveryServices', '$modal', '$translate', 'toaster', '$state', // 'topologyEditorEventFactory',
    function($scope, menu, layoutService, context, archiveVersions, topologyServices, topologyJsonProcessor, toscaService, toscaCardinalitiesService, topoEditVersions, $alresource, hotkeys, topologyRecoveryServices, $modal, $translate, toaster, $state) {// , topologyEditorEventFactory) {
      // register for websockets events
      // var registration = topologyEditorEventFactory($scope.topologyId, function(event) {
      //   console.log('received event', event);
      // });
      // var operation = {
      //   type: 'org.alien4cloud.tosca.editor.commands.AddNodeTemplateOperation',
      //   message: 'Hello world'
      // };
      // registration.send('/app/topology-editor/' + $scope.topologyId, operation);
      // $scope.$on('$destroy', function() {
      //   registration.close();
      // });

      // This controller acts as a specific layout for the topology edition.
      layoutService.process(menu);
      $scope.menu = menu;
      $scope.getShortName = toscaService.simpleName;
      // Manage topology version selection (version is provided as parameter from the template or application)
      $scope.topologyVersions = archiveVersions.data;
      $scope.versionContext = context;
      $scope.released = false; // this allow to avoid file edition in the ui-ace.
      topoEditVersions($scope);

      /**
      * Add bounds information to the requirements and capabilities in the topology based on relationships.
      */
      function fillBounds(topology) {
        _.each(topology.nodeTemplates, function(nodeTemplate) {
          toscaCardinalitiesService.fillRequirementBounds($scope.topology.nodeTypes, nodeTemplate);
          toscaCardinalitiesService.fillCapabilityBounds($scope.topology.nodeTypes, $scope.topology.topology.nodeTemplates, nodeTemplate);
        });
      }

      // Version selection management is below, find here topology update handling
      /**
      * refreshTopology has to be triggered when the topology is updated.
      * Added to the scope as right now every operation returns the full and update topology.
      */
      $scope.refreshTopology = function(topologyDTO, selectedNodeTemplate, initial) {
        $scope.topology = topologyDTO;
        if(topologyDTO.topology.workspace === 'ALIEN_GLOBAL_WORKSPACE') {
          $scope.workspaces = ['ALIEN_GLOBAL_WORKSPACE'];
        } else {
          $scope.workspaces = [topologyDTO.topology.workspace, 'ALIEN_GLOBAL_WORKSPACE'];
        }

        $scope.isTopologyTemplate = ($scope.topology.delegateType !== 'APPLICATION');
        // Process the topology to enrich it with some additional data
        _.each(topologyDTO.topology.nodeTemplates, function(value, key){
          value.name = key;
        });
        // enrich objects to add maps for the fields that are currently mapped as array of map entries.
        topologyJsonProcessor.process($scope.topology);
        fillBounds($scope.topology.topology);
        _.each($scope.topology.topology.inputs, function(value, key){
          value.inputId = key;
        });

        // trigger refresh event so child scope can update what they need. Initial flag allows to know if this is the initial loading of the topology.
        $scope.$broadcast('topologyRefreshedEvent', {
          initial: initial,
          selectedNodeTemplate: selectedNodeTemplate
        });
      };

      $scope.getLastOperationId = function(nullAsString) {
        if(_.get($scope.topology, 'lastOperationIndex')>= 0) {
          return $scope.topology.operations[$scope.topology.lastOperationIndex].id;
        }
        return _.defined(nullAsString) && nullAsString ? 'null' : null;
      };

      var editorResource = $alresource('rest/latest/editor/:topologyId/execute');
      $scope.execute = function(operation, successCallback, errorCallback, selectedNodeTemplate, isPropertyEdit) {
        operation.previousOperationId = $scope.getLastOperationId();
        // execute operations, create is a post
        return editorResource.create({
          topologyId: $scope.topologyId,
        }, angular.toJson(operation), function(result) {
          if(_.defined(result.error) && result.error.code === 860) {
            // Topology recovery
            topologyRecoveryServices.handleTopologyRecovery(result.data, $scope.topologyId, $scope.getLastOperationId(true)).then(function(recoveryResult) {
              if(_.definedPath(recoveryResult, 'data')) {
                $scope.refreshTopology(recoveryResult.data, selectedNodeTemplate);
                if(_.defined(successCallback)) {
                  successCallback(recoveryResult);
                }
                return;
              }
            });
            return result;
          }

          if(_.undefined(result.error)) {
            if(_.defined(isPropertyEdit)) {
              // If the call is related to a property value edition this may be a complex one and we should not perform full topology override.
              $scope.topology.operations = result.data.operations;
              $scope.topology.lastOperationIndex = result.data.lastOperationIndex;
            } else {
              $scope.refreshTopology(result.data, selectedNodeTemplate);
            }
          }
          if(_.defined(successCallback)) {
            successCallback(result);
          }
          return result;
        }, function(error) {
          if(_.defined(errorCallback)) {
            errorCallback(error);
          }
          return error;
        }).$promise;
      };

      var editorSaveResource = $alresource('rest/latest/editor/:topologyId');
      $scope.save = function() {
        if($scope.topology.operations.length === 0 || $scope.topology.lastOperationIndex===-1) {
          // nothing to save
          return;
        }
        editorSaveResource.create({
          topologyId: $scope.topologyId,
          lastOperationId: $scope.getLastOperationId(true)
        }, null, function(result) {
          if(_.undefined(result.error)) {
            $scope.refreshTopology(result.data);
          }
        });
      };

      var editorUndoResource = $alresource('rest/latest/editor/:topologyId/undo');
      function undoRedo(at) {
        editorUndoResource.create({
          topologyId: $scope.topologyId,
          at: at,
          lastOperationId: $scope.getLastOperationId(true)
        }, null, function(result) {
          if(_.undefined(result.error)) {
            $scope.refreshTopology(result.data);
            return;
          }

          //case there actually is an error
          topologyRecoveryServices.handleTopologyRecovery(result.data, $scope.topologyId, $scope.getLastOperationId(true)).then(function(recoveryResult){
            if(_.definedPath(recoveryResult, 'data')){
              $scope.refreshTopology(recoveryResult.data);
              return;
            }
          });

        });
      }
      $scope.undo = function() {
        if(0 === ($scope.topology.lastOperationIndex + 1)) {
          return;
        }
        var at = $scope.topology.lastOperationIndex - 1;
        undoRedo(at);
      };
      $scope.redo = function() {
        if($scope.topology.operations.length === ($scope.topology.lastOperationIndex + 1)) {
          return;
        }
        var at = $scope.topology.lastOperationIndex + 1;
        undoRedo(at);
      };


      // -- Begin GIT SECTIONS --
      var gitRemoteResource = $alresource('rest/latest/editor/:topologyId/git/remote');

      // Define if the push/pull buttons are enabled or not
      //
      gitRemoteResource.get({topologyId: $scope.topologyId}).$promise.then(function(response){
        if(_.defined(response.data.remoteUrl)) {
          $scope.isGitValid = true;
        } else {
          $scope.isGitValid = false;
        }
      });

      // GIT SET REMOTE
      //
      $scope.gitRemote = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/topology/editor_git_remote_modal.html',
          controller: 'EditorGitRemoteModalController',
          scope: $scope,
          resolve: {
            remoteGit: ['$alresource', function($alresource) {
              return $alresource('rest/latest/editor/:topologyId/git/remote').get({topologyId: $scope.topologyId}).$promise.then(function(response){
                return response.data;
              });
            }]
          }
        });

        modalInstance.result.then(function(remoteUrl) {
          gitRemoteResource.update({
              topologyId: $scope.topologyId,
              remoteUrl: remoteUrl
            }, null, function() {
              $scope.isGitValid = true;
            });
        });
      };

      // GIT PUSH FUNCTION
      //
      $scope.gitPush = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/topology/editor_git_push_pull_modal.html',
          controller: 'EditorGitPushPullModalController',
          scope: $scope,
          resolve: {
            action: function() {
              return 'PUSH';
            }
          }
        });
        modalInstance.result.then(function(gitPushPullForm) {
          var gitPushResource= $alresource('rest/latest/editor/:topologyId/git/push');
          gitPushResource.update({topologyId: $scope.topologyId, remoteBranch: gitPushPullForm.remoteBranch}, angular.toJson(gitPushPullForm.credentials), function(response) {
            if(_.undefined(response.error)) {
              toaster.pop('success', $translate.instant('EDITOR.GIT.OPERATIONS.PUSH.TITLE'), $translate.instant('EDITOR.GIT.OPERATIONS.PUSH.SUCCESS_MSGE'), 4000, 'trustedHtml', null);
            }
            console.debug('pushed');
          });
        });
      };

      // GIT PULL FUNCTION
      //
      $scope.gitPull = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/topology/editor_git_push_pull_modal.html',
          controller: 'EditorGitPushPullModalController',
          scope: $scope,
          resolve: {
            action: function() {
              return 'PULL';
            }
          }
        });
        modalInstance.result.then(function(gitPushPullForm) {
          var gitPullResource= $alresource('rest/latest/editor/:topologyId/git/pull');
          gitPullResource.update({topologyId: $scope.topologyId, remoteBranch: gitPushPullForm.remoteBranch}, angular.toJson(gitPushPullForm.credentials), function(response) {
            if(_.undefined(response.error)){
              toaster.pop('success', $translate.instant('EDITOR.GIT.OPERATIONS.PULL.TITLE'), $translate.instant('EDITOR.GIT.OPERATIONS.PULL.SUCCESS_MSGE'), 4000, 'trustedHtml', null);
            }
            console.debug('pulled');
          });
        });
      };
      // -- End of GIT SECTIONS --

      // key binding
      hotkeys.bindTo($scope)
      .add({
        combo: 'mod+s',
        description: 'save and commit the operations.',
        callback: function(e) {
          $scope.save();
          if(e.preventDefault) {
            e.preventDefault();
          } else {
            e.returnValue = false;
          }
        }
      })
        .add({
          combo: 'mod+z',
          description: 'undo the last operation.',
          callback: function(e) {
            $scope.undo();
            if(e.preventDefault) {
              e.preventDefault();
            } else {
              e.returnValue = false;
            }
          }
        })
        .add ({
          combo: 'mod+y',
          description: 'redo the last operation.',
          callback: function(e) {
            $scope.redo();
            if(e.preventDefault) {
              e.preventDefault();
            } else {
              e.returnValue = false;
            }
          }
        });

      // Initial load of the topology
      topologyServices.dao.get({ topologyId: $scope.topologyId },
        function(result) {
          if(_.undefined(result.error)){
            $scope.refreshTopology(result.data, null, true);
            return;
          }
          //case there actually is an error
          topologyRecoveryServices.handleTopologyRecovery(result.data, $scope.topologyId, $scope.getLastOperationId(true)).then(function(recoveryResult){
            if(_.definedPath(recoveryResult, 'data')){
              $scope.refreshTopology(recoveryResult.data, null, true);
              return;
            }
          });
        });

      var AskSaveTopologyController = ['$scope', '$modalInstance',
        function($scope, $modalInstance) {
          $scope.save = function () {
            $modalInstance.close();
          };

          $scope.doNotSave = function () {
            $modalInstance.dismiss();
          };
        }];

      $scope.$on('$stateChangeStart',
        function(event, toState, toParams, fromState) {
          if($scope.topology.operations.length === 0 || $scope.topology.lastOperationIndex === -1) {
            // nothing to save
            return;
          }
          var getStateBasePath = function (state) {
            var lastIndexOfPoint = state.lastIndexOf('.');
            if(lastIndexOfPoint >= 0) {
              return state.substring(0, lastIndexOfPoint);
            } else {
              return state;
            }
          };
          if(getStateBasePath(toState.name) === getStateBasePath(fromState.name)) {
            // We are always inside editor
            return;
          }
          if($scope.skipStateChangeStart) {
            // Just skip once
            $scope.skipStateChangeStart = false;
            return;
          }
          event.preventDefault();
          var modalInstance = $modal.open({
            templateUrl: 'views/topology/editor_ask_save.html',
            controller: AskSaveTopologyController
          });

          var proceedToStateChange = function () {
            // Don't intercept the next one
            $scope.skipStateChangeStart = true;
            // Save
            $state.go(toState, toParams);
          };

          modalInstance.result.then(function() {
            $scope.save();
            proceedToStateChange();
          }, function() {
            proceedToStateChange();
          });
      });
    }
  ]);
}); // define
