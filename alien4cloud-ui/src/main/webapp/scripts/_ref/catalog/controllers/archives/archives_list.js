// components list: browse and search for components
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  // load other locations to manage archives
  require('scripts/components/services/csar');
  require('scripts/_ref/catalog/controllers/archives/archives_detail');
  require('scripts/_ref/catalog/controllers/archives/archives_git');
  require('scripts/_ref/catalog/controllers/archives/archives_new');
  require('scripts/common/directives/pagination');
  require('scripts/authentication/services/authservices');
  require('scripts/_ref/common/directives/search');

  states.state('catalog.archives.list', {
    url: '/list',
    templateUrl: 'views/_ref/catalog/archives/archives_list.html',
    controller: 'ArchivesListCtrl',
  });

  modules.get('a4c-catalog', ['ui.router', 'ui.bootstrap']).controller('ArchivesListCtrl', ['$scope', '$state', 'csarService', '$translate', 'toaster', '$uibModal', '$alresource', 'authService', 'searchServiceFactory',
    function ($scope, $state, csarService, $translate, toaster, $uibModal, $alresource, authService, searchServiceFactory) {

      $scope.writeWorkspaces = [];
      var isComponentManager = authService.hasOneRoleIn(['COMPONENT_MANAGER', 'ARCHITECT']);
      if (isComponentManager === true) {
        $scope.writeWorkspaces.push({id:'ALIEN_GLOBAL_WORKSPACE'});
      } else if (isComponentManager.hasOwnProperty('then')) {
        isComponentManager.then(function (hasRole) {
          if (hasRole) {
            $scope.writeWorkspaces.push({id:'ALIEN_GLOBAL_WORKSPACE'});
          }
        });
      }

      $scope.queryManager = {labelPrefix: 'COMPONENTS.CSAR.'};
      $scope.searchService = searchServiceFactory('rest/latest/csars/search', false, $scope.queryManager, 20);

      $scope.openCsar = function (csarId) {
        $state.go('catalog.archives.detail', {id: csarId});
      };

      // remove a csar
      $scope.remove = function (csarId) {
        csarService.getAndDeleteCsar.remove({
          csarId: csarId
        }, function (result) {
          var errorMessage = csarService.builtErrorResultList(result);
          if (errorMessage) {
            var title = $translate.instant('CSAR.ERRORS.' + result.error.code + '_TITLE');
            toaster.pop('error', title, errorMessage, 4000, 'trustedHtml', null);
          }
          // refresh csar list
          $scope.searchService.search();
        });
      };

      $scope.uploadSuccessCallback = function(){
        $scope.searchService.search();
      };

      //go to git import management
      $scope.goToGitImportManagement = function() {
        $state.go('catalog.archives.git');
      };

      var createTopologyArchiveResource = $alresource('/rest/latest/catalog/topologies/template');
      $scope.createTopologyArchive = function(topologyTemplate) {
        // create a new topologyTemplate from the given name, version and description.
        createTopologyArchiveResource.create([], angular.toJson(topologyTemplate), function(response) {
          // Response contains topology id
          if (_.defined(response.data)) {
          // the id is in form: archiveName:archiveVersion
            var tokens = response.data.trim().split(':');
            if (tokens.length > 1) {
              var archiveName = tokens[0];
              var archiveVersion = tokens[1];
              $state.go('editor_catalog_topology.editor', { archiveId: archiveName + ':' + archiveVersion });
            }
          }
        });
      };
      $scope.openNewArchiveModal = function(topology) {
        var modalConfiguration = {
          templateUrl: 'views/_ref/catalog/archives/archives_new.html',
          controller: 'NewArchiveTemplateCtrl',
          resolve: { topology: function() { return topology; } }
        };

        var modalInstance = $uibModal.open(modalConfiguration);
        modalInstance.result.then($scope.createTopologyArchive);
      };
    }
  ]); // controller
});
