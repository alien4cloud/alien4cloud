'use strict';

/* jasmine specs for controllers go here */
describe('Topology draw service test', function() {

  var nodeMap = {};
  var relationshipTypes = {};

  beforeEach(angular.mock.module('alienUiApp'));

  beforeEach(function() {
    jasmine.getJSONFixtures().fixturesPath = 'base/test/mock';
    nodeMap = UTILS.deepCopy(getJSONFixture('topologyNodes.json'));
    relationshipTypes = getJSONFixture('relationshipTypes.json');
  });

  var checkChildren = function(expectedChildrenName, children) {
    var foundRootChildrenNames = [];
    for (var i = 0; i < children.length; i++) {
      var childName = children[i].name;
      // Expected to be found
      expect(expectedChildrenName.indexOf(childName)).toBeGreaterThan(-1);
      // Expected to not be duplicated
      expect(foundRootChildrenNames.indexOf(childName)).toEqual(-1);
      foundRootChildrenNames.push(childName);
    }
  };

  it('build hosted on tree', inject(function(topologyLayoutService) {
    var tree = topologyLayoutService.buildHostedOnTree(nodeMap, relationshipTypes);
    expect(tree.children.length).toEqual(3);
    checkChildren(['ComputeOracle', 'ComputeGS', 'ComputeWebapp'], tree.children);
    var computeOracle = nodeMap['ComputeOracle'];
    checkChildren(['Oracle'], computeOracle.children);
  }));

  it('calculate node depth', inject(function(topologyLayoutService) {
    var tree = topologyLayoutService.buildHostedOnTree(nodeMap, relationshipTypes);
    topologyLayoutService.calculateNodeDepth(tree);
    expect(nodeMap['ComputeOracle'].nodeDepth).toEqual(1);
    expect(nodeMap['ComputeGS'].nodeDepth).toEqual(1);
    expect(nodeMap['ComputeWebapp'].nodeDepth).toEqual(1);
    expect(nodeMap['Oracle'].nodeDepth).toEqual(2);
    expect(nodeMap['JavaGS'].nodeDepth).toEqual(2);
    expect(nodeMap['Gigaspaces'].nodeDepth).toEqual(2);
    expect(nodeMap['JavaWebapp'].nodeDepth).toEqual(2);
    expect(nodeMap['Tomcat'].nodeDepth).toEqual(2);
    expect(nodeMap['MyWebapp'].nodeDepth).toEqual(3);
  }));

  it('calculate horizontal weight', inject(function(topologyLayoutService) {
    var tree = topologyLayoutService.buildHostedOnTree(nodeMap, relationshipTypes);
    topologyLayoutService.calculateNodeDepth(tree);
    topologyLayoutService.calculateHorizontalWeight(tree, nodeMap, relationshipTypes, 3);
    expect(nodeMap['ComputeOracle'].nodeHorizontalWeight).toEqual(-Number.MAX_VALUE);
    expect(nodeMap['ComputeOracle'].branchHorizontalWeight).toEqual(1);
    expect(nodeMap['ComputeGS'].nodeHorizontalWeight).toEqual(-Number.MAX_VALUE);
    expect(nodeMap['ComputeGS'].branchHorizontalWeight).toEqual(0);
    expect(nodeMap['ComputeWebapp'].nodeHorizontalWeight).toEqual(-Number.MAX_VALUE);
    expect(nodeMap['ComputeWebapp'].branchHorizontalWeight).toEqual(-1);
    expect(nodeMap['Oracle'].nodeHorizontalWeight).toEqual(-3);
    expect(nodeMap['JavaGS'].nodeHorizontalWeight).toEqual(-1);
    expect(nodeMap['Gigaspaces'].nodeHorizontalWeight).toEqual(1);
    expect(nodeMap['JavaWebapp'].nodeHorizontalWeight).toEqual(-1);
    expect(nodeMap['Tomcat'].nodeHorizontalWeight).toEqual(1);
    expect(nodeMap['MyWebapp'].nodeHorizontalWeight).toEqual(3);
  }));

  it('build graph', inject(function(topologyLayoutService) {
    var tree = topologyLayoutService.buildHostedOnTree(nodeMap, relationshipTypes);
    topologyLayoutService.calculateNodeDepth(tree);
    topologyLayoutService.calculateHorizontalWeight(tree, nodeMap, relationshipTypes, 3);
    var graph = topologyLayoutService.calculateLayout(tree, nodeMap, relationshipTypes, 200, 50, 160, 40, 30);
    console.log(graph);
  }));

});
