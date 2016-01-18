define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common').directive('emptyPlaceHolder', function() {
    return {
      templateUrl: 'views/common/empty_place_holder.html',
      restrict: 'E',
      scope: {
        'for': '=',
        'data': '=',
        'additionalMessage': '='
      },
      link: function(scope) {
        scope._ = _;
      }
    };
  });
}); // define
