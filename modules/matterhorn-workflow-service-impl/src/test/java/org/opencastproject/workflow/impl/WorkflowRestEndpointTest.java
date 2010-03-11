/**
 *  Copyright 2010 The Regents of the University of California
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
package org.opencastproject.workflow.impl;

import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.endpoint.WorkflowRestService;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class WorkflowRestEndpointTest {
  WorkflowRestService restService;
  WorkflowInstanceImpl workflow;
  
  @Before
  public void setup() throws Exception {
    // Create a workflow for the service to return
    workflow = new WorkflowInstanceImpl();
    workflow.setTitle("a workflow instance");
    workflow.setId("workflow1");

    // Mock up the behavior of the workflow service
    WorkflowService service = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(service.listAvailableWorkflowDefinitions()).andReturn(new ArrayList<WorkflowDefinition>());
    EasyMock.expect(service.getWorkflowById((String)EasyMock.anyObject())).andReturn(null).times(2).andReturn(workflow);
    EasyMock.replay(service);

    // Set up the rest endpoint
    restService = new WorkflowRestService();
    restService.setService(service);
    restService.activate(null);
  }

  @Test
  public void testGetWorkflowDefinitions() throws Exception {
    Response jsonResponse = restService.getWorkflowDefinitions("json");
    Assert.assertEquals(200, jsonResponse.getStatus());
    Assert.assertEquals(MediaType.APPLICATION_JSON, jsonResponse.getMetadata().getFirst("Content-Type"));

    Response xmlResponse = restService.getWorkflowDefinitions("xml");
    Assert.assertEquals(200, xmlResponse.getStatus());
    Assert.assertEquals(MediaType.TEXT_XML, xmlResponse.getMetadata().getFirst("Content-Type"));
  }

  @Test
  public void testGetWorkflowInstance() throws Exception {
    Response json404Response = restService.getWorkflow("unknown_id", "json");
    Assert.assertEquals(404, json404Response.getStatus());

    Response xml404Response = restService.getWorkflow("unknown_id", "xml");
    Assert.assertEquals(404, xml404Response.getStatus());
    
    Response xmlResponse = restService.getWorkflow("workflow1", "xml");
    Assert.assertEquals(200, xmlResponse.getStatus());
    Assert.assertEquals(WorkflowBuilder.getInstance().toXml(workflow), xmlResponse.getEntity());
  }
}