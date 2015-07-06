Feature: Csargit crud feature

  Background:
    Given I am authenticated with "ADMIN" role
	
	Scenario: Create a new csargit
	 Given I have a csargit with the url "https://github.com/alien4cloud/samples" with username "ff" and password "ff"
	 And I add locations to the csar
	     | branchId | subPath |
	     | master   | oooo    |
	 When I create a csargit
	 Then I should received a response with the auto-generated id
	  And csargit should have the same id as the response id
	  
	Scenario: Create a new csargit which already exists 
	  Given I have a csargit with the url "https://github.com/alien4cloud/samples", with username "" and password ""
	    And I add locations to the csar
	     | branchId | subPath |
	     | master   | oooo    |
	  When I create a csargit 
	  Then I should received a RestResponse with an error code 501
	  
	Scenario: Create a new csargit with empty data
	  Given I have a csargit with the url "", with username "" and password "" and importLocation
	   	  | branchId | subPath
	  	  |  		 | 
	  When I create a csargit 
	  Then I should received a RestResponse with an error code 501 
	        
	Scenario: Import a csargit
	  Given I have a csargit with the url "https://github.com/alien4cloud/samples", with username "" and password "" and importLocation
	   	  | branchId | subPath
	  	  | master 	 | 
	  When I upload all the archives of the csargit with url "https://github.com/alien4cloud/samples"
	  Then I should receive a RestResponse with no error
	   And I have the Csar content imported
	         
	Scenario: Delete a defined csargit
	  Given I have a csargit with the url "https://github.com/alien4cloud/samples", with username "" and password "" and importLocation
	   	  | branchId | subPath
	  	  | master 	 | 
	  When I delete the csargit with the url "https://github.com/alien4cloud/samples"
	  Then I should receive a RestResponse with no error
	   And I have the csargit with the url "https://github.com/alien4cloud/samples" deleted
	   