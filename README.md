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

  * Configure the vintage and the jupiter engine, otherwise only one kind of tests runs
  * use `maven-surefire-plugin` version 2.22.2, otherwise only one kind of tests runs
  * Close the engine after the test, if you have more than one test class in your project. It's independent from Rule or Extension. Otherwise the next process engine complains that the database tables already exist.

* Change the color of the maven output in the console with environment variable `MAVEN_OPTS` using the value `-Dstyle.warning=MAGENTA`

  

