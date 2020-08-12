package com.camunda.training;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerDelegate implements JavaDelegate {
  
  private final static Logger LOG = LoggerFactory.getLogger(LoggerDelegate.class);

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    LOG.info("passing {} from process instance {}", 
        execution.getCurrentActivityName(), 
        execution.getProcessInstanceId());
    

  }

}
