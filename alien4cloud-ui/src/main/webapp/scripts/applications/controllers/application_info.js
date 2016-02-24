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
    resolve: {
      defaultEnvironmentTab: ['appEnvironments', function(appEnvironments) {
        // return the first deployed env found or null
        var onlyDeployed = appEnvironments.environments.filter(function deployed(element) {
          return element.status === 'DEPLOYED';
        });
        return onlyDeployed.length > 0 ? onlyDeployed[0] : null;
      }]
    },
    menu: {
      id: 'am.applications.info',
      state: 'applications.detail.info',
      key: 'NAVAPPLICATIONS.MENU_INFO',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationInfosCtrl',
    ['$scope', '$state', 'authService', '$upload', '$translate', 'applicationServices', 'suggestionServices', 'toaster', 'application', 'appEnvironments',
    function($scope, $state, authService, $upload, $translate, applicationServices, suggestionServices,  toaster, applicationResult, appEnvironments) {
      $scope.applicationServices = applicationServices;

      /* Tag name with all letters a-Z and - and _ and no space */
      $scope.tagKeyPattern = /^[\-\w\d_]*$/;
      $scope.application = applicationResult.data;
      $scope.applicationId = $scope.application.id;
      var pageStateId = $state.current.name;

      $scope.isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
      $scope.isDeployer = authService.hasResourceRole($scope.application, 'DEPLOYMENT_MANAGER');
      $scope.isDevops = authService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
      $scope.isUser = authService.hasResourceRole($scope.application, 'APPLICATION_USER');
      $scope.newAppName = $scope.application.name;

      $scope.isAllowedModify = _.defined($scope.application.topologyId) && ($scope.isManager || $scope.isDevops);
      $scope.envs = appEnvironments.environments;

      $scope.selectedTab = null;
      $scope.selectTab = function selectTab(applicationId, environmentId) {
        $scope.selectedTab = {
          appId: applicationId,
          envId: environmentId
        };

      };

      // when scope change, stop current event listener
      $scope.$on('$destroy', function() {
        $scope.stopEvent();
      });

      // whatching $scope.selectTab changes
      $scope.$watch('selectedTab', function(newValue, oldValue) {
        if (newValue !== oldValue) {
          $scope.stopEvent();
          $scope.setTopologyId(newValue.appId, newValue.envId, null).$promise.then(function(result) {
            // get informations from this topology
            $scope.processTopologyInformations(result.data).$promise.then(function() {
              $scope.refreshInstancesStatuses(newValue.appId, newValue.envId, pageStateId);
            });
          });
        }
      });

      // Upload handler
      $scope.doUpload = function(file) {
        $upload.upload({
          url: 'rest/v1/applications/' + $scope.applicationId + '/image',
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

      var resetTagForm = function(newTag) {
        newTag.key = '';
        newTag.val = '';
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
            toaster.pop('error', $translate('APPLICATIONS.ERRORS.DELETE_TITLE'), $translate('APPLICATIONS.ERRORS.DELETING_FAILED'), 4000, 'trustedHtml', null);
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
            return $translate('ERRORS.' + errorResponse.data.error.code);
          }
        );
      };
    }
  ]);
});
