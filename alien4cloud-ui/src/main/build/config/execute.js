// Add vendor prefixed styles
module.exports = {
  prerequire: {
		// simple inline function call
		call: function(grunt, options, async) {
      var done = async();

      var fs = require('fs');
      var path = require('path');
      var nativeModules = require(path.resolve('src/main/webapp/scripts/a4c-native'));
      var file = 'target/webapp/scripts/alien4cloud-bootstrap.js';

      fs.readFile(file, 'utf8', function (err, data) {
        if (err) {
          grunt.log.error(err);
          done(err);
        }
        var nativeStr = '[';
        for(var i=0; i<nativeModules.length; i++) {
          nativeStr += "'" + nativeModules[i] + "'" ;
          if(i !== nativeModules.length-1) {
            nativeStr+=',';
          }
        }
        nativeStr += ']';

        var result = data.replace('mods.nativeModules', nativeStr);

        fs.writeFile(file, result, 'utf8', function (err) {
           if (err) {
             grunt.log.error(err);
             done(err);
           } else {
             done();
           }
        });
      });
		}
	}
};
