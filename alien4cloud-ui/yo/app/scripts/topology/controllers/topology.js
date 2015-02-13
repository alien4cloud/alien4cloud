/* global UTILS, jsyaml */

'use strict';

angular.module('alienUiApp').controller('TopologyCtrl', ['alienAuthService', '$scope', '$modal', 'topologyJsonProcessor', 'topologyServices', 'resizeServices', '$q', '$translate', '$upload', 'componentService', 'nodeTemplateService', '$timeout', 'applicationVersionServices', 'appVersions', 'topologyId', 'toscaService', 'toscaCardinalitiesService',
  function(alienAuthService, $scope, $modal, topologyJsonProcessor, topologyServices, resizeServices, $q, $translate, $upload, componentService, nodeTemplateService, $timeout, applicationVersionServices, appVersions, topologyId, toscaService, toscaCardinalitiesService) {
    $scope.view = 'RENDERED';

    // TODO : when topology templates edition with use also version, remove this IF statement
    if (UTILS.isDefinedAndNotNull(appVersions)) {
      // default version loading
      $scope.appVersions = appVersions;
      $scope.selectedVersion = $scope.appVersions[0];
      $scope.topologyId = $scope.selectedVersion.topologyId;
    } else {
      // TODO : remove this part when apVersion will be given in state 'topologytemplates.detail.topology'
      $scope.appVersions = appVersions;
      // $scope.selectedVersion = null;
      $scope.topologyId = topologyId;
    }

    // TODO remove this!!!
    $scope.editRights = true;
    // $scope.hasYamlEditor = true;
    $scope.editorContent = '';
    var inputOutputKeys = ['inputProperties', 'outputProperties', 'outputAttributes', 'inputArtifacts'];
    var regexPatternn = '^[\\w_]*$';

    // Size management
    var borderSpacing = 10;
    var border = 2;
    var detailDivWidth = 450;
    var widthOffset = detailDivWidth + (3 * borderSpacing) + (2 * border);
    var heightOffset = 156;
    function onResize(width, height) {
      $scope.dimensions = {
        width: width,
        height: height
      };
      $scope.$apply();
    }

    resizeServices.register(onResize, widthOffset, heightOffset);

    $scope.dimensions = {
      height: resizeServices.getHeight(heightOffset),
      width: resizeServices.getWidth(widthOffset)
    };
    // end size management

    var COMPUTE_TYPE = 'tosca.nodes.Compute';

    var setSelectedVersionByName = function(name) {
      $scope.selectedVersionName = name;
      for (var i = 0; i < $scope.appVersions.length; i++) {
        if ($scope.appVersions[i].version === $scope.selectedVersionName) {
          $scope.selectedVersion = $scope.appVersions[i];
          break;
        }
      }
    };

    var updateSelectedVersionName = function(applicationVersion) {
      if (UTILS.isDefinedAndNotNull(applicationVersion)) {
        applicationVersionServices.getFirst({
          applicationId: $scope.application.id
        }, function updateSelectedVersion(result) {
          $scope.selectedVersionName = result.data.version;
          setSelectedVersionByName($scope.selectedVersionName);
        });
      }
    };
    updateSelectedVersionName($scope.appVersions);

    $scope.changeVersion = function(selectedVersion) {
      setSelectedVersionByName(selectedVersion.version);
      $scope.topologyId = selectedVersion.topologyId;
      topologyServices.dao.get({
        topologyId: $scope.topologyId
      }, function(successResult) {
        refreshTopology(successResult.data);
      });
      selectTab('components-search');
    };

    var refreshTopology = function(topologyDTO, selectedNodeTemplate) {
      for (var nodeId in topologyDTO.topology.nodeTemplates) {
        if (topologyDTO.topology.nodeTemplates.hasOwnProperty(nodeId)) {
          topologyDTO.topology.nodeTemplates[nodeId].name = nodeId;
        }
      }
      $scope.topology = topologyDTO;

      // enrich objects to add maps for the fields that are currently mapped as array of map entries.
      topologyJsonProcessor.process($scope.topology);

      fillBounds($scope.topology.topology);
      initInputsOutputs($scope.topology.topology);
      $scope.editorContent = jsyaml.safeDump($scope.topology.topology);
      if (UTILS.isDefinedAndNotNull(selectedNodeTemplate)) {
        fillNodeSelectionVars($scope.topology.topology.nodeTemplates[selectedNodeTemplate]);
      } else {
        $scope.selectedNodeTemplate = null;
      }
    };

    // Topology can comes from application OR topology template
    topologyServices.dao.get({
      topologyId: $scope.topologyId
    }, function(successResult) {
      refreshTopology(successResult.data);
    });

    $scope.isNodeTemplateCollapsed = false;
    $scope.isPropertiesCollapsed = false;
    $scope.isRelationshipsCollapsed = false;
    $scope.isRelationshipCollapsed = false;
    $scope.isArtifactsCollapsed = false;
    $scope.isArtifactCollapsed = false;
    $scope.isRequirementsCollapsed = false;
    $scope.isCapabilitiesCollapsed = false;

    $scope.checkMapSize = function(map) {
      return angular.isDefined(map) && map !== null && Object.keys(map).length > 0;
    };

    // do effectively add the relationship
    var doAddRelationship = function(openedOnElementName, relationshipResult, requirementName, requirementType) {
      var addRelationshipNodeTemplate = $scope.topology.topology.nodeTemplates[openedOnElementName];
      if (!addRelationshipNodeTemplate.relationships) {
        addRelationshipNodeTemplate.relationships = {};
      }

      var relationshipTemplate = {
        type: relationshipResult.relationship.elementId,
        target: relationshipResult.target,
        requirementName: requirementName,
        requirementType: requirementType,
        targetedCapabilityName: relationshipResult.targetedCapabilityName
      };
      var addRelationshipRequest = {
        relationshipTemplate: relationshipTemplate,
        archiveName: relationshipResult.relationship.archiveName,
        archiveVersion: relationshipResult.relationship.archiveVersion
      };
      topologyServices.relationshipDAO.add({
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: openedOnElementName,
        relationshipName: relationshipResult.name
      }, angular.toJson(addRelationshipRequest), function(result) {
        // for refreshing the ui
        refreshTopology(result.data, openedOnElementName);
      });
    };

    $scope.openSearchRelationshipModal = function(openedOnElementName, openedOnElement, requirementName, requirement, targetNodeTemplateName) {
      if (!openedOnElement.requirementsMap[requirementName].value.canAddRel.yes) {
        console.debug('Should be able to add relationships');
        return;
      }

      $scope.sourceElement = openedOnElement;
      $scope.sourceElementName = openedOnElementName;
      $scope.requirementName = requirementName;
      $scope.requirement = requirement;
      $scope.targetNodeTemplateName = targetNodeTemplateName;

      var modalInstance = $modal.open({
        templateUrl: 'views/fragments/search/search_relationship_modal.html',
        controller: 'SearchRelationshipCtrl',
        windowClass: 'searchModal',
        scope: $scope
      });

      modalInstance.result.then(function(relationshipResult) {
        doAddRelationship(openedOnElementName, relationshipResult, requirementName, requirement.type);
      });
    };

    /**
     * NODE TEMPLATE ADD / DELETE SHOW
     */
    var nodeTemplateNameExists = function(toCheck) {
      for (var name in $scope.topology.topology.nodeTemplates) {
        if (name === toCheck) {
          return true;
        }
      }
      return false;
    };

    var nodeTemplateNameFromType = function(type) {
      var baseName = toscaService.simpleName(type);

      var i = 1;
      var tempName = baseName;
      while (nodeTemplateNameExists(tempName)) {
        i++;
        tempName = baseName + '_' + i;
      }

      return tempName;
    };

    $scope.nodeTypeSelected = function(nodeType, hostNodeName) {
      var nodeTemplName = nodeTemplateNameFromType(nodeType.elementId);
      doAddNodeTemplate(nodeTemplName, nodeType, hostNodeName);
    };

    $scope.onComponentDragged = function(e) {
      var nodeType = angular.fromJson(e.source);
      var evt = e.event;
      var hostNodeName = null;
      if (evt.target.hasAttribute('node-template-id')) {
        hostNodeName = evt.target.getAttribute('node-template-id');
      }
      $scope.nodeTypeSelected(nodeType, hostNodeName);
    };

    // refresh a node name in relationships
    function refreshNodeNameInRelationships(oldNodeName, newName) {
      for (var nodeName in $scope.topology.topology.nodeTemplates) {
        var node = $scope.topology.topology.nodeTemplates[nodeName];
        var relationships = node.relationships;
        if (UTILS.isDefinedAndNotNull(relationships)) {
          for (var relationshipId in relationships) {

            var rel = relationships[relationshipId];
            var oldRelationshipId = toscaService.generateRelationshipName(rel.type, oldNodeName);
            if (relationshipId === oldRelationshipId) {
              var newRelationshipId = toscaService.generateRelationshipName(rel.type, newName);
              delete relationships[relationshipId];
              relationships[newRelationshipId] = rel;
            }

            if (rel.target === oldNodeName) {
              rel.target = newName;
            }
          }
        }
      }
    }

    function autoOpenRelationshipModal(sourceNodeTemplateName, targetNodeTemplateName) {
      var sourceNodeTemplate = $scope.topology.topology.nodeTemplates[sourceNodeTemplateName];
      var targetNodeTemplate = $scope.topology.topology.nodeTemplates[targetNodeTemplateName];
      if (UTILS.isDefinedAndNotNull(sourceNodeTemplate) && UTILS.isDefinedAndNotNull(targetNodeTemplate)) {
        // let's try to find the requirement / for now we just support hosted on but we should improve that...
        var requirementName = nodeTemplateService.getContainerRequirement(sourceNodeTemplate, $scope.topology.nodeTypes, $scope.topology.relationshipTypes, $scope.topology.capabilityTypes);
        $scope.openSearchRelationshipModal(sourceNodeTemplateName, sourceNodeTemplate, requirementName, sourceNodeTemplate.requirementsMap[requirementName].value, targetNodeTemplateName);
      }
    }

    // do effectively add the node template
    function doAddNodeTemplate(nodeTemplateName, selectedNodeType, targetNodeTemplateName) {
      var nodeTemplateRequest = {
        'name': nodeTemplateName,
        'indexedNodeTypeId': selectedNodeType.id
      };
      topologyServices.nodeTemplate.add({
        topologyId: $scope.topology.topology.id
      }, angular.toJson(nodeTemplateRequest), function(result) {
        // for refreshing the ui
        refreshTopology(result.data, nodeTemplateName);
        if (targetNodeTemplateName) {
          autoOpenRelationshipModal(nodeTemplateName, targetNodeTemplateName);
        }
      });
    }

    $scope.nodeNameObj = {}; // added to support <tabset> "childscope issue"
    function fillNodeSelectionVars(nodeTemplate) {
      $scope.selectedNodeTemplate = nodeTemplate;
      $scope.selectedProperties = nodeTemplate.properties;
      $scope.nodeNameObj.val = nodeTemplate.name;
      $scope.selectionabstract = $scope.topology.nodeTypes[nodeTemplate.type].abstract;
    }

    $scope.selectNodeTemplate = function(newSelectedName, oldSelectedName) {
      // select the "Properties" <TAB> to see selected node details
      // selectTab('nodetemplate-details');
      document.getElementById('nodetemplate-details').click();

      $timeout(function() {
        if (oldSelectedName) {
          var oldSelected = $scope.topology.topology.nodeTemplates[oldSelectedName];
          if (oldSelected) {
            oldSelected.selected = false;
          }
        }

        var newSelected = $scope.topology.topology.nodeTemplates[newSelectedName];
        newSelected.selected = true;

        fillNodeSelectionVars(newSelected);
        $scope.$apply();
      });
    };

    $scope.deleteNodeTemplate = function(nodeTemplName) {
      topologyServices.nodeTemplate.remove({
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: nodeTemplName
      }, function(result) {
        // for refreshing the ui
        refreshTopology(result.data);
        selectTab('components-search');
      });
    };

    function selectTab(tabId) {
      // $timeout(callback) will wait until the current digest cycle (if any) is done, then execute your code, then run at the end a full $apply
      // without $timeout => ALREADY IN DIGEST CYCLE error
      $timeout(function() {
        document.getElementById(tabId).click();
      });
    }

    /**
     * UPDATE NODE TEMPLATE
     */

    /* Update node template name */
    $scope.updateNodeName = function(newName) {
      // Update only when the name has changed
      $scope.nodeTempNameEditError = null;
      if ($scope.selectedNodeTemplate.name !== newName) {
        var nodeNameRegEx = new RegExp(regexPatternn, 'g');
        var valid = nodeNameRegEx.test(newName);
        //verify the name format
        if (!valid) {
          $scope.nodeTempNameEditError = {
            code: 'APPLICATIONS.TOPOLOGY.NODETEMPLATE_NAME_ERROR'
          };
          return ' ';
        }

        topologyServices.nodeTemplate.updateName({
          topologyId: $scope.topology.topology.id,
          nodeTemplateName: $scope.selectedNodeTemplate.name,
          newName: newName
        }, function(resultData) {
          if (resultData.error === null) {
            var oldNodeName = $scope.selectedNodeTemplate.name;
            var nodeTemplate = $scope.topology.topology.nodeTemplates[oldNodeName];
            delete $scope.topology.topology.nodeTemplates[$scope.selectedNodeTemplate.name];
            $scope.selectedNodeTemplate.name = newName;
            $scope.topology.topology.nodeTemplates[newName] = nodeTemplate;
            refreshNodeNameInRelationships(oldNodeName, newName);
            refreshInOutMaps($scope.topology.topology, oldNodeName, newName);
          }
        }, function() {
          $scope.nodeNameObj.val = $scope.selectedNodeTemplate.name;
        });
      } // if end
    };

    /* Update properties of a node template */
    $scope.updateProperty = function(propertyDefinition, propertyValue) {
      var propertyName = propertyDefinition.name;
      if (propertyValue === $scope.topology.topology.nodeTemplates[$scope.selectedNodeTemplate.name].properties[propertyName]) {
        return;
      }
      var updatePropsObject = {
        'propertyName': propertyName,
        'propertyValue': propertyValue
      };

      var updatedNodeTemplate = $scope.selectedNodeTemplate;
      return topologyServices.nodeTemplate.updateProperty({
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: $scope.selectedNodeTemplate.name
      }, angular.toJson(updatePropsObject), function() {
        // update the selectedNodeTemplate properties locally
        updatedNodeTemplate.propertiesMap[propertyName].value = propertyValue;
      }).$promise;
    };

    // Add property / artifact / attributes to input list
    var initInOutMap = function(topology, nodeTemplateName, key) {
      if (angular.isDefined(topology[key])) {
        if (!angular.isDefined(topology[key][nodeTemplateName])) {
          topology[key][nodeTemplateName] = [];
        }
      } else {
        topology[key] = {};
        topology[key][nodeTemplateName] = [];
      }
    };

    var initInOutMaps = function(topology, nodeTemplateName) {
      inputOutputKeys.forEach(function(key) {
        initInOutMap(topology, nodeTemplateName, key);
      });
    };

    var refreshInOutMap = function(topology, oldName, newName, key) {
      var map = topology[key][oldName];
      delete topology[key][oldName];
      topology[key][newName] = map;
    };

    var refreshInOutMaps = function(topology, oldName, newName) {
      inputOutputKeys.forEach(function(key) {
        refreshInOutMap(topology, oldName, newName, key);
      });
    };

    var initInputsOutputs = function(topology) {
      if (topology.nodeTemplates) {
        Object.keys(topology.nodeTemplates).forEach(function(nodeTemplateName) {
          initInOutMaps(topology, nodeTemplateName);
        });
      }
    };

    var toggleInputOutput = function(propertyName, inputOrOutput, type) {

      var nodeTemplateName = $scope.selectedNodeTemplate.name;
      var topology = $scope.topology.topology;
      var params = {
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: nodeTemplateName
      };

      if (type === 'property') {
        params.propertyName = propertyName;
      }
      if (type === 'attribute') {
        params.attributeName = propertyName;
      }

      var inputIndex = topology[inputOrOutput][nodeTemplateName].indexOf(propertyName);

      if (inputIndex < 0) {
        // add input property
        topologyServices.nodeTemplate[inputOrOutput].add(
          params,
          function(successResult) {
            if (!successResult.error) {
              topology[inputOrOutput][nodeTemplateName].push(propertyName);
            } else {
              console.debug(successResult.error);
            }
          },
          function(errorResult) {
            console.debug(errorResult);
          }
        );
      } else {
        // remove input
        topologyServices.nodeTemplate[inputOrOutput].remove(
          params,
          function(successResult) {
            if (!successResult.error) {
              topology[inputOrOutput][nodeTemplateName].splice(inputIndex, 1);
            } else {
              console.debug(successResult.error);
            }
          },
          function(errorResult) {
            console.debug(errorResult);
          }
        );
      }
    };

    $scope.toggleInputProperty = function(propertyName) {
      toggleInputOutput(propertyName, 'inputProperties', 'property');
    };

    $scope.toggleOutputProperty = function(propertyName) {
      toggleInputOutput(propertyName, 'outputProperties', 'property');
    };

    $scope.toggleOutputAttribute = function(attributeName) {
      toggleInputOutput(attributeName, 'outputAttributes', 'attribute');
    };

    $scope.openSimpleModal = function(modalTitle, modalContent) {
      $modal.open({
        templateUrl: 'views/fragments/simple_modal.html',
        controller: ModalInstanceCtrl,
        resolve: {
          title: function() {
            return modalTitle;
          },
          content: function() {
            return modalContent;
          }
        }
      });
    };

    var ModalInstanceCtrl = ['$scope', '$modalInstance', 'title', 'content', function($scope, $modalInstance, title, content) {
      $scope.title = title;
      $scope.content = content;

      $scope.close = function() {
        $modalInstance.dismiss('close');
      };
    }];

    $scope.updateInputArtifactList = function(artifactName) {

      var nodeTemplateName = $scope.selectedNodeTemplate.name;
      var topology = $scope.topology.topology;
      var artifactInput = {
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: nodeTemplateName,
        artifactName: artifactName
      };

      var inputIndex = topology.inputArtifacts[nodeTemplateName].indexOf(artifactName);

      if (inputIndex < 0) {
        // add input artifact
        topologyServices.nodeTemplate.artifactInput.add(
          artifactInput,
          function(successResult) {
            if (!successResult.error) {
              topology.inputArtifacts[nodeTemplateName].push(artifactName);
            }
          },
          function(errorResult) {
            console.debug(errorResult);
          }
        );
      } else {
        // remove input artifact
        topologyServices.nodeTemplate.artifactInput.remove(
          artifactInput,
          function(successResult) {
            if (!successResult.error) {
              topology.inputArtifacts[nodeTemplateName].splice(inputIndex, 1);
            } else {
              console.debug(successResult.error);
            }
          },
          function(errorResult) {
            console.debug(errorResult);
          }
        );
      }

    };

    // Upload handler
    $scope.doUpload = function(file, artifactId) {
      var uploadNodeTemplate = $scope.selectedNodeTemplate;
      var artifact = uploadNodeTemplate.artifacts[artifactId];
      if (UTILS.isUndefinedOrNull($scope.uploads)) {
        $scope.uploads = {};
      }
      $scope.uploads[artifactId] = {
        'isUploading': true,
        'type': 'info'
      };
      $upload.upload({
        url: 'rest/topologies/' + $scope.topology.topology.id + '/nodetemplates/' + uploadNodeTemplate.name + '/artifacts/' + artifactId,
        file: file
      }).progress(function(evt) {
        $scope.uploads[artifactId].uploadProgress = parseInt(100.0 * evt.loaded / evt.total);
      }).success(function(success) {
        artifact.artifactRef = success.data;
        artifact.artifactName = file.name;
        $scope.uploads[artifactId].isUploading = false;
        $scope.uploads[artifactId].type = 'success';
      }).error(function(data, status) {
        $scope.uploads[artifactId].type = 'error';
        $scope.uploads[artifactId].error = {};
        $scope.uploads[artifactId].error.code = status;
        $scope.uploads[artifactId].error.message = 'An Error has occurred on the server!';
      });
    };

    $scope.onArtifactSelected = function($files, artifactId) {
      var file = $files[0];
      $scope.doUpload(file, artifactId);
    };

    /** REPLACE A NODE TEMPLATE */
    $scope.getIcon = UTILS.getIcon;

    $scope.getPossibleReplacements = function(selectedNodeTemplate) {
      topologyServices.nodeTemplate.getPossibleReplacements({
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: selectedNodeTemplate.name
      }, function(result) {
        $scope.suggestedReplacements = result.data;
      });
    };

    $scope.swapNodeTemplate = function(selectedNodeTemplate, newNodeType) {
      var newNodeTemplName = nodeTemplateNameFromType(newNodeType.elementId);
      var nodeTemplateRequest = {
        name: newNodeTemplName,
        indexedNodeTypeId: newNodeType.id
      };

      topologyServices.nodeTemplate.replace({
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: selectedNodeTemplate.name
      }, angular.toJson(nodeTemplateRequest), function(result) {
        refreshTopology(result.data, newNodeTemplName);
      });
    };

    /**
     * REMOVE RELATIONSHIP
     */
    $scope.removeRelationship = function(relationshipName, selectedNodeTemplate) {
      topologyServices.relationshipDAO.remove({
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: selectedNodeTemplate.name,
        relationshipName: relationshipName
      }, function(result) {
        if (result.error === null) {
          refreshTopology(result.data, selectedNodeTemplate.name);
        }
      });
    };

    /**
     * SCALING POLICIES
     */
    $scope.addScalingPolicy = function(nodeTemplateName) {
      var newScalingPolicy = {
        minInstances: 1,
        maxInstances: 1,
        initialInstances: 1
      };
      topologyServices.topologyScalingPoliciesDAO.save({
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: $scope.selectedNodeTemplate.name
      }, angular.toJson(newScalingPolicy), function() {
        if (UTILS.isUndefinedOrNull($scope.topology.topology.scalingPolicies)) {
          $scope.topology.topology.scalingPolicies = {};
        }
        $scope.topology.topology.scalingPolicies[nodeTemplateName] = newScalingPolicy;
      });
    };

    $scope.updateScalingPolicy = function(policyFieldName, policyFieldValue) {
      if (policyFieldValue < 0) {
        return $translate('ERRORS.800.greaterOrEqual', {
          reference: '0'
        });
      }
      var existingPolicy = $scope.topology.topology.scalingPolicies[$scope.selectedNodeTemplate.name];
      if (policyFieldName === 'initialInstances') {
        if (policyFieldValue < existingPolicy.minInstances) {
          return $translate('ERRORS.800.greaterOrEqual', {
            reference: 'minInstances'
          });
        }
        if (policyFieldValue > existingPolicy.maxInstances) {
          return $translate('ERRORS.800.lessOrEqual', {
            reference: 'maxInstances'
          });
        }
      } else if (policyFieldName === 'minInstances') {
        if (policyFieldValue > existingPolicy.initialInstances) {
          return $translate('ERRORS.800.lessOrEqual', {
            reference: 'initialInstances'
          });
        }
        if (policyFieldValue > existingPolicy.maxInstances) {
          return $translate('ERRORS.800.lessOrEqual', {
            reference: 'maxInstances'
          });
        }
      } else if (policyFieldName === 'maxInstances') {
        if (policyFieldValue < existingPolicy.initialInstances) {
          return $translate('ERRORS.800.greaterOrEqual', {
            reference: 'initialInstances'
          });
        }
        if (policyFieldValue < existingPolicy.minInstances) {
          return $translate('ERRORS.800.greaterOrEqual', {
            reference: 'minInstances'
          });
        }
      }
      existingPolicy[policyFieldName] = policyFieldValue;
      topologyServices.topologyScalingPoliciesDAO.save({
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: $scope.selectedNodeTemplate.name
      }, angular.toJson(existingPolicy), undefined);
    };

    $scope.deleteScalingPolicy = function(nodeTemplateName) {
      topologyServices.topologyScalingPoliciesDAO.remove({
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: $scope.selectedNodeTemplate.name
      }, function() {
        delete $scope.topology.topology.scalingPolicies[nodeTemplateName];
      });
    };

    /**
     * RELATIONSHIPS CARDINALITIES
     */
    // var fillCapabilityBounds = function(nodeTemplate) {
    //   var nodeType = $scope.topology.nodeTypes[nodeTemplate.type];
    //   if (nodeType.capabilities) {
    //     nodeType.capabilities.forEach(function(capaDef) {
    //       nodeTemplate.capabilitiesMap[capaDef.id].value.upperBound = capaDef.upperBound;
    //       nodeTemplate.capabilitiesMap[capaDef.id].value.canAddRel = canAddRelationshipWithTarget(nodeTemplate, capaDef.id);
    //     });
    //   }
    // };
    //
    // var canAddRelationshipWithTarget = function(nodeTemplate, capabilityName) {
    //   var nodeTemplates = $scope.topology.topology.nodeTemplates;
    //   var capability = nodeTemplate.capabilitiesMap[capabilityName].value;
    //   var UNBOUNDED = 'unbounded';
    //   var toReturn = {
    //     yes: true,
    //     remaining: UNBOUNDED
    //   };
    //   if (capability.upperBound === UNBOUNDED) {
    //     return toReturn;
    //   }
    //
    //   var count = 0;
    //   for (var templateName in nodeTemplates) {
    //     var nodeTemp = nodeTemplates[templateName];
    //     var relationships = nodeTemp.relationships;
    //     if (UTILS.isUndefinedOrNull(relationships)) {
    //       continue;
    //     }
    //     for (var relName in relationships) {
    //       var relTemplate = relationships[relName];
    //       if (UTILS.isDefinedAndNotNull(relTemplate.target) && relTemplate.target === nodeTemplate.name && relTemplate.requirementType === capability.type) {
    //         count++;
    //       }
    //     }
    //   }
    //   toReturn.yes = count < capability.upperBound;
    //   toReturn.remaining = toReturn.yes ? capability.upperBound - count : 0;
    //   return toReturn;
    // };

    var fillBounds = function(topology) {
      if (!topology.nodeTemplates) {
        return;
      }
      Object.keys(topology.nodeTemplates).forEach(function(nodeTemplateName) {
        var nodeTemplate = topology.nodeTemplates[nodeTemplateName];
        toscaCardinalitiesService.fillRequirementBounds($scope.topology.nodeTypes, nodeTemplate);
        toscaCardinalitiesService.fillCapabilityBounds($scope.topology.nodeTypes, $scope.topology.topology.nodeTemplates, nodeTemplate);
      });
    };
    $scope.getShortName = toscaService.simpleName;


    /**
     * Properties scopes
     */
    $scope.isInputProperty = function(propertyName) {
      if(UTILS.isUndefinedOrNull($scope.topology.topology.inputProperties)) {
        return false;
      }
      return $scope.topology.topology.inputProperties[$scope.selectedNodeTemplate.name].indexOf(propertyName) >= 0;
    };

    $scope.isInputArtifact = function(artifactName) {
      if(UTILS.isUndefinedOrNull($scope.topology.topology.inputArtifacts)) {
        return false;
      }
      return $scope.topology.topology.inputArtifacts[$scope.selectedNodeTemplate.name].indexOf(artifactName) >= 0;
    };

    $scope.isOutputProperty = function(propertyName) {
      if(UTILS.isUndefinedOrNull($scope.topology.topology.outputProperties)) {
        return false;
      }
      return $scope.topology.topology.outputProperties[$scope.selectedNodeTemplate.name].indexOf(propertyName) >= 0;
    };

    $scope.isOutputAttribute = function(attributeName) {
      if(UTILS.isUndefinedOrNull($scope.topology.topology.outputAttributes)) {
        return false;
      }
      return $scope.topology.topology.outputAttributes[$scope.selectedNodeTemplate.name].indexOf(attributeName) >= 0;
    };

    $scope.getFormatedProperty = function(propertyKey) {
      var formatedProperty = $scope.topology.nodeTypes[$scope.selectedNodeTemplate.type].propertiesMap[propertyKey].value;
      formatedProperty.name = propertyKey;
      return formatedProperty;
    };

    $scope.getPropertyDescription = function(propertyKey) {
      return $scope.topology.nodeTypes[$scope.selectedNodeTemplate.type].propertiesMap[propertyKey].description;
    };

    /**
     * Capabilities and Requirements docs
     */
    $scope.getComponent = function(nodeTemplate, type) {

      var nodeType = $scope.topology.nodeTypes[nodeTemplate.type];
      var componentId = type + ':' + nodeType.archiveVersion;

      return componentService.get({
        componentId: componentId
      }, function(successResult) {
        if (successResult.error === null) {
          if (successResult.data !== null) {
            return successResult.data.description;
          }
        }
      });
    };

    /**
     * Rename a relationship
     */
    $scope.relNameObj = {};
    /* Update relationship name */
    $scope.updateRelationshipName = function(oldName, newName) {
      // Update only when the name has changed
      if (oldName !== newName) {
        topologyServices.relationship.updateName({
          topologyId: $scope.topology.topology.id,
          nodeTemplateName: $scope.selectedNodeTemplate.name,
          relationshipName: oldName,
          newName: newName
        }, function(resultData) {
          if (resultData.error === null) {
            // update the name of the relationship
            var relationship = $scope.topology.topology.nodeTemplates[$scope.selectedNodeTemplate.name].relationshipsMap[oldName];
            relationship.key = newName;
            $scope.topology.topology.nodeTemplates[$scope.selectedNodeTemplate.name].relationshipsMap[newName] = relationship;
            delete $scope.topology.topology.nodeTemplates[$scope.selectedNodeTemplate.name].relationshipsMap[oldName];
            delete $scope.relNameObj[oldName];
          }
        }, function() {
          $scope.relNameObj[oldName] = oldName;
        });
      } // if end
    };

    /* Update properties of a node template */
    $scope.updateRelationshipProperty = function(propertyDefinition, propertyValue, relationshipType, relationshipName) {
      var propertyName = propertyDefinition.name;

      var updateRelationshipPropertyRequest = {
        'propertyName': propertyName,
        'propertyValue': propertyValue,
        'relationshipType': relationshipType
      };

      return topologyServices.relationship.updateProperty({
        topologyId: $scope.topology.topology.id,
        nodeTemplateName: $scope.selectedNodeTemplate.name,
        relationshipName: relationshipName
      }, angular.toJson(updateRelationshipPropertyRequest), function() {
        // update the selectedNodeTemplate properties locally
      }).$promise;
    };

    // check if compute type
    $scope.isComputeType = function(nodeTemplate) {
      if (UTILS.isUndefinedOrNull($scope.topology) || UTILS.isUndefinedOrNull(nodeTemplate)) {
        return false;
      }
      var nodeType = $scope.topology.nodeTypes[nodeTemplate.type];
      return UTILS.isFromNodeType(nodeType, COMPUTE_TYPE);
    };

    $scope.checkMapSize = function(map) {
      return angular.isDefined(map) && map !== null && Object.keys(map).length > 0;
    };
  }
]);
