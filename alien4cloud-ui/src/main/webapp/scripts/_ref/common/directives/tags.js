define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');

  require('scripts/common/services/suggestion_services');

  modules.get('a4c-common', []).directive('a4cTags', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/_ref/common/tags.html',
      controller: 'TagsCtrl',
      scope: {
        // Base url of the tags management controller
        baseUrl: '@',
        // Resource key and id
        resourceKey: '@',
        resourceId: '=',
        manager: '=',
        // Tags array
        tags: '='
      }
    };
  });

  const alienInternalTags = ['icon'];

  modules.get('a4c-common', []).controller('TagsCtrl', ['$scope', '$alresource', 'suggestionServices',
    function ($scope, $alresource, suggestionServices) {
      var tagService = $alresource($scope.baseUrl+'/:tagId');

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
      $scope.updateTag = function(tagName, tagValue) {
        var pathParams = {};
        pathParams[$scope.resourceKey] = $scope.resourceId;
        $scope.tagUpdateResult = tagService.create(pathParams, angular.toJson({
          tagKey: tagName,
          tagValue: tagValue
        }));
      };

      $scope.deleteTag = function(tag) {
        var index = $scope.tags.indexOf(tag);
        if (index >= 0) {
          var pathParams = {
            tagId: tag.name
          };
          pathParams[$scope.resourceKey] = $scope.resourceId;
          $scope.tagDeleteResult = tagService.remove(pathParams);
          // Remove the selected tag
          $scope.tags.splice(index, 1);
        }
      };

      /* Add new tags */
      var removeTagIfExists = function(tagName) {
        for (var i in $scope.tags) {
          if ($scope.tags.hasOwnProperty(i)) {
            var tag = $scope.tags[i];
            if (tag.name === tagName) {
              $scope.tags.splice(i, 1);
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
        $scope.updateTag(newTag.key, newTag.val);
        removeTagIfExists(newTag.key);
        $scope.tags.push({
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
    }]);
});
