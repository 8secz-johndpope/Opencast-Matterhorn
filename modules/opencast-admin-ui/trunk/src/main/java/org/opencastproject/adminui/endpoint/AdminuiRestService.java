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
package org.opencastproject.adminui.endpoint;

import org.opencastproject.adminui.api.RecordingDataView;
import org.opencastproject.adminui.api.RecordingDataViewImpl;
import org.opencastproject.adminui.api.RecordingDataViewList;
import org.opencastproject.adminui.api.RecordingDataViewListImpl;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.CaptureAgentStatusService;
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.EName;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.util.Map.Entry;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;

/**
 * REST endpoint for the Admin UI proxy service
 */
@Path("/")
public class AdminuiRestService {
  private static final Logger logger = LoggerFactory.getLogger(AdminuiRestService.class);

  private SchedulerService schedulerService;
  private WorkflowService workflowService;
  private CaptureAgentStatusService captureAdminService;

  public void setSchedulerService(SchedulerService service) {
    logger.info("binding SchedulerService");
    schedulerService = service;
  }

  public void unsetSchedulerService(SchedulerService service) {
    logger.info("unbinding SchedulerService");
    schedulerService = null;
  }

  public void setWorkflowService(WorkflowService service) {
    logger.info("binding WorkflowService");
    workflowService = service;
  }

  public void unsetWorkflowService(WorkflowService service) {
    logger.info("unbinding WorkflowService");
    workflowService = null;
  }

  public void setCaptureAdminService(CaptureAgentStatusService service) {
    logger.info("binding CaptureAgentStatusService");
    captureAdminService = service;
  }

  public void unsetCaptureAdminService(CaptureAgentStatusService service) {
    logger.info("unbinding CaptureAgentStatusService");
    captureAdminService = null;
  }


  /**
   * Returns a list of recordings in a certain state.
   * @param state state according to which the recordings should filtered
   * @return recordings list of recordings in specified state
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recordings/{state}")
  public RecordingDataViewListImpl getRecordings(@PathParam("state") String state) {
    RecordingDataViewListImpl out = new RecordingDataViewListImpl();
    if ( (state.toUpperCase().equals("UPCOMING")) || (state.toUpperCase().equals("ALL")) ) {
      out.addAll(getUpcomingRecordings());
    }
    if ( (state.toUpperCase().equals("PROCESSING")) || (state.toUpperCase().equals("ALL")) ) {
      out.addAll(getRecordingsFromWorkflowService(State.RUNNING));
    }
    if ( (state.toUpperCase().equals("FINISHED")) || (state.toUpperCase().equals("ALL")) ) {
      out.addAll(getRecordingsFromWorkflowService(State.SUCCEEDED));
    }
    return out;
  }

  /**
   * returns a RecordingDataViewList of recordings that are currently begin processed. 
   * If the WorkflowService is not present an empty list is returned.
   * @return RecordingDataViewList list of upcoming recordings
   */
  private RecordingDataViewList getRecordingsFromWorkflowService(State state) {
    RecordingDataViewList out = new RecordingDataViewListImpl();
    if (workflowService != null) {
      logger.info("getting currently processed/finished recordings from workflowService");
      WorkflowInstance[] workflows = workflowService.getWorkflowInstances(workflowService.newWorkflowQuery().withState(state)).getItems();
      // next line is for debuging: return all workflowInstaces
      //WorkflowInstance[] workflows = workflowService.getWorkflowInstances(workflowService.newWorkflowQuery()).getItems();
      for (int i = 0; i < workflows.length; i++) {
        RecordingDataView item = new RecordingDataViewImpl();
        item.setId(workflows[i].getId());
        item.setId(workflows[i].getId());
        DublinCoreCatalog dcCatalog = getDublinCore(workflows[i].getCurrentMediaPackage());
        if (dcCatalog != null) {
          item.setTitle(getDublinCoreProperty(dcCatalog, DublinCoreCatalog.PROPERTY_TITLE));
          item.setPresenter(getDublinCoreProperty(dcCatalog, DublinCoreCatalog.PROPERTY_CREATOR));
          item.setSeries(getDublinCoreProperty(dcCatalog, DublinCoreCatalog.PROPERTY_IS_PART_OF));
          item.setStartTime(getDublinCoreProperty(dcCatalog, DublinCoreCatalog.PROPERTY_DATE));  // FIXME get timestamp
          item.setCaptureAgent(getDublinCoreProperty(dcCatalog, DublinCoreCatalog.PROPERTY_SPATIAL)); //FIXME get capture agent from where...?
          WorkflowOperationInstance operation = workflows[i].getCurrentOperation();
          if (operation != null) {
            item.setProcessingStatus(operation.getName());
          }
          // TODO get distribution status #openquestion is there a way to find out if a workflowOperation does distribution?
          out.add(item);
        } else {
          logger.warn("MediaPackage has no Catalog");
        }
      }
    } else {
      logger.warn("WorkflowService not present, returning empty list");
    }
    return out;
  }

