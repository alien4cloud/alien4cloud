define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

//Just group taskType by category (code)
  function processTaskType(validationDTO, taskType) {
    //  prepare
    if (_.defined(validationDTO[taskType])) {
      var processedList = {};
      validationDTO[taskType].forEach(function(task) {
        if (!processedList.hasOwnProperty(task.code)) {
          processedList[task.code] = [];
        }
        processedList[task.code].push(task);
      });
      // replace the default list
      validationDTO[taskType] = processedList;
    }
  }
  
  modules.get('a4c-applications').factory('tasksProcessor',
    [ function(listToMapService, topologyJsonProcessor) {
        // This service post-process tasks (required and warning)
        return {
          // Just group warnings by category (code) for the display
          processWarnings: function (validationDTO) { 
            processTaskType(validationDTO, 'warningList')
          },
          // Just group tasks by category (code) for the display
          processRequired: function (validationDTO) {
            processTaskType(validationDTO, 'taskList')
          },
          processAll: function (validationDTO) {
            this.processWarnings(validationDTO);
            this.processRequired(validationDTO);
          }
        };
      } // function
    ]); // factory
}); // define
