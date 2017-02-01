/* global SockJS, Stomp */

// Angular service to ease stomp web-socket communication.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');
  require('stomp');

  modules.get('a4c-common').factory('webSocketServices', ['$q', '$interval', '$log', '$window',
    function($q, $interval, $log, $window) {

      var socket = null;
      var subscribesRegistry = {};
      var debugEnabled = false;
      var debugStompEnabled = false;
      var subscribeQueue = [];
      var connected = false;
      var connecting = false;
      var connectingDefer;

      var initSockets = function() {
        if(connecting) {
          return connectingDefer.promise;
        }
        connecting = true;
        connectingDefer = $q.defer();

        socket = {};
        socket.client = new SockJS($window.location.pathname + 'rest/latest/alienEndPoint');
        socket.stomp = Stomp.over(socket.client);
        socket.stomp.debug = function(text) {
          if (debugStompEnabled) {
            $log.debug(text);
          }
        };
        socket.stomp.connect({}, function() {
          if (debugEnabled) {
            $log.debug('Connected to alien end point');
          }
          connected = true;
          connectingDefer.resolve(true);
        }, function(error) {
          $log.error('Could not connect to alien end point');
          $log.error(error);
          connected = false;
          connectingDefer.resolve(false);
        });

        socket.client.onclose = function() {
          $interval(function() {
            connected = false;
            if (Object.keys(subscribesRegistry).length > 0) {
              initSockets().then(function() {
                for (var topic in subscribesRegistry) {
                  if (subscribesRegistry.hasOwnProperty(topic)) {
                    doSubscribe(topic, subscribesRegistry[topic].listener);
                  }
                }
              });
            }
          }, 2000, 1);
        };
        return connectingDefer.promise;
      };

      var send = function(destination, message) {
        initSockets().then(function() {
          socket.stomp.send(destination, {}, angular.toJson(message));
        });
      };

      var doSubscribe = function(topic, listener) {
        subscribesRegistry[topic] = {
          'subscribe': socket.stomp.subscribe(topic, listener),
          'listener': listener
        };
      };

      var subscribe = function(topic, listener) {
        if (debugEnabled) {
          $log.debug('Subscribe to ' + topic);
        }
        if (!connected) {
          subscribeQueue.push({
            topic: topic,
            listener: listener
          });
          if (!_.defined(socket)) {
            initSockets().then(function() {
              for (var i = 0; i < subscribeQueue.length; i++) {
                doSubscribe(subscribeQueue[i].topic, subscribeQueue[i].listener);
              }
              subscribeQueue = [];
            });
          }
        } else {
          doSubscribe(topic, listener);
        }
      };

      var unSubscribe = function(topic) {
        if (debugEnabled) {
          $log.debug('UnSubscribe from ' + topic);
        }
        if (_.defined(subscribesRegistry[topic])) {
          subscribesRegistry[topic].subscribe.unsubscribe();
          delete subscribesRegistry[topic];
        }
      };

      var isTopicSubscribed = function(topic) {
        if (subscribeQueue.length > 0) {
          for (var i = 0; i < subscribeQueue.length; i++) {
            if (subscribeQueue[i].topic === topic) {
              return true;
            }
          }
        }
        var found = subscribesRegistry.hasOwnProperty(topic);
        return found;
      };

      return {
        'send': send,
        'subscribe': subscribe,
        'unSubscribe': unSubscribe,
        'isTopicSubscribed': isTopicSubscribed
      };
    }
  ]); // factory
}); // define
