define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common').directive('emptyPlaceHolder', function() {
    return {
      templateUrl: 'views/common/empty_place_holder.html',
      restrict: 'E',
      scope: {
        /*data to check for emptyness*/
        'data': '=',
        /*if provided, this message wil be displayed*/
        'message': '='
      },
      link: function(scope) {
        scope._ = _;
      }
    };
  });
}); // define
