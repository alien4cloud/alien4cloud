define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-styles').factory('runtimeColorsService', function() {
    return {
      creating: '#afe9af',
      created: '#87de87',
      configuring: '#5fd35f',
      configured: '#37c837',
      starting: '#2ca02c',
      started: '#217821',
      stopping: '#cccccc',
      stopped: '#999999',
      deleting: '#666666',
      deleted: '#333333',
      warning: '#ff7f2a',
      error: '#ff0000',
      unknown: '#2183b2',
      groupColorCss: function(topology, groupId) {
        // 10 colors max (cf. topology-svg.scss)
        return 'groupColor-' + topology.groups[groupId].index % 10;
      }
    };
  });
});
