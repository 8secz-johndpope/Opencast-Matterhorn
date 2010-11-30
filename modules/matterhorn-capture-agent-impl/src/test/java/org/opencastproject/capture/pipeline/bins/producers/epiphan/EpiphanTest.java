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
package org.opencastproject.capture.pipeline.bins.producers.epiphan;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.gstreamer.Gst;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.BinTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.producers.ProducerType;
import org.opencastproject.util.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for Epiphan producer bins testing.
 * (De)Initialize a JUnit test environment.
 */
public abstract class EpiphanTest {

  private static final Logger logger = LoggerFactory.getLogger(EpiphanTest.class);

  /** True to run the tests */
  protected static boolean gstreamerInstalled = true;

  /** Location of Epiphan VGA2USB device */
  protected final static String epiphanLocation = "/dev/video1";

  /** Properties specifically designed for unit testing */
  protected Properties properties;

  /** Capture Device created for unit testing */
  protected CaptureDevice captureDevice;

  /** Capture Device Properties created for unit testing **/
  protected Properties captureDeviceProperties;


  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }

  @Before
  public void setup() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;

    captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties(null, null, null, null, null, null, null,
            null, null);
    captureDevice = BinTestHelpers.createCaptureDevice(epiphanLocation, ProducerType.EPIPHAN_VGA2USB,
              "Epiphan VGA 2 USB", System.getProperty("java.io.tmpdir")+"/test.mpeg", captureDeviceProperties);

    // setup testing properties
    properties = new Properties();
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION,
            System.getProperty("java.io.tmpdir")+"/confidence");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE, "false");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION,
            System.getProperty("java.io.tmpdir")+"/confidence");
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;

    properties = null;
    captureDevice = null;
    FileUtils.deleteQuietly(new File(System.getProperty("java.io.tmpdir"), "testpipe"));
  }

  @AfterClass
  public static void deinitGst() {
    if (!gstreamerInstalled)
      return;

    Gst.deinit();
  }
}