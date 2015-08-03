/*
Utility service that provides functions to get maps of relationships by types.
*/
define(function (require) {
  'use strict';

  var modules = require('modules');
  var $ = require('jquery');
  var _ = require('lodash');

  require('scripts/tosca/services/tosca_service');

  modules.get('a4c-tosca').factory('toscaRelationshipsService', ['toscaService',
    function(toscaService) {
      return {
        /**
        * Get all hosted on or network relationships on a given node template.
        */
        getHostedOnRelationships: function(nodeTemplate, relationshipTypes) {
          var hostedOnRelationships = toscaService.getRelationships(nodeTemplate, function(relationship) {
            return toscaService.isHostedOnType(relationship.type, relationshipTypes);
          });
          return hostedOnRelationships;
        },

        /**
        * Get all attached to relationships on a given node template.
        */
        getAttachedToRelationships: function(nodeTemplate, relationshipTypes) {
          var relationships = toscaService.getRelationships(nodeTemplate, function(relationship) {
            return toscaService.isAttachedToType(relationship.type, relationshipTypes);
          });
          return relationships;
        },

        /**
        * Get all attached to relationships on a given node template.
        */
        getNetworkRelationships: function(nodeTemplate, relationshipTypes) {
          var relationships = toscaService.getRelationships(nodeTemplate, function(relationship) {
            return toscaService.isNetworkType(relationship.type, relationshipTypes);
          });
          return relationships;
        },

        /**
        * Get all depends on to relationships on a given node template.
        */
        getDependsOnRelationships: function(nodeTemplate, relationshipTypes) {
          var dependsOnRelationships = toscaService.getRelationships(nodeTemplate, function(relationship) {
            return !toscaService.isHostedOnType(relationship.type, relationshipTypes) && !toscaService.isNetworkType(relationship.type, relationshipTypes);
          });
          return dependsOnRelationships;
        }
      }; // return
    } // function
  ]); // factory
});// define
