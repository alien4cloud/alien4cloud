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
    $scope.importInfos = [];
    $scope.importing = {};
    $scope.id = 0;

    $scope.query = '';
    // onSearchCompleted is used as a callaback for the searchServiceFactory and triggered when the search operation is completed.
    $scope.onSearchCompleted = function(searchResult) {
      $scope.csarGits = searchResult.data.data;
    };
    // we have to insert the search service in the scope so it is available for the pagination directive.
    $scope.searchService = searchServiceFactory('rest/csarsgit', true, $scope, 20);
    $scope.search = function() { $scope.searchService.search(); };
    $scope.search(); // initialize

    $scope.triggerImport = function(id, url) {
      $scope.importing[id] = true;
      $scope.isImporting = true;
      csarGitService.fetch({
        id: id
      }, angular.toJson(id),
      function(result) {
        handleResult(result, url);
        $scope.importing[id] = false;
        $scope.isImporting = false;
        $scope.isImportingAll = false;
      }, function(error) {
        $scope.importInfos.push({
          'name': url,
          'infoType': statesToClasses.error,
          'error': {
            'code': status,
            'message': 'An Error has occurred on the server.'
          }
        });
        $scope.importing[id] = false;
        $scope.isImporting = false;
        $scope.isImportingAll = false;
      });
    };

    function processImportData(data, importResult) {
      if (_.defined(data.data) && data.data.length > 0) {
        _.each(data.data, function(parsingResult){
          // if(_.defined(parsingResult.context.parsingErrors) && parsingResult.context.parsingErrors.length > 0) {
          // push to the errors
          importResult.errors.push({
            'fileName': parsingResult.context.fileName,
            'parsingErrors': parsingResult.context.parsingErrors
          });
          // }
        });
      }
    };

    function handleResult(data, url) {
      var importResult = {
        'name': url,
        'progress': 100,
        'errors': []
      };
      // file is uploaded successfully and the server respond without error
      if (data.error === null) {
        importResult.infoType = statesToClasses.success;
        // there might be warnings. display them
        processImportData(data, importResult)
      } else {
        importResult.infoType = statesToClasses.error;
        if (_.undefined(data.data)) {
          importResult.otherError = {};
          importResult.otherError.code = data.error.code;
          importResult.otherError.message = data.error.message;
        } else {
          processImportData(data, importResult)
        }
      }
      $scope.importInfos.push(importResult);
      $scope.search();
    };

    $scope.closeUploadInfos = function(index) {
      $scope.importInfos.splice(index, 1);
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
  }]); // controller
}); // define
