define(function (require) {
  'use strict';

  var modules = require('modules');

  require('scripts/common/services/websocket_services');

  /** Return a factory method that subscribe to topology editor events for the given topology. */
  modules.get('a4c-topology-editor').factory('topologyEditorEventFactory', ['webSocketServices',
    function(webSocketServices) {
      return function(topologyId, callback) {
        var topicName = '/topic/topology-editor/' + topologyId;

        // subscribe to environment status related events
        if (!webSocketServices.isTopicSubscribed(topicName)) {
          webSocketServices.subscribe(topicName, function(event) {
            callback(JSON.parse(event.body));
          });
        }

        return {
          send: function(destination, message) {
            webSocketServices.send(destination, message);
          },
          close: function() {
            if (webSocketServices.isTopicSubscribed(topicName)) {
              webSocketServices.unSubscribe(topicName);
            }
          }
        };
      };
    }
  ]); // factory
}); // define