  /**
   * copied from WorkflowRestService
   * @param mediaPackage
   * @return
   */
  protected DublinCoreCatalog getDublinCore(MediaPackage mediaPackage) {
    Catalog[] dcCatalogs = mediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR, MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    if(dcCatalogs.length == 0) return null;
    return (DublinCoreCatalog)dcCatalogs[0];
  }

  /**
   * copied from WorkflowRestService
   * @param catalog
   * @param property
   * @return
   */
  protected String getDublinCoreProperty(DublinCoreCatalog catalog, EName property) {
    if(catalog == null) return null;
    return catalog.getFirst(property, DublinCoreCatalog.LANGUAGE_ANY);
  }

  /**
   * Retruns simple statistics about "recordings" in the system
   * @return simple statistics about "recordings" in the system
   */
  @SuppressWarnings("unchecked")
  @GET
  @Path("countRecordings")
  public Response countRecordings() {
    HashMap<String,Integer> stats = getRecordingsStatistic();
    Iterator<Entry<String,Integer>> i = stats.entrySet().iterator();
    JSONObject out = new JSONObject();
    while (i.hasNext()) {
      Entry<String,Integer> ent = i.next();
      out.put(ent.getKey(), ent.getValue());
    }
    return Response.ok(out.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
  }

  /**
   * returns a statistic about number and state of recordings in the system
   * @return statistic about number and state of recordings in the system
   */
  public HashMap<String,Integer> getRecordingsStatistic() {
    String logMessage = "got statistics from: ";
    HashMap<String,Integer> out = new HashMap<String,Integer>();
    Integer total = new Integer(0);

    // get number of upcoming recordings if scheduler is present
    if (schedulerService != null) {
      SchedulerEvent[] events = schedulerService.getUpcomingEvents();
      out.put("upcoming", new Integer(events.length));
      logMessage += "scheduler-service";
      total += events.length;
    } else {
      logger.warn("scheduler service not present, unable to retreive number of upcoming events");
    }

    // get statistics from capture admin if present
    if (captureAdminService != null) {
      Map<String,Recording> recordings = captureAdminService.getKnownRecordings();
      Iterator<String> i = recordings.keySet().iterator();
      int capturing = 0;
      while (i.hasNext()) {
        if (recordings.get(i.next()).getState().equals(AgentState.CAPTURING)) {
          capturing++;
        }
      }
      out.put("capturing", new Integer(capturing));
      total += capturing;
      logMessage += " capture-admin-service";
    } else {
      logger.warn("CaptureAdmin service not present, unable to retrieve capture statistics");
    }

    // get statistics from workflowService if present
    if (workflowService != null) {
      WorkflowInstance[] workflows = workflowService.getWorkflowInstances(workflowService.newWorkflowQuery()).getItems();
      int i = 0, processing = 0, inactive = 0, finished = 0, errors = 0;
      for (; i < workflows.length; i++) {
        switch (workflows[i].getState()) {
          case FAILED:
          case FAILING:
            errors++;
            break;
          case INSTANTIATED:
          case RUNNING:
            processing++;
            break;
          case PAUSED:
          case STOPPED:
            inactive++; 
            break;
          case SUCCEEDED:
            finished++;
        }
      }
      out.put("processing", Integer.valueOf(processing));
      out.put("inactive", Integer.valueOf(inactive));
      out.put("errors", Integer.valueOf(errors));
      out.put("finished", Integer.valueOf(finished));
      total += i;
      logMessage += " workflow-service";
    } else {
      logger.warn("workflow service not present, unable to retrieve workflow statistics");
    }
    out.put("total", total);
    logger.info(logMessage);
    return out;
  }

  /**
   * returns a RecordingDataViewList of upcoming events. If the schedulerService
   * is not present an empty list is returned.
   * @return RecordingDataViewList list of upcoming recordings
   */
  private RecordingDataViewList getUpcomingRecordings() {
    RecordingDataViewList out = new RecordingDataViewListImpl();
    if (schedulerService != null) {
      logger.info("getting upcoming recordings from scheudler");
      SchedulerEvent[] events = schedulerService.getUpcomingEvents();
      for (int i = 0; i < events.length; i++) {
        RecordingDataView item = new RecordingDataViewImpl();
        item.setId(events[i].getID());
        item.setId(events[i].getID());
        item.setTitle(events[i].getTitle());
        item.setPresenter(events[i].getCreator());
        item.setSeries(events[i].getSeriesID());    // FIXME get title for seriesID
        item.setStartTime(Long.toString(events[i].getStartdate().getTime()));
        item.setStartTime(Long.toString(events[i].getEnddate().getTime()));
        item.setCaptureAgent(events[i].getDevice());
        item.setProcessingStatus("scheduled");
        item.setDistributionStatus("not distributed");
        out.add(item);
      }
    } else {
      logger.warn("scheduler not present, returning empty list");
    }
    return out;
  }

  /**
   * @return documentation for this endpoint
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;
  
  public AdminuiRestService() {
    docs = "FIXME -- add documentation";
  }
}
