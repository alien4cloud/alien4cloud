define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

//Just group taskType by category (code)
  function processTaskType(validationDTO, taskType) {
    //  prepare
    if (_.defined(validationDTO[taskType])) {
      var processedList = {};
      var sourceMap = {}; // errors by source
      validationDTO[taskType].forEach(function(task) {
        if (!processedList.hasOwnProperty(task.code)) {
          processedList[task.code] = [];
        }
        processedList[task.code].push(task);
        var source = _.get(task, 'source', 'none');
        if(_.undefined(_.get(sourceMap, [source, taskType, task.code]))) {
          _.set(sourceMap, [source, taskType, task.code], []);
        }
        sourceMap[source][taskType][task.code].push(task);
      });
      // replace the default list
      validationDTO[taskType] = processedList;
      validationDTO.bySources = sourceMap;
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
