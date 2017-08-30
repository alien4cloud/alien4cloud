define(function(require) {
  'use strict';

  var states = require('states');

  require('scripts/_ref/catalog/controllers/components/components_info');

  states.state('catalog.components.detail', {
    url: '/detail/:id',
    template: '<ui-view/>',
    resolve: {
      component: ['componentService', '$stateParams',
        function(componentService, $stateParams) {
          return componentService.get({
            componentId: $stateParams.id
          }).$promise.then(function(result){
            return result.data;
          });
        }]
    }
  });

  states.forward('catalog.components.detail', 'catalog.components.detail.info');
});
