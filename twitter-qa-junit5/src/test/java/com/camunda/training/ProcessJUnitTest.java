package com.camunda.training;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import twitter4j.TwitterException;

@RunWith(MockitoJUnitRunner.class)
public class ProcessJUnitTest {
  
  @Mock
  TwitterService mockedTwitterService;
  
  @ClassRule
  @Rule
//  public ProcessEngineRule rule = new ProcessEngineRule("test-coverage-camunda.cfg.xml");
  public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create(
      ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource("test-coverage-camunda.cfg.xml")
        .buildProcessEngine())
    .build();
  /*.assertClassCoverageAtLeast(0.9)*/

  @Before
  public void setup() {
    init(rule.getProcessEngine());
    Mocks.register("createTweetDelegate", new CreateTweetDelegate(mockedTwitterService));
  }
  
  @AfterClass
  public static void cleanUp() {
    rule.getProcessEngine().close();
  }
  
  @Test
  @Deployment(resources = {"TwitterQAProcess.bpmn", "tweetApproval.dmn"})
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

  @Test
  @Deployment(resources = {"tweetApproval.dmn", "TwitterQAProcess.bpmn"})
  public void testIBMTweetRejected() {
    Map<String, Object> variables = withVariables(
        "content", "process engines from IBM are small", 
        "email", "don't care");
    DmnDecisionTableResult decisionTableResult = decisionService().evaluateDecisionTableByKey("tweetApproval", variables);
    
    assertThat(decisionTableResult.getFirstResult()).contains(entry("approved", false));
  }
  
  @Test
  @Deployment(resources = {"tweetApproval.dmn", "TwitterQAProcess.bpmn"})
  public void testDefaultRule() {
    Map<String, Object> variables = withVariables(
        "content", "just a common tweet", 
        "email", "ingo.richtsmeier@camunda.com");
    DmnDecisionTableResult decisionTableResult = decisionService().evaluateDecisionTableByKey("tweetApproval", variables);
    
    assertThat(decisionTableResult.getFirstResult()).contains(entry("approved", true));
  }
}

