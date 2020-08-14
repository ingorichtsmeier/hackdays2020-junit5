# hackdays2020-JUnit5
Write process tests with JUnit 5

## Findings

* Extensions are simple to use
* Yana did most of the work beforehand
* process-test-coverage developers don't use Linux
* Extensions support the lifecycle of the Class better than Rules:
  * Rules have to deal if the annotation is coming from the Class or the Method
  * The Lifecycle of the ProcessEngineTestCoverageRule is quite complex. It's getting easier with Extensions.
* Running tests with maven:
  * Disable an activated profile: `-P !camunda-bpm-engine-7.13.0`
  * Configure the vintage and the jupiter engine, otherwise only one kind of tests runs
  * use `maven-surefire-plugin` version 2.22.2, otherwise only one kind of tests runs
  * Close the engine after the test, if you have more than one test class in your project. It's independent from Rule or Extension. Otherwise the next process engine complains that the database tables already exist.
* Change the color of the maven output in the console with environment variable `MAVEN_OPTS` using the value `-Dstyle.warning=MAGENTA`

## Available Resources

Existing JIRA ticket: https://jira.camunda.com/browse/CAM-11955

Branch for camunda-bpm-platform, including the ProcessEngineExtension: https://github.com/ingorichtsmeier/camunda-bpm-platform/tree/test-junit5

Branch for camunda-bpm-process-test-coverage: https://github.com/ingorichtsmeier/camunda-bpm-process-test-coverage/tree/junit5

Example tests: https://github.com/ingorichtsmeier/hackdays2020-junit5/tree/master/twitter-qa-junit5/src/test/java/com/camunda/training



