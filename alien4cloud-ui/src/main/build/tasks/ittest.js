module.exports = function (grunt) {
  var tasks = ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'protractor_webdriver:start'];

  var registerIt = function(name, launchTask) {
    var itTasks = tasks.slice(0);
    itTasks.push(launchTask);
    grunt.registerTask(name, itTasks);
  };

  var chromeTasks = tasks.slice(0);
  chromeTasks.push('protractor:runChrome');
  grunt.registerTask('chrome-ittest', '', function() {
    var tasks = chromeTasks;
    grunt.option('force', true);
    grunt.task.run(tasks);
  });

  registerIt('firefox-ittest', 'protractor:runFirefox');
  registerIt('iexplore-ittest', 'protractor:runIexplore');
  registerIt('local-ittest', 'protractor:runLocalserver');
  registerIt('ittest-admin', 'protractor:runAdmin');
  registerIt('ittest-application', 'protractor:runApplication');
  registerIt('ittest-applicationTopology', 'protractor:runApplicationTopology');
  registerIt('ittest-deploymentAndSecurity', 'protractor:runDeploymentAndSecurity');
  registerIt('ittest-otherTests', 'protractor:runOtherTests');
};