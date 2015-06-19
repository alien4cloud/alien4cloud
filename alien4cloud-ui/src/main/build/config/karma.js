'use strict';

// Test settings
module.exports = {
  unit: {
    configFile: 'src/test/webapp/karma.conf.js',
    singleRun: true
  },
  e2e: {
    configFile: 'src/test/webapp/karma-e2e.conf.js',
    singleRun: true
  },
  jenkins: {
    configFile: 'src/test/webapp/karma.conf.js',
    singleRun: true,
    runnerPort: 9999,
    browsers: ['PhantomJS']
  }
};