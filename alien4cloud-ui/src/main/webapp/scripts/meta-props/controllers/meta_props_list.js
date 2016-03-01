define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var confState = require('scripts/meta-props/controllers/meta_props_conf');
  require('scripts/meta-props/services/meta_props_conf_services');
  require('scripts/common/directives/generic_form');
  require('scripts/common/services/formdescriptor_services');

  require('scripts/common/directives/pagination');

  // register the state to access the metrics
  states.state('admin.metaprops', {
    url: '/metaproperties',
    template: '<ui-view/>',
    menu: {
      id: 'am.admin.metaprops',
      state: 'admin.metaprops',
      key: 'NAVADMIN.MENU_TAGS',
      icon: 'fa fa-tags',
      priority: 9900
    }
  });
  states.state('admin.metaprops.list', {
    url: '/list',
    templateUrl: 'views/meta-props/meta_props_list.html',
    controller: 'MetaPropsListCtrl'
  });
  states.forward('admin.metaprops', 'admin.metaprops.list');

  // have a dependency on both the generic-form and search.
  modules.get('a4c-metas', ['a4c-common', 'a4c-search']).controller('MetaPropsListCtrl',
    ['$scope', '$state', 'metapropConfServices', 'searchServiceFactory', 'formDescriptorServices',
    function($scope, $state, metapropConfServices, searchServiceFactory, formDescriptorServices) {
    $scope.tagCreationFormOpened = false;

    $scope.formTitle = 'Tag Configuration';

    $scope.query = '';

    $scope.onSearchCompleted = function(searchResult) {
      $scope.data = searchResult.data;
    };

    $scope.searchService = searchServiceFactory('rest/latest/metaproperties/search', false, $scope, 20);

    //first load
    $scope.searchService.search();

    formDescriptorServices.getTagConfigurationDescriptor.get({}, function(success) {
      $scope.objectDefinition = success.data;
    });
    $scope.saveTagConfiguration = function(config) {
      return metapropConfServices.save(config).then(function(response) {
        if (response.validationErrors) {
          return metapropConfServices.processValidationErrors(response.validationErrors);
        } else {
          $scope.tagCreationFormOpened = false;
          $scope.searchService.search();
        }
      });
    };

    $scope.cancelTagConfigurationCreation = function() {
      $scope.tagCreationFormOpened = false;
    };

    $scope.removeTagConfiguration = function(configId) {
      metapropConfServices.remove(configId).then(function() {
        $scope.searchService.search();
      });
    };

    $scope.goToTagConfiguration = function(id) {
      $state.go(confState, {id: id});
    };

    $scope.openTagCreationForm = function() {
      $scope.tagCreationFormOpened = true;
    };
  }]);
});
