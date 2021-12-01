define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-option').factory('optionService', [ function() {

    var options = null;

    var init = function() {
        var o = localStorage.getItem("options");
        if (o != null) {
            options = JSON.parse(o);
        } else {
            options = {};
            save();
        }
    };

    var save = function() {
        localStorage.setItem("options",JSON.stringify(options));
    };

    init();

    return {
        reset: function() {
            options = {};
            save();
        },

        get: function(key) {
            return options[key];
        },

        set: function(key, val) {
            options[key] = val;
            save();
        }
    };
  }]);
});
