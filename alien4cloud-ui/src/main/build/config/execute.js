// Add vendor prefixed styles
module.exports = {
  prerequire: {
		// simple inline function call
		call: function(grunt, options, async) {
      'use strict';
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
	},
  revrename: {
    // Edit the requirejs config file to load the alien4cloud-bootstrap.js file after being renamed by rev
    call: function(grunt, options, async) {
      'use strict';
      var done = async();

      var fs = require('fs');
      var path = require('path');

      fs.readdir('target/webapp/views', function(err, files) {
        if(err) {
          grunt.log.error(err);
          done(err);
        }
        var templateFile;
        files.forEach( function(file) {
          if(file.indexOf('alien4cloud-templates.js') !== -1) {
            templateFile = file;
          }
        });

        var dir = 'target/webapp/scripts';
        fs.readdir(dir, function(err, files) {
          if(err) {
            grunt.log.error(err);
            done(err);
          }
          var requireFile, bootstrapFile, dependenciesFile;
          files.forEach( function(file) {
            if(file.indexOf('alien4cloud-bootstrap.js') !== -1) {
              bootstrapFile = file;
            }
            if(file.indexOf('require.config.js') !== -1) {
              requireFile = file;
            }
            if(file.indexOf('alien4cloud-dependencies.js') !== -1) {
              dependenciesFile = file;
            }
          });

          var requireFilePath = path.join(dir, requireFile);
          fs.readFile(requireFilePath, 'utf8', function (err, data) {
            var result = data.replace('alien4cloud-bootstrap', bootstrapFile.substring(0, bootstrapFile.length-3));
            result = result.replace('alien4cloud-templates', templateFile.substring(0, templateFile.length-3));
            result = result.replace('alien4cloud-dependencies', dependenciesFile.substring(0, dependenciesFile.length-3));


            fs.writeFile(requireFilePath, result, 'utf8', function (err) {
               if (err) {
                 grunt.log.error(err);
                 done(err);
               } else {
                 done();
               }
            });
          });
        });
      });
    }
  }
};
