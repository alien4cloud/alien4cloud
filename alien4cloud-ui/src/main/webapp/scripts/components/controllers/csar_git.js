define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/components/services/csar_git');
  require('scripts/components/controllers/csar_git_new');
  require('scripts/components/controllers/csar_git_edit');
  require('scripts/common/directives/pagination');

  states.state('components.git', {
    url: '/git',
    templateUrl: 'views/components/csar_git.html',
    controller: 'CsarGitListCtrl',
    menu: {
      id: 'cm.components.git',
      state: 'components.git',
      key: 'NAVBAR.MENU_CSARS',
      icon: 'fa fa-git',
      priority: 30,
      roles: ['COMPONENTS_MANAGER']
    }
  });
  states.forward('components.csars', 'components.csars.list');

  /* Main CSAR search controller */
  modules.get('a4c-components', ['ui.router', 'ui.bootstrap']).controller('CsarGitListCtrl', ['$scope', '$modal', '$state', 'csarGitService', 'searchServiceFactory', '$translate', 'toaster',
  function($scope, $modal, $state, csarGitService, searchServiceFactory, $translate, toaster) {
    var statesToClasses = {
      'error': 'danger',
      'success': 'success',
      'progress': 'info'
    };
    $scope.uploadErrors = [];
    $scope.id = 0;

    $scope.query = '';
    // onSearchCompleted is used as a callaback for the searchServiceFactory and triggered when the search operation is completed.
    $scope.onSearchCompleted = function(searchResult) {
      $scope.csarGits = searchResult.data.data;
    };
    // we have to insert the search service in the scope so it is available for the pagination directive.
    $scope.searchService = searchServiceFactory('rest/csarsgit', true, $scope, 20);
    $scope.search = function() {$scope.searchService.search();};
    $scope.search(); // initialize

    $scope.triggerImport = function(id, url) {
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

    $scope.handleResult = function(result, url) {
      var state = statesToClasses.progress;
      var progress = 100;
      var isCollapsed = false;
      var index = $scope.uploadErrors.length;
      for(var j=0;j<result.data.length;j++){
        if(result.data[j].context.parsingErrors.length >0){
          state = statesToClasses.error;
          isCollapsed = true;
        }
        else{
          state = statesToClasses.success;
          isCollapsed = true;
        }
      }
      $scope.uploadErrors.push({
        'url': url,
        'isErrorBlocCollapsed':isCollapsed,
        'data': result,
        'infoType': state,
        'progress': progress
      }
    );
    $scope.search();
  }

  $scope.closeUploadInfos = function(index) {
    $scope.uploadErrors.splice(index, 1);
  };
  $scope.triggerImportAllCsarGit = function() {
    if (_.defined($scope.csarGits) && $scope.csarGits.length > 0) {
      for (var i=0; i<$scope.csarGits.length; i++) {
        $scope.triggerImport($scope.csarGits[i].id, $scope.csarGits[i].repositoryUrl);
      }
    }
    else{
      var titleError = $translate('CSAR.ERRORS.NO_DATA.HEADER');
      var bodyError=$translate('CSAR.ERRORS.NO_DATA.BODY')
      toaster.pop('note', titleError, bodyError, 4000, 'trustedHtml',null);
    }
  };

  $scope.removeCsarGit = function(id) {
    csarGitService.remove({
      id: id
    }, function() {
      // refresh csargit list
      $scope.search();
    });
  };

  $scope.openNewCsarGitTemplate = function() {
    var modalInstance = $modal.open({
      templateUrl: 'views/components/csar_git_new.html',
      controller: 'NewCsarGitController',
      scope: $scope
    });
    modalInstance.result.then(function(csarGitTemplate) {
      csarGitService.create([], angular.toJson(csarGitTemplate), function(successResponse) {
        var errorMessage = successResponse;
        if (errorMessage.error != null) {
          var title = $translate('CSAR.ERRORS.' + errorMessage.error.code + '_TITLE');
          toaster.pop('error', title, errorMessage.message, 4000, 'trustedHtml', null);
        }
        $scope.search();
        $scope.id = 0;
      });
    });
  };

  $scope.openCsarGit = function(csar) {
    var modalInstance = $modal.open({
      templateUrl: 'views/components/csar_git_edit.html',
      controller: 'EditCsarGitController',
      scope: $scope,
      resolve:{
        csar: function () {
          return csar;
        }
      }
    });
    modalInstance.result.then(function(DTOObject) {
      var JsonId = angular.toJson(DTOObject.id);
      csarGitService.update({id: DTOObject.id },angular.toJson(DTOObject.dto), function(successResponse) {
        var errorMessage = successResponse;
        if (errorMessage.error != null) {
          var title = $translate('CSAR.ERRORS.' + errorMessage.error.code + '_TITLE');
          toaster.pop('error', title, errorMessage.message, 4000, 'trustedHtml', null);
        }
        $scope.search();
      });
    });
  };
}
]); // controller
}); // define
