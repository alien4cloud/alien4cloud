define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common').directive('aceSave', function() {
    return {
      templateUrl: 'views/common/ace_save_button.html',
      controller: 'aceSaveCtrl',
      restrict: 'E',
      scope: {
        'size': '=',
        'saveCallback': '&',
        'content': '=',
        'disable': '&?'
      }
    };
  });

  modules.get('a4c-common').controller('aceSaveCtrl', ['$scope',  function($scope) {
    $scope.isDisabled = function(){
      return ($scope.disable?$scope.disable():false) || (_.get($scope,'content.new') ||'') === (_.get($scope,'content.old')||'');
    };
  }]); // controller
}); // define
