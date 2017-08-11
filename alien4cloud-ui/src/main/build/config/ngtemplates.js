'use strict';

// Create an angular template file from the views
module.exports = {
  alien4cloud: {
    cwd: '<%= yeoman.app %>',
    src: 'views/**/**.html',
    dest: '<%= yeoman.dist %>/views/alien4cloud-templates.js',
    options:    {
      htmlmin:  {
        collapseWhitespace: true,
        conservativeCollapse: true,
        collapseBooleanAttributes: false,
        removeCommentsFromCDATA: true,
        removeOptionalTags: false
      },
      bootstrap:  function(module, script) {
        return 'define(function () { return function($templateCache) {' + script + '};});';
      }
    }
  }
};
