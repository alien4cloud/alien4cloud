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
    [ function() {
        // This service groups tasks by code to ease display
        return {
          processAll: function (validationDTO) {
            processTaskType(validationDTO, 'infoList');
            processTaskType(validationDTO, 'warningList');
            processTaskType(validationDTO, 'taskList');
          }
        };
      } // function
    ]); // factory
}); // define
