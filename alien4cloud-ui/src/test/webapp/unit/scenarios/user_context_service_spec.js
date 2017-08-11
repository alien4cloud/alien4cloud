'use strict';

/* jasmine specs for controllers go here */
describe('User context service', function () {

    var service;

    beforeEach(function() {
        service = userContextServices();
    });

    it('when an applicaiton was not selected before should return null when ask for previous environment', function () {
        var env = service.getSelectedEnvironment('app'); 
        expect(env).toEqual(null);
    });

    it('when an applicaiton has been selected previously should return last update', function () {
        service.setSelectedEnvironment('appId', 'env1'); 
        service.setSelectedEnvironment('appId', 'env2'); 

        var env = service.getSelectedEnvironment('appId');
        expect(env).toEqual('env2');
    });

    it('after a clear all contexts should be cleaned', function () {
        service.setSelectedEnvironment('appId', 'env1'); 
        service.clear();
        var env = service.getSelectedEnvironment('appId');
        expect(env).toEqual(null);
    });
});
