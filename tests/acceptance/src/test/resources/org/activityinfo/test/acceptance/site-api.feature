@api
Feature: Site API

  Background:
    Given I have created a database "RRMP"
    And I have added partner "NRC" to "RRMP"
    And I have created a published form named "Distributions"
    And I have created a quantity field "kits" in "Distributions" with code "kits"
    And I have created a quantity field "score" in "Distributions" with code "score"
    And I have created a enumerated field "donor" with items:
      | USAID  |
      | ECHO   |
      | NRC    |
    When I have submitted a "Distributions" form with:
      | field     | value      |
      | kits      | 1          |
      | score     | 2          |
      | partner   | NRC        |
      | donor     | USAID      |
      | fromDate  | 2014-01-01 |
      | toDate    | 2014-04-10 |

  @AI-574
  Scenario: Querying site's points on public database with unauthenticated user
    When Unauthenticated user requests /resources/sites/points?activity=$Distributions
    Then the response should be:
    """
    type : FeatureCollection
    features :
     -
        type : Feature
        id : $SiteId
        properties :
          locationName : RDC
          partnerName : $NRC
          activity : $Distributions
          activityName : Distributions
          startDate : 2014-01-01
          endDate : 2014-04-10
          indicators :
            $kits : 1.0
            $score : 2.0
        geometry :
          type : Point
          coordinates :
           -  21.746970920000003
           -  -4.034950903
    """

  @AI-574
  Scenario: Querying site's points on public database with owner user
    When I request /resources/sites/points?activity=$Distributions
    Then the response should be:
    """
    type : FeatureCollection
    features :
     -
        type : Feature
        id : $SiteId
        properties :
          locationName : RDC
          partnerName : $NRC
          activity : $Distributions
          activityName : Distributions
          startDate : 2014-01-01
          endDate : 2014-04-10
          indicators :
            $kits : 1.0
            $score : 2.0
        geometry :
          type : Point
          coordinates :
           -  21.746970920000003
           -  -4.034950903
    """

  @AI-574
  Scenario: Querying site's points on public database with another user (not owner)
    When bob@bedatadriven.com requests /resources/sites/points?activity=$Distributions
    Then the response should be:
    """
    type : FeatureCollection
    features :
     -
        type : Feature
        id : $SiteId
        properties :
          locationName : RDC
          partnerName : $NRC
          activity : $Distributions
          activityName : Distributions
          startDate : 2014-01-01
          endDate : 2014-04-10
          indicators :
            $kits : 1.0
            $score : 2.0
        geometry :
          type : Point
          coordinates :
           -  21.746970920000003
           -  -4.034950903
    """