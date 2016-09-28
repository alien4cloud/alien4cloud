module.exports = function (grunt) {
  grunt.registerTask('build',
    [
      'clean:dist', // cleanup dist folder
      'jshint:all',
      'concurrent:dist', // process compass and images
      'autoprefixer', // update CSS to add browser specifics (moz- etc.)
      'copy:dist', // copy resource files to dist folders
      'cssmin', // minify css
      'execute:prerequire', // inject the native modules in the a4c bootstrap requirements so they are loaded before plugins
      'requirejs:dist',
      'useminPrepare',
      'rev', // rename files for caching purpose
      'usemin',
      'execute:revrename',
      'htmlmin'
    ]
  );
  grunt.registerTask('nooptimize',
    [
      'clean:dist', // cleanup dist folder
      'jshint:all',
      'concurrent:dist', // process compass and images
      'autoprefixer', // update CSS to add browser specifics (moz- etc.)
      'copy:dist', // copy resource files to dist folders
      'cssmin', // minify css
      'execute:prerequire',
      'requirejs:noOptimize',
      'htmlmin'
    ]
  );
};
