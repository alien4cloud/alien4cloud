define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  require('scripts/meta-props/services/meta_props_conf_services');
  require('scripts/common/directives/generic_form');
  require('scripts/common/services/formdescriptor_services');

  var state = 'admin.metaprops.detail';

  states.state(state, {
    url: '/:id',
    templateUrl: 'views/meta-props/meta_props_conf.html',
    controller: 'MetaPropsConfCtrl'
  });

  modules.get('a4c-metas', ['a4c-common']).controller('MetaPropsConfCtrl',
    ['$scope', '$stateParams', '$state', 'metapropConfServices', 'formDescriptorServices',
    function($scope, $stateParams, $state, metapropConfServices, formDescriptorServices) {
      $scope.refreshDetails = function() {
        metapropConfServices.get($stateParams.id).then(function(config) {
          $scope.configuration = config;
          $scope.formTitle = config.name;
        });
      };

      $scope.refreshDetails();

      formDescriptorServices.getTagConfigurationDescriptor.get({}, function(success) {
        $scope.objectDefinition = success.data;
      });

      $scope.saveTagConfiguration = function(config) {
        return metapropConfServices.save(config).then(function(response) {
          return metapropConfServices.processValidationErrors(response.validationErrors);
        });
      };

      $scope.removeTagConfiguration = function() {
        metapropConfServices.remove($stateParams.id).then(function() {
          $state.go('admin.metaprops.list');
        });
      };

      $scope.cancelTagConfigurationCreation = function() {
        $state.go('admin.metaprops.list');
      };
    }]);

  return state;
});
