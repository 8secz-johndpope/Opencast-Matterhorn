/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.capture.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.api.StateService;
import org.opencastproject.capture.impl.jobs.AgentStateJob;
import org.opencastproject.capture.impl.jobs.JobParameters;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * State service implementation.  This service keeps track of the states for the agent, as well as its recording(s).
 */
public class StateServiceImpl implements StateService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(StateServiceImpl.class);

  private Agent agent = null;
  private Hashtable<String, Recording> recordings = null;
  private ConfigurationManager configService = null;
  private Scheduler pollScheduler = null;

  public void setConfigService(ConfigurationManager svc) {
    configService = svc;
  }

  public void unsetConfigService() {
    configService = null;
  }

  public void activate(ComponentContext ctx) {
    recordings = new Hashtable<String, Recording>();
    agent = new Agent(configService.getItem(CaptureParameters.AGENT_NAME), AgentState.UNKNOWN);
    createPollingTask();
  }
  
  public void deactivate() {
    try {
      if (pollScheduler != null) {
          pollScheduler.shutdown(true);
      }
    } catch (SchedulerException e) {
      logger.warn("Finalize for pollScheduler did not execute cleanly: {}.", e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#setRecordingState(java.lang.String, java.lang.String)
   */
  public void setRecordingState(String recordingID, String state) {
    recordings.put(recordingID, new Recording(recordingID, state));
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getRecordingState(java.lang.String)
   */
  public Recording getRecordingState(String recordingID) {
    return recordings.get(recordingID);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getKnownRecordings()
   */
  public Map<String, Recording> getKnownRecordings() {
    return recordings;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getAgent()
   */
  public Agent getAgent() {
    return agent;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getAgentState()
   */
  public String getAgentState() {
    return agent.getState();
  }
  
  /**
   * {@inheritDoc}
   * Note that this specific implementation does nothing.  getAgentState queries directly from the CaptureAgent itself, rather than storing a string here.
   * @see org.opencastproject.capture.api.StateService#setAgentState(java.lang.String)
   */
  public void setAgentState(String state) {
    agent.setState(state);
  }

  /**
   * Creates the Quartz task which pushes the agent's state to the state server.
   */
  private void createPollingTask() {
    try {
      long pollTime = Long.parseLong(configService.getItem(CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL)) * 1000L;
      Properties pollingProperties = new Properties();
      InputStream s = getClass().getClassLoader().getResourceAsStream("config/state_update_scheduler.properties");
      if (s == null) {
        throw new RuntimeException("Resource config/state_update_scheduler.properties was not found!");
      }
      pollingProperties.load(s);
      StdSchedulerFactory sched_fact = new StdSchedulerFactory(pollingProperties);
  
      //Create and start the scheduler
      pollScheduler = sched_fact.getScheduler();
      if (pollScheduler.getJobGroupNames().length > 0) {
        logger.info("Duplicate attempt to create agent status task detected, ignoring...");
        return;
      }
      pollScheduler.start();
  
      //Setup the polling
      JobDetail job = new JobDetail("agentStateUpdate", Scheduler.DEFAULT_GROUP, AgentStateJob.class);

      job.getJobDataMap().put(JobParameters.STATE_SERVICE, this);
      job.getJobDataMap().put(JobParameters.CONFIG_SERVICE, configService);

      //TODO:  Support changing the polling interval
      //Create a new trigger                    Name              Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger trigger = new SimpleTrigger("state_polling", Scheduler.DEFAULT_GROUP, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, pollTime);

      //Schedule the update
      pollScheduler.scheduleJob(job, trigger);
    } catch (NumberFormatException e) {
      logger.error("Invalid time specified in the {} value, unable to push state to remote server!", CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL);
    } catch (IOException e) {
      logger.error("IOException caught in StateServiceImpl: {}.", e.getMessage());
    } catch (SchedulerException e) {
      logger.error("SchedulerException in StateServiceImpl: {}.", e.getMessage());
    }
  }

  public void updated(Dictionary properties) throws ConfigurationException {
    // TODO Auto-generated method stub
  }
}