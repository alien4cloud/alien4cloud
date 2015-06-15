
angular.module('alienUiApp').factory('runtimeColorsService', function() {
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
    unknown: '#2183b2'
  };
});
