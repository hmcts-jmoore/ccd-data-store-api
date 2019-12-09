@F-066
Feature: F-066: Retrieve a start event trigger by ID for dynamic display

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-182
  Scenario: should retrieve trigger when the case and event exists
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Standard_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 200 return code]
    And the response has all other details as expected

  @S-183 @Ignore # case-type is not required in the path variables, So this positive test scenario is invalid.
    Scenario: should retrieve trigger when the case type and event exists
      Given a user with [an active profile in CCD]
      And a case that has just been created as in [Standard_Full_Case]
      When a request is prepared with appropriate values
      And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store]
      Then a positive response is received
      And the response [has the 200 return code]
      And the response has all other details as expected

  @S-175
  Scenario: must return negative response when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-176
  Scenario: must return negative response when request provides authentic credentials without authorised access
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide an authorised access to the operation]
    And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-177
  Scenario: must return negative response when request contains an invalid case-reference
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid case-reference]
    And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 400 return code]
    And the response has all the details as expected

  @S-178 @Ignore # This scenario is returning 400 instead of expected 404, Need to raise defect JIRA
  Scenario: must return negative response when request contains a non-existing case-reference
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing case-reference]
    And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 404 return code]
    And the response has all the details as expected

  @S-179 @Ignore # case-type is not required in the path variables, So this test scenario is invalid.
    Scenario: must return negative response when request contains an invalid case-type
      Given a user with [an active profile in CCD]
      When a request is prepared with appropriate values
      And the request [contains an invalid case-type]
      And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store]
      Then a negative response is received
      And the response [has the 400 return code]
      And the response has all the details as expected

  @S-180
    Scenario: must return negative response when request contains an invalid event-trigger
      Given a user with [an active profile in CCD]
      And a case that has just been created as in [Standard_Full_Case_Creation_Data]
      When a request is prepared with appropriate values
      And the request [contains an invalid event-trigger]
      And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store]
      Then a negative response is received
      And the response [has the 404 return code]
      And the response has all the details as expected

  @S-181 @Ignore # Duplicate
      Scenario: should get 404 when event trigger does not exist
        Given a user with [an active profile in CCD]
        And a case that has just been created as in [Standard_Full_Case_Creation_Data]
        When a request is prepared with appropriate values
        And the request [contains an invalid event-trigger]
        And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store]
        Then a negative response is received
        And the response [has the 404 return code]
        And the response has all the details as expected


