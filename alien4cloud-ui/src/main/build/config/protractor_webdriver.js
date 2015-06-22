'use strict';

// Test settings
module.exports = {
  start: {
    options: {
      // default webdriver packaged with protractor
      // `webdriver-manager update` done by the calm-yeoman-maven-plugin
      keepAlive : true,
      path: 'node_modules/.bin/',
      command: 'webdriver-manager start'
    }
  }
};
