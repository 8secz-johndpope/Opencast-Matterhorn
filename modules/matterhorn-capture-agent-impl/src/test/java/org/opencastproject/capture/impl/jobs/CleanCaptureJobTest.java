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
package org.opencastproject.capture.impl.jobs;

import org.opencastproject.capture.impl.CaptureParameters;
import org.opencastproject.capture.impl.RecordingImpl;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;


/**
 * Tests for the CleanCaptureJob class
 */
public class CleanCaptureJobTest {

  private static final Logger logger = LoggerFactory.getLogger(CleanCaptureJobTest.class);

  private static final long CONVERSION_RATE =  5 * 1000; // five seconds

  Properties props = null;
  CleanCaptureJob theJob = null;
  RecordingImpl[] theRecordings = null;

  int numberOfRecordings = 5;

  File baseDir = new File(getClass().getClassLoader().getResource(".").getPath(), "cleanTest");

  @Before
  public void setUp() {
    // Define particular instances for the CleanCaptureJob required arguments
    props = new Properties();
    theJob = new CleanCaptureJob();
    theRecordings = new RecordingImpl[numberOfRecordings];

    baseDir.mkdir();
    if (!baseDir.exists())
      Assert.fail();

    try {
      for (int i = 0; i < numberOfRecordings; i++) {
        // Create the directory for the new recording 
        File recDir = new File(baseDir, new String("recording" + i));
        recDir.mkdir();

        // Set the created directory in the properties file passed to the recording
        props.setProperty(CaptureParameters.RECORDING_ROOT_URL, recDir.getAbsolutePath());

        // Create the capture.ingested file only for the upper half of the array
        // This is to simulate that the "central" value was ingested in the current time, the lower indexes, some time ago,
        // and the upper indexes are not ingested yet
        if (i <= numberOfRecordings/2) {
          File ingestFile = new File(recDir,CaptureParameters.CAPTURE_INGESTED_FILE);

          // Set the modification date for the file
          long modifDate = System.currentTimeMillis() + (i - numberOfRecordings/2)*CONVERSION_RATE; 

          // Check the "capture.ingested" file is created and its 'last modified' date set
          if (!ingestFile.isFile())
            Assert.assertTrue(ingestFile.createNewFile());
          Assert.assertTrue(ingestFile.setLastModified(modifDate));
        }

        // Creates the recording
        theRecordings[i] = new RecordingImpl(null, props);
      }
    } catch (IllegalArgumentException e) {
      logger.error("Unexpected Illegal Argument Exception when creating test recordings: {}", e.getMessage());
      return;
    } catch (IOException e) {
      logger.error("Unexpected I/O Exception when creating test recordings: {}", e.getMessage());
      return;
    } 
  }

  @After
  public void tearDown() {
    props = null;
    for (int i = 0; i < numberOfRecordings; i++)
      FileUtils.deleteQuietly(theRecordings[i].getDir());

    theRecordings = null;
  }

  @Test
  /**
   * The minimum space allowed in disk is set to zero and the maximum archival time is zero
   */
  public void diskZeroArchivalZero() {
    // Insert properties accordingly for this test
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS, "0");
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MIN_DISK_SPACE, "0");

    // Should clean the recordings in the indexes 0, 1 and 2
    theJob.doCleaning(props,theRecordings);

    // Check the cleaning was OK
    for (int i = 0; i < numberOfRecordings; i++) 
      if (i <= numberOfRecordings/2)
        Assert.assertFalse(theRecordings[i].getDir().exists());
      else
        Assert.assertTrue(theRecordings[i].getDir().exists());

  }

  @Test
  /**
   * The minimum free space in disk is zero and the maximum archival time is "infinity"
   */
  public void diskZeroArchivalLots() {
    // Insert properties for this test
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS, String.valueOf(Long.MAX_VALUE));
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MIN_DISK_SPACE, "0");

    // Should leave all the recordings untouched
    theJob.doCleaning(props, theRecordings);

    //Check the cleaning was OK
    for (RecordingImpl aRec : theRecordings)
      Assert.assertTrue(aRec.getDir().exists());
  }

  @Test
  /**
   * The minimum free space in disk is a lot and the maximum archival time is "infinity". All recordings have been ingested
   */
  public void diskLotsArchivalLotsAllIngested() {
    // Insert properties for this test
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS, String.valueOf(Long.MAX_VALUE));
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MIN_DISK_SPACE, String.valueOf(Long.MAX_VALUE));
    try {
      // Create a capture.ingested file for the recordings that don't have it
      for (RecordingImpl aRec : theRecordings) {
        File ingested = new File(aRec.getDir(), CaptureParameters.CAPTURE_INGESTED_FILE);
        if (!ingested.isFile())
          Assert.assertTrue(ingested.createNewFile());
      }       
    } catch (IOException e) {
      logger.error("Unexpected I/O exception: {}", e.getMessage());
      Assert.fail();
    }
    
    // Should clean all the recordings *because of the disk space*
    theJob.doCleaning(props,theRecordings);

    // Check the cleaning was OK
    for (RecordingImpl aRec : theRecordings)
      Assert.assertFalse(aRec.getDir().exists());
  }

  @Test
  /**
   * The minimum free space in disk is a lot and the maximum archival time is "infinity". There is non-ingested recordings
   */
  public void diskLotsArchivalLots() {
    // Insert properties for this test
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS, String.valueOf(Long.MAX_VALUE));
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MIN_DISK_SPACE, String.valueOf(Long.MAX_VALUE));
        
    // Should clean all the recordings *because of the disk space*
    theJob.doCleaning(props,theRecordings);

    // Check the cleaning was OK
    for (int i = 0; i < numberOfRecordings; i++) 
      if (i <= numberOfRecordings/2)
        Assert.assertFalse(theRecordings[i].getDir().exists());
      else
        Assert.assertTrue(theRecordings[i].getDir().exists());
  }
}