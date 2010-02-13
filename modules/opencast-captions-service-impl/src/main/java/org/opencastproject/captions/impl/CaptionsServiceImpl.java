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
package org.opencastproject.captions.impl;

import org.opencastproject.captions.api.CaptionsMediaItem;
import org.opencastproject.captions.api.CaptionsService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionListImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

/**
 * The captions handler service <br/>
 * Search will provide means of searching for media packages without timed text catalogs <br/>
 * The consumer will present a list of those media packages <br/>
 * The consumer sends back timed text, which will be stored in the working file repository and added to the media package <br/>
 * A new workflow (or the existing one with additional operations) will be kicked off (for redistribution including captions)
 */
public class CaptionsServiceImpl implements CaptionsService, ManagedService, WorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(CaptionsServiceImpl.class);

  private Workspace workspace;
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
  public void unsetWorkspace(Workspace workspace) {
    this.workspace = null;
  }

  private WorkflowService workflowService;
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }
  public void unsetWorkflowService(WorkflowService workflowService) {
    this.workflowService = null;
  }
  protected void activate(ComponentContext context) {
    logger.info("ACTIVATE"); // Called when all service dependencies are in place
  }
  protected void deactivate(ComponentContext ctxt) {
    logger.info("SHUTDOWN"); // Called on shutdown
  }

  public CaptionsServiceImpl() {
    logger.info("CONSTRUCT");
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.captions.api.CaptionsService#getCaptionableMedia(int, int, java.lang.String)
   */
  public CaptionsResults getCaptionableMedia(int start, int max, String sort) {
    List<CaptionsMediaItem> l = new ArrayList<CaptionsMediaItem>();
    if (start < 0) {
      start = 0;
    }
    if (max < 0 || max > 50) {
      max = 50;
    }
    // TODO make this actually get the captionable items using get workflows with state and something else
    WorkflowQuery q = workflowService.newWorkflowQuery();
    //q.withState(State.PAUSED).withLimit(max).withOffset(start); // MH-1743
    q.withCount(max).withStartPage(start); // MH-1743
    WorkflowSet wfs = workflowService.getWorkflowInstances(q);
    int total = (int) wfs.size();
    WorkflowInstance[] workflows = wfs.getItems();
    for (WorkflowInstance workflow : workflows) {
      //WorkflowOperationInstance operation = workflow.getCurrentOperation(); // MH-1743
      //if ( CAPTIONS_OPERATION_NAME.equals(operation.getName()) ) { // MH-1743
        MediaPackage mp = workflow.getCurrentMediaPackage();
        l.add( new CaptionsMediaItemImpl(workflow.getId(), mp) );
      //} // MH-1743
    }
    return new CaptionsResults(l, start, max, total);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.captions.api.CaptionsService#updateCaptions(java.lang.String, java.lang.String, java.io.InputStream)
   */
  public CaptionsMediaItem updateCaptions(String workflowId, String captionType, InputStream data) {
    if (workflowId == null || "".equals(workflowId)) {
      throw new IllegalArgumentException("workflowId must be set");
    }
    if (captionType == null || "".equals(captionType)) {
      throw new IllegalArgumentException("captionType must be set");
    }
    if (data == null) {
      throw new IllegalArgumentException("data must be set");
    }
    // find the media package given a workflow
    WorkflowInstance workflow = workflowService.getWorkflowById(workflowId);
    CaptionsMediaItem cmi;
    if (workflow != null) {
      MediaPackage mp = workflow.getCurrentMediaPackage(); // TODO change to current media package
      // get the MP and update it
      String mediaPackageElementID = CAPTIONS_ELEMENT+captionType;
      URI uri = workspace.put(mp.getIdentifier().compact(), mediaPackageElementID, data);

      if (WorkflowInstance.State.SUCCEEDED.equals(workflow.getState())) {
        // TODO for now this is not really doing anything
        addCaptionToMediaPackage(mp, uri, mediaPackageElementID, captionType);
        WorkflowDefinitionImpl workflowDefinition = new WorkflowDefinitionImpl();
        workflowDefinition.setTitle("Captions Added");
        workflowDefinition.setDescription("Captions added workflow for media: " + workflowId);
        // TODO what is this and what do I do with it?
        workflowDefinition.setOperations(new WorkflowOperationDefinitionListImpl());
        workflowService.start(workflowDefinition, mp, null);
      } else if (WorkflowInstance.State.PAUSED.equals(workflow.getState())) {
        MediaPackage mediaPackage = workflow.getSourceMediaPackage();
        addCaptionToMediaPackage(mediaPackage, uri, mediaPackageElementID, captionType);
        workflowService.update(workflow);
        workflowService.resume(workflow.getId());
      } else {
        logger.warn("Workflow (" + workflowId + ") is in invalid state for captioning: " + workflow.getState());
      }
      cmi = new CaptionsMediaItemImpl(workflowId, mp);
    } else {
      throw new IllegalArgumentException("No workflow found with the given id: " + workflowId);
    }
    return cmi;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.captions.api.CaptionsService#getCaptionsMediaItem(java.lang.String)
   */
  public CaptionsMediaItem getCaptionsMediaItem(String workflowId) {
    if (workflowId == null || "".equals(workflowId)) {
      throw new IllegalArgumentException("workflowId must be set");
    }
    // find the media package given a workflow
    WorkflowInstance workflow = workflowService.getWorkflowById(workflowId);
    CaptionsMediaItem cmi;
    if (workflow != null) {
      MediaPackage mp = workflow.getSourceMediaPackage(); // TODO change to current media package
      cmi = new CaptionsMediaItemImpl(workflowId, mp);
    } else {
      cmi = null;
    }
    return cmi;
  }

  private void addCaptionToMediaPackage(MediaPackage mediaPackage, URI uri, String elementId, String type) {
    if (mediaPackage == null || uri == null || elementId == null || type == null) {
      throw new IllegalArgumentException("All values must not be null: " + mediaPackage + " : " + uri + " : " + elementId + " : " + type);
    }
    MediaPackageElementBuilder mpeb = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    MediaPackageElementFlavor captionsFlavor = CaptionsMediaItemImpl.makeCaptionsFlavor(type);
    try {
      MediaPackageElement element = mpeb.elementFromURI(uri, MediaPackageElement.Type.Catalog, captionsFlavor);
      element.setIdentifier(elementId);
      mediaPackage.add(element);
      logger.info("Updated the media package ({}) caption ({}): {}",
              new Object[] {mediaPackage.getIdentifier().compact(), elementId, uri});
    } catch (UnsupportedElementException e) {
      logger.error(e.toString(), e);
      throw new IllegalStateException("Failed while adding caption to media package ("+mediaPackage.getIdentifier().compact()+"):" + e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#run(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult run(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(
            workflowInstance.getSourceMediaPackage(), null, true);
  }

}
