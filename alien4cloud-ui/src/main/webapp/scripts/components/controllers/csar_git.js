define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/components/services/csar_git');
  require('scripts/components/controllers/csar_git_crud');
  require('scripts/common/directives/pagination');

  states.state('components.git', {
    url: '/git',
    templateUrl: 'views/components/csar_git.html',
    controller: 'CsarGitListCtrl',
    menu: {
      id: 'cm.components.git',
      state: 'components.git',
      key: 'NAVBAR.MENU_CSARS_GIT',
      icon: 'fa fa-git',
      priority: 40,
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
      $scope.searchResult = searchResult.data;
    };
    // we have to insert the search service in the scope so it is available for the pagination directive.
    $scope.searchService = searchServiceFactory('rest/latest/csarsgit', true, $scope, 20);
    $scope.search = function() { $scope.searchService.search(); };
    $scope.search(); // initialize

    $scope.triggerImport = function(id, url) {
      $scope.importing[id] = true;
      csarGitService.fetch({
        id: id
      }, angular.toJson(id),
      function(result) {
        handleResult(result, url);
        delete $scope.importing[id];
      }, function() {
        $scope.importInfos.push({
          'name': url,
          'infoType': statesToClasses.error,
          'error': {
            'code': status,
            'message': 'An Error has occurred on the server.'
          }
        });
        delete $scope.importing[id];
      });
    };

    $scope.triggerImportAllCsarGit = function() {
      var gitRepositories = $scope.searchResult.data;
      if (_.defined(gitRepositories) && gitRepositories.length > 0) {
        for (var i=0; i<gitRepositories.length; i++) {
          $scope.triggerImport(gitRepositories[i].id, gitRepositories[i].repositoryUrl);
        }
      }
      else{
        var titleError = $translate.instant('CSAR.ERRORS.NO_DATA.HEADER');
        var bodyError=$translate.instant('CSAR.ERRORS.NO_DATA.BODY');
        toaster.pop('note', titleError, bodyError, 4000, 'trustedHtml',null);
      }
    };

    $scope.isImporting = function(){
      return !_.isEmpty($scope.importing);
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
    }

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
        processImportData(data, importResult);
      } else {
        importResult.infoType = statesToClasses.error;
        if (_.undefined(data.data)) {
          importResult.otherError = {};
          importResult.otherError.code = data.error.code;
          importResult.otherError.message = data.error.message;
        } else {
          processImportData(data, importResult);
        }
      }
      $scope.importInfos.push(importResult);
      $scope.search();
    }

    $scope.closeUploadInfos = function(index) {
      $scope.importInfos.splice(index, 1);
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
        templateUrl: 'views/components/csar_git_crud.html',
        controller: 'CsarGitCrudController',
        scope: $scope,
        resolve: {
          gitRepository: function () {
            return {
              'username': undefined,
              'password': undefined,
              'repositoryUrl': undefined,
              'importLocations': [],
              'storedLocally': false
            };
          }
        }
      });
      modalInstance.result.then(function(csarGitTemplate) {
        csarGitService.create([], angular.toJson(csarGitTemplate), function(successResponse) {
          var errorMessage = successResponse;
          if (errorMessage.error !== null) {
            var title = $translate.instant('CSAR.ERRORS.' + errorMessage.error.code + '_TITLE');
            toaster.pop('error', title, errorMessage.message, 4000, 'trustedHtml', null);
          }
          $scope.search();
          $scope.id = 0;
        });
      });
    };

    $scope.openCsarGit = function(csar) {
      var modalInstance = $modal.open({
        templateUrl: 'views/components/csar_git_crud.html',
        controller: 'CsarGitCrudController',
        scope: $scope,
        resolve: {
          gitRepository: function () {
            return csar;
          }
        }
      });
      modalInstance.result.then(function(gitRepo) {
        csarGitService.update({id: gitRepo.id },angular.toJson(gitRepo), function(successResponse) {
          var errorMessage = successResponse;
          if (errorMessage.error !== null) {
            var title = $translate.instant('CSAR.ERRORS.' + errorMessage.error.code + '_TITLE');
            toaster.pop('error', title, errorMessage.message, 4000, 'trustedHtml', null);
          }else{
            //Do this instead of $scope.search(), to avoid useless REST call
            var index = _.indexOf($scope.searchResult.data, csar);
            $scope.searchResult.data.splice(index, 1, gitRepo);
          }
        });
      });
    };
  }]); // controller
}); // define
