/* global by */

'use strict';

var common = require('../../common/common');
var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');
var applications = require('../../applications/applications');
var applicationsData = require(__dirname + '/_data/application_topology_editor_groups/applications.json');

describe('Topology node groups', function() {

  var expectMemberName = function(member, memberName1, memberName2) {
    member.element(by.tagName('span')).getText().then(function(memberName) {
      expect(memberName === memberName1 || memberName === memberName2).toBe(true);
    });
  };

  it('beforeAll', function() {
    setup.setup();
    setup.index('application', 'application', applicationsData);
    common.home();
    authentication.login(authentication.users.sauron.username);
  });

  it('should be able to create a node group from a node template', function() {
    console.log('################# should be able to create a node group from a node template');
    applications.goToApplicationTopologyPage();
    console.log('## add a group from the node Compute and ensure the group appear in the list');
    // add a new group
    topologyEditorCommon.addNodeTemplateToNodeGroup('Compute');
    // ensure the groups details is expanded
    element(by.id('node-details-groups-panel')).isDisplayed().then(function(isVisible) {
      if (!isVisible) {
        element(by.id('node-details-groups')).click();
      }
      // we should have 1 group for this node
      expect(element.all(by.repeater('groupId in selectedNodeTemplate.groups')).count()).toEqual(1);
      element.all(by.repeater('groupId in selectedNodeTemplate.groups')).then(function(groups) {
        expect(groups[0].element(by.tagName('span')).getText()).toEqual('Compute');
      });
      // close the node details box
      element(by.id('closeNodeTemplateDetails')).click();
    });

    console.log('## ensure the group appear in the group box with the correct members');
    // show the groups box
    element(by.id('topology-groups')).element(by.xpath('..')).click();
    // we should have 1 group at all
    expect(element(by.id('groups-box')).all(by.repeater('group in orderedNodeGroups')).count()).toEqual(1);
    // expand the members for the group 'Compute'
    element(by.id('group-members-Compute-content')).isDisplayed().then(function(isVisible) {
      if (!isVisible) {
        expect(element(by.id('group-members-Compute-header')).isDisplayed()).toBeTruthy();
        element(by.id('group-members-Compute-header')).click();
        browser.sleep(2000);
      }
      // expect to have 1 member in this group
      expect(element(by.id('group-members-Compute-content')).all(by.repeater('member in group.members')).count()).toEqual(1);
      element(by.id('group-members-Compute-content')).all(by.repeater('member in group.members')).then(function(members) {
        expect(members[0].element(by.tagName('span')).getText()).toEqual('Compute');
      });
      // close the groups panel
      element(by.id('closeGroups')).click();
    });

    console.log('## add a new node and create a new group from it');
    // add another node
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({
      compute: componentData.toscaBaseTypes.compute()
    });
    // and another group
    topologyEditorCommon.addNodeTemplateToNodeGroup('Compute-2');
    // close the node details box
    element(by.id('closeNodeTemplateDetails')).click();

    console.log('## rename this new group');
    // show the groups box
    element(by.id('topology-groups')).element(by.xpath('..')).click();
    // rename the group
    var xeditable = element(by.id('nodeGroupName_Compute-2'));
    xeditable.click();
    var form = xeditable.element(by.xpath('..')).element(by.tagName('form'));
    var input = form.element(by.tagName('input'));
    input.clear();
    input.sendKeys('MyGroup');
    form.submit();
    // close the groups panel
    element(by.id('closeGroups')).click();

    console.log('## associate the new group to the node Compute');
    // associate the new group to the node 'Compute'
    topologyEditorCommon.addNodeTemplateToNodeGroup('Compute', 'MyGroup');
    // ensure the groups details is expanded
    element(by.id('node-details-groups-panel')).isDisplayed().then(function(isVisible) {
      if (!isVisible) {
        expect(element(by.id('node-details-groups')).isDisplayed()).toBeTruthy();
        element(by.id('node-details-groups')).click();
        browser.sleep(2000);
      }
      // expect to have 2 groups now
      expect(element.all(by.repeater('groupId in selectedNodeTemplate.groups')).count()).toEqual(2);
      element.all(by.repeater('groupId in selectedNodeTemplate.groups')).then(function(members) {
        // It's a test one of as order is not respected
        expectMemberName(members[0], 'MyGroup', 'Compute');
        expectMemberName(members[1], 'MyGroup', 'Compute');
      });
      // close the node details box
      expect(element(by.id('closeNodeTemplateDetails')).isDisplayed()).toBeTruthy();
      element(by.id('closeNodeTemplateDetails')).click();
    });

    console.log('## ensure the 2 groups appear in the group box');
    // show the groups box
    expect(element(by.id('topology-groups')).isDisplayed()).toBeTruthy();
    element(by.id('topology-groups')).element(by.xpath('..')).click();
    // we should have 2 group at all
    expect(element(by.id('groups-box')).all(by.repeater('group in orderedNodeGroups')).count()).toEqual(2);

    console.log('## remove a member from a group');
    // ensure the members for the group 'Compute' are displayed
    element(by.id('group-members-MyGroup-content')).isDisplayed().then(function(isVisible) {
      if (!isVisible) {
        expect(element(by.id('group-members-MyGroup-header')).isDisplayed()).toBeTruthy();
        element(by.id('group-members-MyGroup-header')).click();
        browser.sleep(2000);
      }
      // expect to have 2 members in this group
      expect(element(by.id('group-members-MyGroup-content')).all(by.repeater('member in group.members')).count()).toEqual(2);
      element(by.id('group-members-MyGroup-content')).all(by.repeater('member in group.members')).then(function(members) {
        // It's a test one of as order is not respected
        expectMemberName(members[0], 'Compute-2', 'Compute');
        expectMemberName(members[1], 'Compute-2', 'Compute');
        // delete the member 'Compute'
        expect(members[1].element(by.tagName('a')).isDisplayed()).toBeTruthy();
        members[1].element(by.tagName('a')).click();
        common.confirmAction(true);
      });
      // close the groups panel
      expect(element(by.id('closeGroups')).isDisplayed()).toBeTruthy();
      element(by.id('closeGroups')).click();
    });

    console.log('## check that the removed member has no more the group referenced');
    // click on the node 'Compute'
    expect(element(by.id('rect_Compute')).isDisplayed()).toBeTruthy();
    element(by.id('rect_Compute')).click();
    // ensure the groups details is expanded
    element(by.id('node-details-groups-panel')).isDisplayed().then(function(isVisible) {
      if (!isVisible) {
        element(by.id('node-details-groups')).click();
        browser.sleep(2000);
      }
      // expect to have 1 groups now
      expect(element.all(by.repeater('groupId in selectedNodeTemplate.groups')).count()).toEqual(1);
      element.all(by.repeater('groupId in selectedNodeTemplate.groups')).then(function(members) {
        expect(members[0].element(by.tagName('span')).getText()).toEqual('Compute');
      });
      // close the node panel
      element(by.id('closeNodeTemplateDetails')).click();
    });

    console.log('## delete a group');
    // show the groups box
    element(by.id('topology-groups')).element(by.xpath('..')).click();
    // remove the group
    common.deleteWithConfirm('btn-delete-group-MyGroup', true);
    // now we should have 1 group at all
    expect(element(by.id('groups-box')).all(by.repeater('group in orderedNodeGroups')).count()).toEqual(1);
    // close the groups panel
    element(by.id('closeGroups')).click();

    console.log('## ensure the node no more reference the deleted group');
    // click on the node 'Compute-2'
    element(by.id('rect_Compute-2')).click();
    // expect to have 0 groups now
    expect(element.all(by.repeater('groupId in selectedNodeTemplate.groups')).count()).toEqual(0);
    // the 'group' block should disappear
    expect(element(by.id('node-details-groups')).isPresent()).toBe(false);
    // close the node details box
    element(by.id('closeNodeTemplateDetails')).click();

    console.log('## remove a member from the node');
    // click on the node 'Compute'
    element(by.id('rect_Compute')).click();
    // ensure the groups details is expanded
    element(by.id('node-details-groups-panel')).isDisplayed().then(function(isVisible) {
      if (!isVisible) {
        element(by.id('node-details-groups')).click();
        browser.sleep(2000);
      }
      element.all(by.repeater('groupId in selectedNodeTemplate.groups')).then(function(members) {
        expect(members[0].element(by.tagName('span')).getText()).toEqual('Compute');
        // remove the group from the node
        members[0].element(by.tagName('a')).click();
        common.confirmAction(true);
      });
      element(by.id('closeNodeTemplateDetails')).click();
    });

    console.log('## ensure the group has no more members');
    // show the groups box
    expect(element(by.id('topology-groups')).isDisplayed()).toBeTruthy();
    element(by.id('topology-groups')).element(by.xpath('..')).click();
    // we should have 1 group at all
    expect(element(by.id('groups-box')).all(by.repeater('group in orderedNodeGroups')).count()).toEqual(1);
    // ensure no more members for the group 'Compute'
    element(by.id('group-members-Compute-content')).isDisplayed().then(function(isVisible) {
      if (!isVisible) {
        expect(element(by.id('group-members-Compute-header')).isDisplayed()).toBeTruthy();
        element(by.id('group-members-Compute-header')).click();
        browser.sleep(2000);
      }
      // expect to have 0 members in this group
      expect(element(by.id('group-members-Compute-content')).all(by.repeater('member in group.members')).count()).toEqual(0);
      // close the groups panel
      expect(element(by.id('closeGroups')).isDisplayed()).toBeTruthy();
      element(by.id('closeGroups')).click();
    });


    // finally ensure that no group button is available for a non compute node
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({
      war: componentData.alienTypes.war()
    });
    // click on the node 'War'
    element(by.id('rect_War')).click();
    expect(element(by.id('node_groups_War')).isPresent()).toBe(false);
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
