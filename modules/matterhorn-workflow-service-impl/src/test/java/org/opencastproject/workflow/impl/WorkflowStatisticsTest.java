/**
 *  Copyright 2009, 2010 The Regents of the University of California
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

import static org.junit.Assert.assertEquals;

import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandlerBase;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowStatistics;
import org.opencastproject.workflow.api.WorkflowStatistics.WorkflowDefinitionReport;
import org.opencastproject.workflow.api.WorkflowStatistics.WorkflowDefinitionReport.OperationReport;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Test cases for the implementation at {@link WorkflowStatistics}.
 */
public class WorkflowStatisticsTest {

  private static final Logger logger = LoggerFactory.getLogger(WorkflowStatisticsTest.class);

  /** Number of operations per workflow */
  private static final int WORKFLOW_DEFINITION_COUNT = 2;

  /** Number of operations per workflow */
  private static final int OPERATION_COUNT = 5;

  private WorkflowServiceImpl workflowService = null;
  private List<WorkflowDefinition> workflowDefinitions = null;
  private Set<HandlerRegistration> workflowHandlers = null;
  private WorkflowServiceDaoSolrImpl dao = null;
  private Workspace workspace = null;
  private MediaPackage mediaPackage = null;

  @Before
  public void setup() throws Exception {
    // always start with a fresh solr root directory
    String storageRoot = "." + File.separator + "target" + File.separator + "workflow-test-db" + File.separator
            + System.currentTimeMillis();
    File sRoot = new File(storageRoot);
    try {
      FileUtils.forceMkdir(sRoot);
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    workflowDefinitions = new ArrayList<WorkflowDefinition>();
    workflowHandlers = new HashSet<HandlerRegistration>();

    // create operation handlers for our workflows
    for (int i = 1; i <= WORKFLOW_DEFINITION_COUNT; i++) {
      WorkflowDefinition workflowDef = new WorkflowDefinitionImpl();
      workflowDef.setId("workflow-" + i);
      for (int opCount = 1; opCount <= OPERATION_COUNT; opCount++) {

        // Create a new operation
        String opId = "op-" + opCount;
        String opDescription = "workflow operation " + opCount;
        WorkflowOperationDefinition op = new WorkflowOperationDefinitionImpl(opId, opDescription, null, true);
        workflowDef.add(op);

        // Register a handler form the operation
        HandlerRegistration handler = new HandlerRegistration(opId, new TestOperationHandler(opId));
        if (!workflowHandlers.contains(handler))
          workflowHandlers.add(handler);
      }
      workflowDefinitions.add(workflowDef);
    }

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    workflowService = new WorkflowServiceImpl() {
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return workflowHandlers;
      }
    };

    // Register the workflow definitions
    for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
      workflowService.registerWorkflowDefinition(workflowDefinition);
    }

    // Mock the workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.getCollectionContents((String) EasyMock.anyObject())).andReturn(new URI[0]);
    EasyMock.replay(workspace);

    // Mock the service registry
    ServiceRegistry serviceRegistry = new MockServiceRegistry();
    workflowService.setServiceRegistry(serviceRegistry);

    // Create the workflow database (solr)
    dao = new WorkflowServiceDaoSolrImpl();
    dao.solrRoot = storageRoot + File.separator + "solr." + System.currentTimeMillis();
    dao.setServiceRegistry(serviceRegistry);
    dao.activate();
    workflowService.setDao(dao);
    workflowService.activate(null);

