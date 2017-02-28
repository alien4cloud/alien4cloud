define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/users/controllers/apps_authorization_directive_ctrl');
  require('scripts/users/controllers/apps_authorization_modal_ctrl');

  modules.get('a4c-security').directive('alienAppAuthorization', function () {
    return {
      templateUrl: 'views/users/apps_authorization_directive.html',
      restrict: 'E',
      scope: {
        /*The resource to secure*/
        'resource': '=',
        /*the service for applications authorizations CRUD*/
        'appService': '=',

        /*the service for environments authorizations CRUD*/
        'envService': '=',

        /*searchConfigBuilder, a function that should return an object like
          {
            url: // the url for searching
            params: // eventually params to complete the url
            useParams: // whether the remaining params after constructing the url shold be passed like param args or into the body of the request
        }
      */
        'searchConfigBuilder': '&',
        'displayCustomSearch': '='
      }
    };
  });
});
