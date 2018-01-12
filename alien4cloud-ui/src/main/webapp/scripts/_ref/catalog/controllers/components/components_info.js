define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('catalog.components.detail.info', {
    url: '/info',
    templateUrl: 'views/_ref/catalog/components/components_info.html',
    controller: 'ComponentInfoCtrl',
    menu: {
      state: 'catalog.components.detail.info',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT_INFO',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  require('scripts/authentication/services/authservices');
  require('scripts/common/filters/strings');
  require('scripts/common/services/suggestion_services');
  require('scripts/common/services/property_suggestion_services');
  require('scripts/common/directives/simple_modal');
  require('scripts/components/services/tags');
  require('scripts/components/controllers/csar_explorer');
  require('scripts/common/directives/info.js');

  modules.get('a4c-catalog', ['ngResource', 'ui.bootstrap', 'ui.router', 'a4c-auth']).controller('ComponentInfoCtrl',
    ['authService', '$scope', '$resource', '$state', '$stateParams', 'componentTagService', '$uibModal', 'suggestionServices', 'toscaService', 'component', 'breadcrumbsService', '$filter',
    function(authService, $scope, $resource, $state, $stateParams, componentTagService, $uibModal, suggestionServices, toscaService, component, breadcrumbsService, $filter) {

      breadcrumbsService.putConfig({
        state : 'catalog.components.detail.info',
        text: function(){
          return $filter('splitAndGet')($scope.component.elementId, '.', 'last') + ':' +$scope.component.archiveVersion;
        }
      });

      var alienInternalTags = ['icon'];
      // users with role COMPONENTS_MANAGER are allowed to add archives.
      $scope.isManager = authService.hasRole('COMPONENTS_MANAGER');

      /* Tag name with all letters a-Z and  - and _ and no space*/
      $scope.tagKeyPattern = /^[\-\w\d_]*$/;
      $scope._ = _;
      $scope.component = component;

      var RecommendationResource = $resource('rest/latest/components/recommendation/:capability', {}, {
        'getRecommended': {
          method: 'GET',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        },
        'recommend': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }

      });

      var UnflagResource = $resource('rest/latest/components/unflag', {}, {
        'unflag': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      /**
       * Confirm modal
       */
      var confirmRecommendationCtrl = ['$scope', '$uibModalInstance', 'confirmObject',
        function($scope, $uibModalInstance, confirmObject) {
          $scope.confirmObject = confirmObject;

          $scope.recommend = function() {
            $uibModalInstance.close();
          };

          $scope.cancel = function() {
            $uibModalInstance.dismiss('cancel');
          };
        }
      ];

      //do the effective recommendation
      var doRecommendation = function(capability) {
        var recommendationRequest = {
          'componentId': $stateParams.id,
          'capability': capability
        };
        RecommendationResource.recommend([], angular.toJson(recommendationRequest),
        function(successResult) {
          //for refreshing the ui
          $scope.component = successResult.data;
        });
      };

      var openConfirmModal = function(confirmObject) {
        var modalInstance = $uibModal.open({
          templateUrl: 'confirmChoice.html',
          controller: confirmRecommendationCtrl,
          resolve: {
            confirmObject: function() {
              return confirmObject;
            }
          }
        });

        modalInstance.result.then(function() {
          // trigger the recommendation
          doRecommendation(confirmObject.capability);
        });
      };


      /**
       *recommend this component as default for a given capability
       *
       */
      $scope.recommendedComponentForCapa = [];


      //do this when capacity already have a recommended component
      var handleWhenAlreadyRecommended = function(capability, data) {
        var confirmObject = {
          'componentId': data.id,
          'capability': capability
        };
        openConfirmModal(confirmObject);
      };

      //decide if to recommend or not?
      var recommendOrNot = function(capability, succesGetResult) {
        //if a component is already recommended for this capability
        if (succesGetResult.data) {
          $scope.recommendedComponentForCapa[capability] = succesGetResult.data;
          handleWhenAlreadyRecommended(capability, succesGetResult.data);
          return;
        }

        //if not then recommend this component
        doRecommendation(capability);
      };

      //trigger the recommendation logic
      // first Check for existing recommended component for this capability,
      // check the internal var to prevent useless REST call
      //and then decide to recommend this component or not
      $scope.recommendForThisCapability = function(capability) {

        if (!$scope.recommendedComponentForCapa[capability]) {
          RecommendationResource.getRecommended({
            capability: capability
          }, function(successResult) {
            recommendOrNot(capability, successResult);
          });
        } else {
          handleWhenAlreadyRecommended(capability, $scope.recommendedComponentForCapa[capability]);
        }

      };

      $scope.unflagAsDefaultForThisCapability = function(capability) {
        var recommendationRequest = {
          'componentId': $stateParams.id,
          'capability': capability
        };
        UnflagResource.unflag([], angular.toJson(recommendationRequest), function(successResult) {
          //for refreshing the ui
          $scope.component = successResult.data;
        });
        delete $scope.recommendedComponentForCapa[capability];
      };

      /**
       * check if this component is default for a capability
       */
      $scope.isADefaultCapability = function(capability) {
        if ($scope.component.defaultCapabilities) {
          return ($scope.component.defaultCapabilities.indexOf(capability) >= 0);
        }
      };

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

      /* Update / Delete component tags */
      $scope.updateTag = function(componentTag, componentTagValue) {
        var updateComponentTagObject = {
          'tagKey': componentTag,
          'tagValue': componentTagValue
        };
        $scope.tagUpdateResult = componentTagService.upsert({
            componentId: $scope.component.id
          },
          angular.toJson(updateComponentTagObject)
        );
      };

      $scope.deleteTag = function(componentTag) {
        var index = $scope.component.tags.indexOf(componentTag);
        if (index >= 0) {
          $scope.tagDeleteResult = componentTagService.remove({
            componentId: $scope.component.id,
            tagKey: componentTag.name
          });
          //Remove the selected filter
          $scope.component.tags.splice(index, 1);
        }
      };

      var removeTagIfExists = function(tagName) {
        for (var i in $scope.component.tags) {
          if ($scope.component.tags.hasOwnProperty(i)) {
            var tag = $scope.component.tags[i];
            if (tag.name === tagName) {
              $scope.component.tags.splice(i, 1);
              return;
            }
          }
        }
      };

      var resetTagForm = function() {
        $scope.newTag = $scope.newTag || {};
        $scope.newTag.key = '';
        $scope.newTag.val = '';
      };

      /* Add new tags */
      $scope.addTag = function(newTag) {
        $scope.newTag = newTag;
        $scope.updateTag($scope.newTag.key, $scope.newTag.val);
        removeTagIfExists($scope.newTag.key);
        $scope.component.tags.push({
          name: $scope.newTag.key,
          value: $scope.newTag.val
        });
        resetTagForm();
      };

      //get the icon
      $scope.getIcon = toscaService.getIcon;

      /**
       *
       * TAG SUGGESTION
       *
       */

      var getTagNameSuggestions = function(keyword) {
        return suggestionServices.tagNameSuggestions(keyword);
      };

      $scope.tagSuggestion = {
        get: getTagNameSuggestions,
        waitBeforeRequest: 0,
        minLength: 2
      };

      $scope.openArchiveModal = function(scriptReference) {
        var openOnFile = scriptReference ? scriptReference : null;
        $uibModal.open({
          templateUrl: 'views/components/csar_explorer.html',
          controller: 'CsarExplorerController',
          windowClass: 'searchModal',
          resolve: {
            archiveName: function() {
              return $scope.component.archiveName;
            },
            archiveVersion: function() {
              return $scope.component.archiveVersion;
            },
            openOnFile: function() {
              return openOnFile;
            }
          }
        });
      };

      $scope.displaySubtitutionTopology = function(archiveName, archiveVersion) {
        $state.go('topologycatalog.csar', {
          archiveName: archiveName,
          archiveVersion: archiveVersion
        });
      };
    }
  ]); // controller
});// define
