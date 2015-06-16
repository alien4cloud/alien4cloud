define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common').directive('paginationDirective', function() {
    return {
      templateUrl : 'views/fragments/pagination_template.html',
      restrict : 'E',
      scope : {
        'paginationInfo' : '='
      }
    };
  });
}); // define
