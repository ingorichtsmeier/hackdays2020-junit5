package com.camunda.training;

import static org.assertj.core.api.Assertions.*;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.process_test_coverage.junit.extension.ProcessEngineTestCoverageExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import twitter4j.TwitterException;

@ExtendWith(MockitoExtension.class)
public class NextGenerationCoverageJUnit5Test {
 
  @RegisterExtension
  static ProcessEngineTestCoverageExtension processEngineExtension = ProcessEngineTestCoverageExtension
      .builder()
      .configurationResource("test-coverage-camunda.cfg.xml")
      .build();
  
  @Mock
  TwitterService mockedTwitterService;  
  
  @BeforeEach
  public void initTest() {
    init(processEngineExtension.getProcessEngine());
    Mocks.register("createTweetDelegate", new CreateTweetDelegate(mockedTwitterService));
  }
  
  @AfterAll
  public static void cleanUp() {
    processEngineExtension.getProcessEngine().close();
  }
  
  @Test
  @Deployment(resources = "TwitterQAProcess.bpmn")
  public void testHappyPath() throws TwitterException {
    Mockito.when(mockedTwitterService.publish(Mockito.anyString())).thenReturn(100L);
    // Start process with Java API and variables
    ProcessInstance processInstance = runtimeService()
        .startProcessInstanceByKey("TwitterQAProcess", 
            withVariables(
                "content", "I did it from JUnit! Cheers Ingo 1",
                "email", "some.writer@camunda.com"));

    // Make assertions on the process instance
    assertThat(processInstance)
      .isWaitingAt(findId("Check tweet"))
      .task().hasCandidateGroup("management");
    
    complete(task(), withVariables("approved", true));
    
    assertThat(processInstance).isWaitingAt(findId("Publish tweet"));
    execute(job());
    
    assertThat(processInstance).isEnded().hasPassed(findId("Tweet published"))
      .variables().contains(entry("tweetId", 100L));
    Mockito.verify(mockedTwitterService).publish("I did it from JUnit! Cheers Ingo 1");    
  }
  
  @Test
  @Deployment(resources = "TwitterQAProcess.bpmn")
  public void testTweetRejected() {
    ProcessInstance processInstance = runtimeService()
        .createProcessInstanceByKey("TwitterQAProcess")
        .setVariables(withVariables("content", "should be rejected", "approved", false))
        .startAfterActivity(findId("Check tweet"))
        .execute();
    
    assertThat(processInstance).isWaitingAt(findId("Inform writer about rejection"))
      .externalTask().hasTopicName("notification");
  }
 
  @Test
  @Deployment(resources = "TwitterQAProcess.bpmn")
  public void testInformWriter() {
    ProcessInstance processInstance = runtimeService()
        .createProcessInstanceByKey("TwitterQAProcess")
        .setVariables(withVariables("content", "please reject", "approved", false))
        .startBeforeActivity(findId("Inform writer about rejection"))
        .execute();
    
    assertThat(processInstance).isActive();
    complete(externalTask());
    
    assertThat(processInstance).isEnded().hasPassed(findId("Tweet rejected"));
  }
  
  @Test
  @Deployment(resources = "TwitterQAProcess.bpmn")
  public void testSuperuserTweet() {
    ProcessInstance processInstance = runtimeService()
        .createMessageCorrelation("superuserTweetMessage")
        .setVariable("content", "I'm root")
        .correlateWithResult().getProcessInstance();
    
    assertThat(processInstance).isWaitingAt(findId("Publish tweet"));
  }
  
  @Test
  @Deployment(resources = "TwitterQAProcess.bpmn")
  public void testTweetWithdrawn() {
    ProcessInstance processInstance = runtimeService()
        .startProcessInstanceByKey("TwitterQAProcess", 
            withVariables("content", "should not be published"));
    
    assertThat(processInstance).isWaitingAt(findId("Check tweet"));
    
    runtimeService().startProcessInstanceByKey("TwitterQAProcess", 
        withVariables("content", "Don't do it again"));
    
    runtimeService()
      .createMessageCorrelation("tweetWithdrawalMessage")
      .processInstanceVariableEquals("content", "should not be published")
      .correlate();
    
    assertThat(processInstance).isEnded();
  }
  
  @Test
  @Deployment(resources = "TwitterQAProcess.bpmn")
  public void testDuplicateTweetError() throws TwitterException {
    Mockito.when(mockedTwitterService.publish("Duplicate tweet")).thenThrow(new BpmnError("duplicateTweetCode"));
    ProcessInstance processInstance = runtimeService()
        .createProcessInstanceByKey("TwitterQAProcess")
        .startBeforeActivity(findId("Publish tweet"))
        .setVariables(withVariables(
            "content",  "Duplicate tweet", 
            "approved", true))
        .execute();
    
    assertThat(processInstance).isWaitingAt(findId("Publish tweet"));
    execute(job());
    assertThat(processInstance).isWaitingAt(findId("Amend tweet"));
    complete(task());
    assertThat(processInstance).isWaitingAt(findId("Check tweet"));
  }
}