    // Ensure the workflow service has an unbounded thread pool for testing
    workflowService.executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    // Crate a media package
    InputStream is = null;
    try {
      MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
      is = WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-1.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
      IOUtils.closeQuietly(is);
      Assert.assertNotNull(mediaPackage.getIdentifier());
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @After
  public void teardown() throws Exception {
    System.out.println("All tests finished... tearing down...");
    Thread.sleep(1000);
    dao.deactivate();
    workflowService.deactivate();
  }

  /**
   * Tests whether the workflow service statistics are gathered correctly while there are no workflows active in the
   * system. Since no workflows are known, not even empty definition reports are to be expected.
   */
  @Test
  public void testEmptyStatistics() throws Exception {
    WorkflowStatistics stats = workflowService.getStatistics();
    assertEquals(0, stats.getDefinitions().size());
    assertEquals(0, stats.getFailed());
    assertEquals(0, stats.getFailing());
    assertEquals(0, stats.getFinished());
    assertEquals(0, stats.getInstantiated());
    assertEquals(0, stats.getPaused());
    assertEquals(0, stats.getRunning());
    assertEquals(0, stats.getStopped());
    assertEquals(0, stats.getTotal());
  }

  /**
   * Tests whether the workflow service statistics are gathered correctly.
   */
  @Test
  public void testStatistics() throws Exception {

    // Start the workflows and advance them in "random" order. With every definition, an instance is started for every
    // operation that is part of the definition. So we end up with an instance per definition and operation, and there
    // are no two workflows that are in the same operation.

    int total = 0;
    int paused = 0;
    int failed = 0;
    int failing = 0;
    int instantiated = 0;
    int running = 0;
    int stopped = 0;
    int succeeded = 0;

    for (WorkflowDefinition def : workflowDefinitions) {
      for (int j = 0; j < def.getOperations().size(); j++) {
        WorkflowInstance instance = workflowService.start(def, mediaPackage);
        for (int k = 0; k <= j; k++) {
          instance = workflowService.resume(instance.getId(), null);
        }
        total++;
        paused++;
      }
    }

    Thread.sleep(3000);

    // TODO: Add failed, failing, stopped etc. workflows as well

    // Get the statistics
    WorkflowStatistics stats = workflowService.getStatistics();
    assertEquals(failed, stats.getFailed());
    assertEquals(failing, stats.getFailing());
    assertEquals(instantiated, stats.getInstantiated());
    assertEquals(succeeded, stats.getFinished());
    assertEquals(paused, stats.getPaused());
    assertEquals(running, stats.getRunning());
    assertEquals(stopped, stats.getStopped());
    assertEquals(total, stats.getTotal());

    // TODO: Test the operations
    // Make sure they are as expected
    // for (WorkflowDefinitionReport report : stats.getDefinitions()) {
    //
    // }

  }

  /**
   * 
   * @throws Exception
   */
  @Test
  public void testStatisticsMarshalling() throws Exception {
    WorkflowStatistics stats = new WorkflowStatistics();
    stats.setFailed(100);
    stats.setInstantiated(20);

    OperationReport op1 = new OperationReport();
    op1.setId("compose");
    op1.setInstantiated(10);
    op1.setFailing(1);

    List<OperationReport> ops1 = new ArrayList<WorkflowStatistics.WorkflowDefinitionReport.OperationReport>();
    ops1.add(op1);

    WorkflowDefinitionReport def1 = new WorkflowDefinitionReport();
    def1.setFailed(40);
    def1.setInstantiated(10);
    def1.setOperations(ops1);
    def1.setId("def1");
    def1.setOperations(ops1);

    WorkflowDefinitionReport def2 = new WorkflowDefinitionReport();
    def1.setFailed(60);
    def1.setInstantiated(10);

    List<WorkflowDefinitionReport> reports = new ArrayList<WorkflowDefinitionReport>();
    reports.add(def1);
    reports.add(def2);
    stats.setDefinitions(reports);

    logger.info(WorkflowParser.toXml(stats));
  }

  /**
   * Utility implementation for a resumable workflow operation handler.
   */
  public static class TestOperationHandler extends ResumableWorkflowOperationHandlerBase {

    /**
     * Creates a new test operation handler that is dealing with operations of id <code>operationId</code>.
     * 
     * @param operationId
     *          the operation identifier
     */
    public TestOperationHandler(String operationId) {
      super.id = operationId;
    }
  }

}
