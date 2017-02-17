define(function (require) {
  'use strict';

  var modules = require('modules');
  var authUtils = require('scripts/users/utils/authUtils.js');
  require('scripts/users/controllers/subjects_authorization_modal_ctrl');

  modules.get('a4c-security').directive('alienAuthorizeUsers', ['$uibModal', '$parse', function ($uibModal, $parse) {
    return authUtils.buildAuthorizeModalDirective('views/users/users_authorization_popup.html', 'rest/latest/users/search', 'username', $parse, $uibModal);
  }]);
});
