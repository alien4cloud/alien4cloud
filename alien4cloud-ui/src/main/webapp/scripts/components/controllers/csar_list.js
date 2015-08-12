define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/components/services/csar');
  require('scripts/components/services/csar_git');
  require('scripts/components/controllers/csar_details');
  require('scripts/common/directives/pagination');

  states.state('components.csars', {
    url: '/csars',
    template: '<ui-view/>',
    controller: 'LayoutCtrl',
    menu: {
      id: 'cm.components.csars.list',
      state: 'components.csars.list',
      key: 'NAVBAR.MENU_CSARS',
      icon: 'fa fa-archive',
      priority: 20,
      roles: ['COMPONENTS_MANAGER']
    }
  });
  states.state('components.csars.list', {
    url: '/list',
    templateUrl: 'views/components/csar_list.html',
    controller: 'CsarListCtrl'
  });
  states.forward('components.csars', 'components.csars.list');

  var NewCsarGitController = ['$scope', '$modalInstance',
    function($scope, $modalInstance) {
      $scope.csarGitTemplate = {};
      $scope.create = function(csarGit) {
      var locations = $scope.importLocation;
      var csargitDTO = {
          'username': csarGit.username,
          'password': csarGit.password,
          'repositoryUrl': csarGit.url,
          'importLocations': locations
        };
        $modalInstance.close(csargitDTO);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
        $scope.id = 0;
      };

      $scope.removeCsarLocation = function(subPath){
        for (var i=0;i<$scope.importLocation.length;i++) {
          var loc = $scope.importLocation[i];
            if (loc.subPath === subPath) {
              $scope.importLocation.splice(i, 1);
              return;
            }
        }
      }
      var removeIfLocationExists=function(location){
        for (var i=0;i<$scope.importLocation.length;i++) {
          var loc = $scope.importLocation[i];
            if (loc.subPath === location.subPath && loc.branchId === location.branchId) {
              $scope.importLocation.splice(i, 1);
              return;
            }
        }
      };

      var resetLocationForm=function(location){
        location.subPath = '';
        location.branchId = '';
      };

      $scope.addLocation=function(location){
        $scope.importLocation = $scope.importLocation || [];
        removeIfLocationExists(location);
        $scope.importLocation.push({
          subPath: location.subPath,
          branchId: location.branchId
        });
        resetLocationForm(location);
      };
    }
  ];

  var EditCsarGitController = ['$scope', '$modalInstance','csar',
    function($scope, $modalInstance,csar) {
      $scope.csarGitTemplate = {};
      $scope.url = csar.repositoryUrl;
      $scope.username = csar.username;
      $scope.password = csar.password;
      $scope.id = csar.id;

      $scope.update = function(url,username,password) {
      var csargitDTO = {
          'repositoryUrl': url,
          'username': username,
          'password': password,
        };
        var id = $scope.id;
        var dtoData = {
          'dto':csargitDTO,
          'id':id
        }
        $modalInstance.close(dtoData);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ];

  /* Main CSAR search controller */
  modules.get('a4c-components', ['ui.router', 'ui.bootstrap']).controller('CsarListCtrl', ['$scope', '$modal', '$state', 'csarService', 'csarGitService', '$translate', 'toaster',
   function($scope, $modal, $state, csarService, csarGitService, $translate, toaster) {
      $scope.search = function() {
        var searchRequestObject = {
          'query': $scope.query,
          'from': 0,
          'size': 50
        };
        $scope.csarSearchResult = csarService.searchCsar.search([], angular.toJson(searchRequestObject));
      };
      var statesToClasses = {
        'error': 'danger',
        'success': 'success',
        'progress': 'info'
      };
      $scope.uploadErrors = [];
      $scope.id = 0;
      $scope.searchCsarsGit = function() {
        var searchRequestObject = {
          'query': $scope.queryCsarGit,
          'from': 0,
          'size': 50
        };
        $scope.csarGitSearchResult = csarGitService.search([],angular.toJson(searchRequestObject));
      };

      $scope.triggerImport = function(id,url) {
        $scope.isImporting = true;
        csarGitService.fetch({
          id: id
        }, angular.toJson(id),
         function(result) {
           $scope.handleResult(result,url);
           $scope.isImporting = false;
           $scope.isImportingAll = false;
       }, function(error) {
           $scope.isImporting = false;
           $scope.isImportingAll = false;
       });
      };

      $scope.handleResult = function(result,url){
        var state = statesToClasses.progress;
        var progress = 100;
        var isCollapsed=false;
        var index = $scope.uploadErrors.length;
          for(var j=0;j<result.data.data.length;j++){
            if(result.data.data[j].context.parsingErrors.length >0){
              state = statesToClasses.error;
            }
            else{
              state = statesToClasses.success;
              isCollapsed = true;

            }
        }
        $scope.uploadErrors.push({
          'url': url,
          'isErrorBlocCollapsed': isCollapsed,
          'data': result.data,
          'infoType': state,
          'progress': progress
          }
        );
        $scope.search();
      }

      $scope.closeUploadInfos = function(index) {
        $scope.uploadErrors.splice(index, 1);
      };
      $scope.triggerImportAllCsarGit = function(data) {
        if (data.length > 0) {
          for (var i=0; i<data.length; i++) {
            $scope.triggerImport(data[i].id,data[i].repositoryUrl);
          }
        }
        else{
          var titleError = $translate('CSAR.ERRORS.NO_DATA.HEADER');
          var bodyError=$translate('CSAR.ERRORS.NO_DATA.BODY')
          toaster.pop('note', titleError, bodyError, 4000, 'trustedHtml',null);
        }
      };

      $scope.openCsar = function(csarId) {
        $state.go('components.csars.csardetail', { csarId: csarId });
      };

      // remove a csar
      $scope.remove = function(csarId) {
        csarService.getAndDeleteCsar.remove({
          csarId: csarId
        }, function(result) {
          var errorMessage = csarService.builtErrorResultList(result);
          if (errorMessage) {
            var title = $translate('CSAR.ERRORS.' + result.error.code + '_TITLE');
            toaster.pop('error', title, errorMessage, 4000, 'trustedHtml', null);
          }
          // refresh csar list
          $scope.search();
        });
      };

      $scope.removeCsarGit = function(id) {
        csarGitService.remove({
          id: id
        }, function() {
          // refresh csargit list
          $scope.searchCsarsGit();
        });
      };

      $scope.openNewCsarGitTemplate = function() {
        var modalInstance = $modal.open({
          templateUrl: 'newCsarGit.html',
          controller: NewCsarGitController,
          scope: $scope
        });
        modalInstance.result.then(function(csarGitTemplate) {
          csarGitService.create([], angular.toJson(csarGitTemplate), function(successResponse) {
            var errorMessage = successResponse;
            if (errorMessage.error != null) {
              var title = $translate('CSAR.ERRORS.' + errorMessage.error.code + '_TITLE');
              toaster.pop('error', title, errorMessage.message, 4000, 'trustedHtml', null);
            }
           $scope.searchCsarsGit();
           $scope.id=0;
          });
        });
      };

      $scope.openCsarGit = function(csar) {
        var modalInstance = $modal.open({
          templateUrl: 'editCsarGit.html',
          controller: EditCsarGitController,
          scope: $scope,
            resolve:{
              csar: function () {
              return csar;
            }
          }
        });
        modalInstance.result.then(function(DTOObject) {
          var JsonId=angular.toJson(DTOObject.id);
          csarGitService.update({id: DTOObject.id },angular.toJson(DTOObject.dto), function(successResponse) {
              var errorMessage = successResponse;
              if (errorMessage.error != null) {
                var title = $translate('CSAR.ERRORS.' + errorMessage.error.code + '_TITLE');
                toaster.pop('error', title, errorMessage.message, 4000, 'trustedHtml', null);
              }
            $scope.searchCsarsGit();
          });
        });
      };

      // init search
      $scope.search();
      $scope.searchCsarsGit();
    }
  ]); // controller
}); // define
