package com.camunda.training;

import javax.inject.Inject;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import twitter4j.TwitterException;

@Component
public class CreateTweetDelegate implements JavaDelegate {
  
  private final Logger LOGGER = LoggerFactory.getLogger(CreateTweetDelegate.class.getName());
  
  private TwitterService twitterService;

  @Inject
  public CreateTweetDelegate(TwitterService twitterService) {
    super();
    this.twitterService = twitterService;
  }



  public void execute(DelegateExecution execution) throws Exception {
    String content = (String) execution.getVariable("content");
    LOGGER.info("Publishing tweet: " + content);
    if (content.equals("networkError")) {
      throw new ProcessEngineException("Network error");
    } else {
      long tweetId;
      try {
        tweetId = twitterService.publish(content);
      } catch (TwitterException e) {
        if (e.getErrorCode() == 187) {
          throw new BpmnError("duplicateTweetCode", e.getLocalizedMessage());
        } else {
          throw e;
        }
      }
      execution.setVariable("tweetId", tweetId);
    }
  }

}

