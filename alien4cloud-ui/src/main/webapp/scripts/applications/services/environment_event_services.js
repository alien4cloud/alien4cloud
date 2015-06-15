define(function (require) {
  'use strict';

  var modules = require('modules');

  require('scripts/common/services/websocket_services');

  modules.get('a4c-applications').factory('environmentEventServicesFactory', ['webSocketServices',
    function(webSocketServices) {
    return function(applicationId, environment, callback) {
      var topicName = '/topic/environment-events/' + environment.id;

      // subscribe to environment status related events
      if (!webSocketServices.isTopicSubscribed(topicName)) {
        webSocketServices.subscribe(topicName, function(event) {
          callback(environment, JSON.parse(event.body));
        });
      }

      return {
        'close': function() {
          if (webSocketServices.isTopicSubscribed(topicName)) {
            webSocketServices.unSubscribe(topicName);
          }
        }
      };
    };
  }]); // factory
}); // define
