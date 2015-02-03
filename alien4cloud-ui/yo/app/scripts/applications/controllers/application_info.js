/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('ApplicationInfosCtrl', ['$scope', '$state', 'alienAuthService', '$upload', '$translate',
  'applicationServices', 'suggestionServices', 'tagConfigurationServices', 'toaster', 'application', 'appEnvironments', 'defaultEnvironmentTab', '$timeout', '$stateParams',
  function($scope, $state, alienAuthService, $upload, $translate, applicationServices, suggestionServices, tagConfigurationServices, toaster, applicationResult, appEnvironments,
    defaultEnvironmentTab, $timeout, $stateParams) {

    /* Tag name with all letters a-Z and - and _ and no space */
    $scope.tagKeyPattern = /^[\-\w\d_]*$/;
    $scope.application = applicationResult.data;
    $scope.applicationId = $scope.application.id;
    var pageStateId = $state.current.name;

    $scope.isManager = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    $scope.isDeployer = alienAuthService.hasResourceRole($scope.application, 'DEPLOYMENT_MANAGER');
    $scope.isDevops = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
    $scope.isUser = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_USER');
    $scope.newAppName = $scope.application.name;

    $scope.isAllowedModify = UTILS.isDefinedAndNotNull($scope.application.topologyId) && ($scope.isManager || $scope.isDevops);
    $scope.envs = appEnvironments.environments;

    $scope.selectedTab = null;
    $scope.selectTab = function selectTab(applicationId, environmentId) {
      $scope.selectedTab = {
        appId: applicationId,
        envId: environmentId
      };
    };

    // select a default environment if any
    $timeout(function() {
      if (defaultEnvironmentTab !== null) {
        // console.log('Select this TAB >', defaultEnvironmentTab.name);
        document.getElementById('tab-env-' + defaultEnvironmentTab.name).click();
      }
    });

    // when scope change, stop current event listener
    $scope.$on('$destroy', function() {
      $scope.stopEvent();
    });

    // whatching $scope.selectTab changes
    $scope.$watch(function(scope) {
      if (UTILS.isDefinedAndNotNull(scope.selectedTab)) {
        return scope.selectedTab;
      }
      return 'SAME_TAB_ENV';
    }, function(newValue, oldValue) {
      if (newValue !== 'SAME_TAB_ENV') {
        $scope.stopEvent();
        $scope.setTopologyId(newValue.appId, newValue.envId, null).$promise.then(function(result) {
          // get informations from this topology
          $scope.processTopologyInformations(result.data);
          $scope.refreshInstancesStatuses(newValue.appId, newValue.envId, pageStateId);
        });
      }

    });

    // Upload handler
    $scope.doUpload = function(file) {
      $upload.upload({
        url: 'rest/applications/' + $scope.applicationId + '/image',
        file: file
      }).success(function(result) {
        $scope.application.imageId = result.data;
      });
    };

    $scope.onImageSelected = function($files) {
      var file = $files[0];
      $scope.doUpload(file);
    };

    $scope.updateProperties = function(propertyDefinition, propertyValue) {
      var updateApplicationPropertyObject = {
        'propertyId': propertyDefinition.id,
        'propertyDefinition': propertyDefinition,
        'propertyValue': propertyValue
      };

      return applicationServices.upsertProperty({
        applicationId: $scope.application.id
      }, angular.toJson(updateApplicationPropertyObject)).$promise;
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
      // Updating the app name if it 's not already used
      var applicationUpdateRequest = {};
      applicationUpdateRequest[fieldName] = fieldValue;
      return applicationServices.update({
        applicationId: $scope.application.id
      }, angular.toJson(applicationUpdateRequest), undefined).$promise.then(
        function() {
          // Success
        },
        function(errorResponse) {
          // Error
          return $translate('ERRORS.' + errorResponse.data.error.code);
        }
      );
    };

    $scope.loadConfigurationTag = function() {
      // filter only by target 'application'
      var filterApplication = {};
      filterApplication.target = [];
      filterApplication.target.push('application'); // or 'component'

      var searchRequestObject = {
        'query': '',
        'filters': filterApplication,
        'from': 0,
        'size': 50
      };

      tagConfigurationServices.search([], angular.toJson(searchRequestObject), function(successResult) {
        $scope.configurationProperties = successResult.data.data;
      });
    };
    $scope.loadConfigurationTag();
  }
]);
