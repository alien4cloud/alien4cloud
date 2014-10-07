/* global UTILS */
'use strict';

angular.module('alienUiApp').factory('toscaService',
  function() {
    var containerType = 'tosca.capabilities.Container';
    var hostedOnType = 'tosca.relationships.HostedOn';

    return {
      /**
      * Checks if a capability type is a Container capability.
      *
      * @param capabilityTypeName The name of the capability type.
      * @param capabilityTypes A map of available capability types. It must contains the actual capabilityTypeName.
      * @return true if the type is a tosca container type.
      */
      isContainerType: function(capabilityTypeName, capabilityTypes) {
        return this.isOneOfType([containerType], capabilityTypeName, capabilityTypes);
      },

      /**
      * Checks if a relationshipType is an instance of hosted on.
      *
      * @param relationshipTypeName The name of the relationship type to check.
      * @param relationshipTypes A map of available relationships types. It must contains the actual relationshipTypeName.
      */
      isHostedOnType: function(relationshipTypeName, relationshipTypes) {
        return this.isOneOfType([hostedOnType], relationshipTypeName, relationshipTypes);
      },

      /**
      * Check if a type is a one of requested types.
      *
      * @param requestedTypes Array that contains type names, one of them has to be the candidate type or one of it's parent type.
      * @param candidateTypeName The name of the candidate type.
      * @param typesMap A map of available types that should contains the candidate type.
      * @return true if the type is one of the requested type, false if not.
      */
      isOneOfType: function(requestedTypes, candidateTypeName, typesMap) {
        for(var i=0; i<requestedTypes.length; i++) {
          var validTarget = requestedTypes[i];
          if(candidateTypeName === validTarget ||
            (UTILS.isDefinedAndNotNull(typesMap[candidateTypeName]) &&
              UTILS.arrayContains(typesMap[candidateTypeName].derivedFrom, validTarget))) {
            return true;
          }
        }
        return false;
      }
    };
  } // function
);
