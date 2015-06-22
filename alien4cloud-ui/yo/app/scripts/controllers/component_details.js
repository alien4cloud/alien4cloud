/* global UTILS */

'use strict';

angular.module('alienUiApp').controller('ComponentDetailsCtrl', ['alienAuthService', '$scope', '$resource', '$stateParams', 'componentTagUpdate', 'componentTagDelete', '$modal', 'suggestionServices',
  function(alienAuthService, $scope, $resource, $stateParams, componentTagUpdate, componentTagDelete, $modal, suggestionServices) {

    /* Possible roles : COMPONENTS_MANAGER, COMPONENTS_BROWSER,
     APPLICATIONS_MANAGER, ADMIN */
    var alienInternalTags = ['icon'];

    $scope.isManager = alienAuthService.hasRole('COMPONENTS_MANAGER');

    /* Tag name with all letters a-Z and  - and _ and no space*/
    $scope.tagKeyPattern = /^[\-\w\d_]*$/;

    var ComponentResource = $resource('rest/components/:componentId', {}, {
      method: 'GET',
      isArray: false,
      headers: {
        'Content-Type': 'application/json; charset=UTF-8'
      }
    });

    var RecommendationResource = $resource('rest/components/recommendation/:capability', {}, {
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

    var UnflagResource = $resource('rest/components/unflag', {}, {
      'unflag': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var topologyVersionResource = $resource('rest/topologies/:topologyId/version');
    
    ComponentResource.get({
      componentId: $stateParams.id
    }, function(successResult) {
      $scope.component = successResult.data;
    });

    $scope.currentDisplayed = {};
    //update currently displayed object
    $scope.updateCurrentDiplayed = function(name, interfass) {
      $scope.currentDisplayed.name = name;
      $scope.currentDisplayed.doc = interfass.description;
      $scope.currentDisplayed.operations = interfass.operations;
      $scope.currentDisplayed.urlToInclude = 'views/fragments/interfaceDetail.html';
    };

    /**
     * Confirm modal
     */

    var confirmRecommendationCtrl = ['$scope', '$modalInstance', 'confirmObject',
      function($scope, $modalInstance, confirmObject) {
        $scope.confirmObject = confirmObject;

        $scope.recommend = function() {
          $modalInstance.close();
        };

        $scope.cancel = function() {
          $modalInstance.dismiss('cancel');
        };
      }
    ];

    var openConfirmModal = function(confirmObject) {
      var modalInstance = $modal.open({
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
      $scope.tagUpdateResult = componentTagUpdate.upsert({
          componentId: $scope.component.id
        },
        angular.toJson(updateComponentTagObject)
      );
    };

    $scope.deleteTag = function(componentTag) {
      var index = $scope.component.tags.indexOf(componentTag);
      if (index >= 0) {
        $scope.tagDeleteResult = componentTagDelete.remove({
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

    /* Add new tags */
    $scope.addTag = function() {
      $scope.updateTag($scope.newTag.key, $scope.newTag.val);
      removeTagIfExists($scope.newTag.key);
      $scope.component.tags.push({
        name: $scope.newTag.key,
        value: $scope.newTag.val
      });
      resetTagForm();
    };

    var resetTagForm = function() {
      $scope.newTag = $scope.newTag || {};
      $scope.newTag.key = '';
      $scope.newTag.val = '';
    };

    //get the icon
    $scope.getIcon = UTILS.getIcon;

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
      $modal.open({
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

    $scope.openSimpleModal = function (content) {
      $modal.open({
        templateUrl: 'views/fragments/simple_modal.html',
        controller: ModalInstanceCtrl,
        resolve: {
          description: function () {
            return content;
          }
        }
      });
    };

    var ModalInstanceCtrl = ['$scope', '$modalInstance', 'description', function ($scope, $modalInstance, description) {
      $scope.title = 'MODAL.TITLE.PROPERTY';
      $scope.content = description;

      $scope.close = function () {
        $modalInstance.dismiss('close');
      };
    }];

    $scope.displaySubtitutionTopology = function(topologyId) {
      topologyVersionResource.get({
        topologyId: topologyId
      }, {}, function(result) {
        if (!result.error) {
          window.open('/#/topologytemplates/' + result.data.topologyTemplateId + '/topology/' + result.data.version);
        }
      });              
    };
    
  }
]);
