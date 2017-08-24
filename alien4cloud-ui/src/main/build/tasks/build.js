'use strict';

module.exports = function (grunt) {
  grunt.registerTask('build',
    [
      'clean:dist', // cleanup dist folder
      'jshint:all',
      'concurrent:dist', // process compass and images
      'autoprefixer', // update CSS to add browser specifics (moz- etc.)
      'copy:dist', // copy resource files to dist folders
      'execute:prerequire', // inject the native modules in the a4c bootstrap requirements so they are loaded before plugins
      'requirejs:a4cdist', // Package all alien4cloud files into a single alien4cloud-bootstrap.js file.
      'execute:minify', // Minify alien4cloud-bootstrap.js file.
      'requirejs:a4cdepdist', // Package all dependencies (bower components) into a single alien4cloud-dependencies.js file
      'clean:bower', // remove the bower_components directory from the build (to avoid shipping useless files)
      'copy:bower', // Copy back the bower_components that are required for the application to run (mainly requirejs file)
      'useminPrepare', // Prepare for renaming of css files and so on.
      'concat', // concat css files
      'cssmin', // minify css
      'ngtemplates', // Copy all views html files into a javascript file that will populate the angular template cache.
      'rev:appFiles', // rename files for caching purpose except require.config.js
      'execute:revrename', // Apply renaming of javascript files inside the require.config.js file (alien4cloud-bootstrap, alien4cloud-dependencies and alien4cloud-templates) and translation files
      'rev:requireConfig', // rename require.config.js file for caching purpose
      'usemin' // Apply renaming of js and css files to index.html (including require.config.js renaming)
    ]
  );
};
