define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/applications/services/application_services');
  require('scripts/applications/services/application_environment_services');
  require('scripts/applications/services/application_version_services');
  require('scripts/meta-props/directives/meta_props_display');

  require('scripts/deployment/directives/display_outputs');

  states.state('applications.detail.info', {
    url: '/infos',
    templateUrl: 'views/applications/application_infos.html',
    controller: 'ApplicationInfosCtrl',
    menu: {
      id: 'am.applications.info',
      state: 'applications.detail.info',
      key: 'NAVAPPLICATIONS.MENU_INFO',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationInfosCtrl',
    ['$scope', '$state', 'authService', 'Upload', '$translate', 'applicationServices', 'suggestionServices', 'toaster', 'application', 'appEnvironments',
    function($scope, $state, authService, $upload, $translate, applicationServices, suggestionServices,  toaster, applicationResult, appEnvironments) {
      $scope.applicationServices = applicationServices;

      /* Tag name with all letters a-Z and - and _ and no space */
      $scope.tagKeyPattern = /^[\-\w\d_]*$/;
      $scope.application = applicationResult.data;
      var pageStateId = $state.current.name;

      $scope.isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
      $scope.isDeployer = authService.hasResourceRole($scope.application, 'DEPLOYMENT_MANAGER');
      $scope.isDevops = authService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
      $scope.isUser = authService.hasResourceRole($scope.application, 'APPLICATION_USER');
      $scope.newAppName = $scope.application.name;

      $scope.isAllowedModify = _.defined($scope.application.topologyId) && ($scope.isManager || $scope.isDevops);
      $scope.appEnvironments = appEnvironments;

      $scope.setEnvironment = function setEnvironment(environmentId) {
        // select an environment and register a callback in case the env has changed.
        appEnvironments.select(environmentId, function() {
          $scope.stopEvent(); // stop to listen for instance events
          if(appEnvironments.selected.status !== 'UNDEPLOYED') {
            // If the application is deployed then get informations to display.
            $scope.processDeploymentTopologyInformation().$promise.then(function() {
              $scope.refreshInstancesStatuses($scope.application.id, appEnvironments.selected.id, pageStateId);
            });
          }
        }, true);
      };

      $scope.$on('$destroy', function() { // routing to another page
        $scope.stopEvent(); // stop to listen for instance events
      });

      // Image upload
      $scope.doUpload = function(file) {
        $upload.upload({
          url: 'rest/latest/applications/' + $scope.application.id + '/image',
          file: file
        }).success(function(result) {
          $scope.application.imageId = result.data;
        });
      };
      $scope.onImageSelected = function($files) {
        var file = $files[0];
        $scope.doUpload(file);
      };

      var alienInternalTags = ['icon'];
      /* Restrict tags visibility */
      $scope.isInternalTag = function(tag) {
        var internalTag = false;
        for (var i = 0; i < alienInternalTags.length; i++) {
          if (alienInternalTags[i] === tag) {
            return true;
          }
        }
        return internalTag;
      };

      /* Update / Delete application tags */
      $scope.updateTag = function(applicationTagName, applicationTagValue) {
        var updateapplicationTagObject = {
          'tagKey': applicationTagName,
          'tagValue': applicationTagValue
        };
        $scope.tagUpdateResult = applicationServices.tags.upsert({
          applicationId: $scope.application.id
        }, angular.toJson(updateapplicationTagObject));
      };

      $scope.deleteTag = function(applicationTag) {
        var index = $scope.application.tags.indexOf(applicationTag);
        if (index >= 0) {
          $scope.tagDeleteResult = applicationServices.tags.remove({
            applicationId: $scope.application.id,
            tagKey: applicationTag.name
          });
          // Remove the selected tag
          $scope.application.tags.splice(index, 1);
        }
      };

      /* Add new tags */

      var removeTagIfExists = function(tagName) {
        for (var i in $scope.application.tags) {
          if ($scope.application.tags.hasOwnProperty(i)) {
            var tag = $scope.application.tags[i];
            if (tag.name === tagName) {
              $scope.application.tags.splice(i, 1);
              return;
            }
          }
        }
      };

      var resetTagForm = function(newTag) {
        newTag.key = '';
        newTag.val = '';
      };

      $scope.addTag = function(newTag) {
        $scope.application.tags = $scope.application.tags || [];
        $scope.updateTag(newTag.key, newTag.val);
        removeTagIfExists(newTag.key);
        $scope.application.tags.push({
          name: newTag.key,
          value: newTag.val
        });
        resetTagForm(newTag);
      };

      /**
       * TAG SUGGESTION
       */
      var getTagNameSuggestions = function(keyword) {
        return suggestionServices.tagNameSuggestions(keyword);
      };

      $scope.tagSuggestion = {
        get: getTagNameSuggestions,
        waitBeforeRequest: 0,
        minLength: 2
      };

      $scope.removeApplication = function(applicationId) {
        applicationServices.remove({
          applicationId: applicationId
        }, function(response) {
          if (!response.error && response.data === true) {
            $state.go('applications.list');
          } else {
            // toaster message
            toaster.pop('error', $translate.instant('APPLICATIONS.ERRORS.DELETE_TITLE'), $translate.instant('APPLICATIONS.ERRORS.DELETING_FAILED'), 4000, 'trustedHtml', null);
          }
        });
      };

      $scope.updateApplication = function(fieldName, fieldValue) {
        var applicationUpdateRequest = {};
        if(_.undefined(fieldValue)) {
          fieldValue = '';
        }
        applicationUpdateRequest[fieldName] = fieldValue;
        return applicationServices.update({
          applicationId: $scope.application.id
        }, angular.toJson(applicationUpdateRequest), undefined).$promise.then(
          function() { // Success
            // reload the current application info page after update
            $state.go($state.current, {}, {reload: true});
          },
          function(errorResponse) {// Error
            return $translate.instant('ERRORS.' + errorResponse.data.error.code);
          }
        );
      };
    }
  ]);
});
