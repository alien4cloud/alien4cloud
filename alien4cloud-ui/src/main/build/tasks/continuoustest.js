module.exports = function (grunt) {
  grunt.registerTask('continuoustest', ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'karma:jenkins']);
};