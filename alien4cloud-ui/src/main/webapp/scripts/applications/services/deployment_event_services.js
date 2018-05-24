

define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/websocket_services');

  modules.get('a4c-applications').factory('deploymentEventServicesFactory', ['webSocketServices',
    function(webSocketServices) {

      return function(deploymentId, listeners) {

        var allListeners;

        var topicFactory = function(type) {
          return  '/topic/deployment-events/' + deploymentId + '/' + type;
        };

        var listenerFactory = function(type, listeners) {
          return function(event) {
            var parsedEvent = JSON.parse(event.body);
            for (var listenerId in listeners) {
              if (listeners.hasOwnProperty(listenerId)) {
                listeners[listenerId](type, parsedEvent);
              }
            }
          };
        };

        if (_.defined(listeners)) {
          allListeners = listeners;
          for (var type in allListeners) {
            if (allListeners.hasOwnProperty(type)) {
              var topicName = topicFactory(type);
              if (Object.keys(allListeners[type]).length > 0 && !webSocketServices.isTopicSubscribed(topicName)) {
                webSocketServices.subscribe(topicName, listenerFactory(type, allListeners[type]));
              }
            }
          }
        } else {
          allListeners = {
            paasdeploymentstatusmonitorevent: {},
            paasinstancestatemonitorevent: {},
            paasmessagemonitorevent: {},
            paasinstancestoragemonitorevent: {},
            paasworkflowmonitorevent: {}
          };
        }

        var close = function() {
          for (var type in allListeners) {
            if (allListeners.hasOwnProperty(type)) {
              var topicName = topicFactory(type);
              if (webSocketServices.isTopicSubscribed(topicName)) {
                webSocketServices.unSubscribe(topicName);
              }
            }
          }
          allListeners = null;
        };

        var subscribe = function(listenerId, type, callback) {
          if (allListeners.hasOwnProperty(type)) {
            allListeners[type][listenerId] = callback;
            var topicName = topicFactory(type);
            if (!webSocketServices.isTopicSubscribed(topicName)) {
              webSocketServices.subscribe(topicName, listenerFactory(type, allListeners[type]));
            }
          }
        };

        var unsubscribe = function(listenerId, type) {
          if (allListeners.hasOwnProperty(type)) {
            delete allListeners[type][listenerId];
            if (Object.keys(allListeners[type]).length === 0) {
              var topicName = topicFactory(type);
              if (webSocketServices.isTopicSubscribed(topicName)) {
                webSocketServices.unSubscribe(topicName);
              }
            }
          }
        };

        return {
          'allListeners': allListeners,
          'close': close,
          'subscribe': subscribe,
          'unsubscribe': unsubscribe
        };
      };
    }
  ]); // factory
}); // define
