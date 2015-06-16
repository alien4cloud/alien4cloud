define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common').directive('paginationDirective', function() {
    return {
      templateUrl : 'views/common/pagination_template.html',
      restrict : 'E',
      scope : {
        'paginationInfo' : '='
      }
    };
  });
}); // define
