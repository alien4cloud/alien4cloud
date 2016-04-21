define(function(require) {
  'use strict';

  var states = require('states');

  states.state('components.detail', {
    url: '/details/:id',
    template: '<ui-view></ui-view>',
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

  states.forward('components.detail', 'components.detail.info');
});
