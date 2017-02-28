define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/users/controllers/subjects_authorization_directive_ctrl');

  modules.get('a4c-security').directive('alienUserAuthorization', function () {
    return {
      templateUrl: 'views/users/users_authorization_directive.html',
      restrict: 'E',
      scope: {
        /*The resource to secure*/
        'resource': '=',

        /*the service for authorizations CRUD*/
        'service': '=',

        /*searchConfigBuilder, a function to pass to the add authorisation modal that should return an object like
          {
            url: // the url for searching
            params: // eventually params to complete the url
            useParams: // whether the remaining params after constructing the url shold be passed like param args or into the body of the request
        }
      */
        'searchConfigBuilder': '&',
      /*
        display for option in add auth modal
      */
        'displayCustomSearch': '='
      },
      link: function(scope) {
        scope.authorizeModalTemplateUrl = 'views/users/users_authorization_popup.html';

        scope.getId = function(user){
          return user.username;
        };

        scope.getRevokeParams = function(user){
          return {
            username: user.username
          };
        };
      }
    };
  });
});
