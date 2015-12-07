// Utilities to work with alien file-system data

'use strict';

var fs = require('fs-extra');
var path = require('path');
var ALIEN_BASE = (process.env.HOME || process.env.HOMEPATH || process.env.USERPROFILE) + path.sep + '.alien' + path.sep;

// Remove the content of a folder and the folder too only if removeDir is true.
function deleteFolderRecursive(folderPath, removeDir) {
  if (fs.existsSync(folderPath)) {
    fs.readdirSync(folderPath).forEach(function(file) {
      var curPath = folderPath + path.sep + file;
      if (fs.statSync(curPath).isDirectory()) { // recurse
        deleteFolderRecursive(curPath, true);
      } else { // delete file
        fs.unlinkSync(curPath);
      }
    });
    if (removeDir) {
      fs.rmdirSync(folderPath);
    }
  }
}

module.exports.rmArchives = function() {
  deleteFolderRecursive(ALIEN_BASE + 'csar');
};

module.exports.rmPlugins = function() {
  deleteFolderRecursive(ALIEN_BASE + 'plugins');
  deleteFolderRecursive(ALIEN_BASE + 'work/plugins');
};

module.exports.rmArtifacts = function() {
  deleteFolderRecursive(ALIEN_BASE + 'artifacts');
};

module.exports.rmImages = function() {
  deleteFolderRecursive(ALIEN_BASE + 'images');
};

module.exports.copyPlugin = function(pluginId, from) {
  fs.copySync(from, ALIEN_BASE + 'work/plugins/content/' + pluginId);
};

module.exports.copyArchive = function(archiveId, archiveVersion, from, contentJsonPath) {
  if(contentJsonPath && contentJsonPath !== null) {
    fs.copySync(contentJsonPath, ALIEN_BASE + 'csar/' + archiveId + '/' + archiveVersion);
  }
  fs.copySync(from, ALIEN_BASE + 'csar/' + archiveId + '/' + archiveVersion + '/expanded');
};

module.exports.copyImages = function(from) {
  fs.copySync(from, ALIEN_BASE + 'images');
};
