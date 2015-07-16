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
    menu: {
      id: 'cm.components.csars.list',
      state: 'components.csars.list',
      key: 'NAVBAR.MENU_CSARS',
      icon: 'fa fa-archive',
      priority: 20
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
          'username': 'empty',
          'password': 'empty',
          'repositoryUrl': csarGit.url,
          'importLocations': locations
        };
        $modalInstance.close(csargitDTO);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
        $scope.id=0;
      };

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
        location.subPath='';
        location.branchId='';
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

      $scope.id=0;
      $scope.searchCsarsGit = function() {
        var searchRequestObject = {
          'query': $scope.queryCsarGit,
          'from': 0,
          'size': 50
        };
        $scope.csarGitSearchResult = csarGitService.search([],angular.toJson(searchRequestObject));
      };

      $scope.triggerImport = function(id) {
        $scope.isImporting = true;
        csarGitService.fetch({
          id: id
        }, angular.toJson(id),
         function(result) {
           $scope.buildParsingErrorToast(result);
           $scope.isImporting = false;
           $scope.isImportingAll = false;
       }, function(error) {
           $scope.isImporting = false;
           $scope.isImportingAll = false;
       });
      };

      $scope.buildParsingErrorToast=function(result){
        var responseBody='';
        var responseSuccessBody='';
        var bodyImport;
        var title ;
        var titleSuccess ;
        for(var i=0;i<result.data.data.length;i++){
         if(result.data.data[i].context.parsingErrors.length > 0){
           if(result.data.data[i].context.parsingErrors[0].errorCode !='TOPOLOGY_DETECTED'){
            responseBody+='<ul>';
            responseBody+=' <li>'
            responseBody+=result.data.data[i].result.name +' '+result.data.data[i].context.parsingErrors[0].errorCode;
            responseBody+=' </li>';
            responseBody+=result.data.data[i].context.parsingErrors[0].note;
            responseBody+=' </ul>';
            title  = $translate('CSAR.ERRORS.IMPORT');
            }
          }
          else{
            responseSuccessBody+='<ul>';
            responseSuccessBody+=' <li>';
            responseSuccessBody+=result.data.data[i].context.fileName;
            responseSuccessBody+=' </li>';
            responseSuccessBody+=' </ul>';
            titleSuccess=$translate('CSAR.IMPORT_SUCCESS');
          }
        }
        if(responseBody != '' && responseSuccessBody != ''){
          toaster.pop('warning', title, responseBody, 4000, 'trustedHtml', null);
          toaster.pop('success', titleSuccess ,responseSuccessBody, 4000, 'trustedHtml', null);
        }
        else{
          if(responseBody!=''){
            toaster.pop('warning', title, responseBody, 4000, 'trustedHtml', null);
          }
          if(responseSuccessBody!=''){
            toaster.pop('success', titleSuccess ,responseSuccessBody, 4000, 'trustedHtml', null);
          }
        }
        $scope.search();
      }

      $scope.triggerImportAllCsarGit = function(data) {
        if (data.length > 0) {
          for (var i=0; i<data.length; i++) {
            $scope.triggerImport(data[i].id);
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
        console.log(id);
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
            console.log(errorMessage);
            if (errorMessage.error != null) {
              var title = $translate('CSAR.ERRORS.' + errorMessage.error.code + '_TITLE');
              toaster.pop('error', title, errorMessage.message, 4000, 'trustedHtml', null);
            }
           $scope.searchCsarsGit();
           $scope.id=0;
          });
        });
      };

      // init search
      $scope.search();
      $scope.searchCsarsGit();
    }
  ]); // controller
}); // define
