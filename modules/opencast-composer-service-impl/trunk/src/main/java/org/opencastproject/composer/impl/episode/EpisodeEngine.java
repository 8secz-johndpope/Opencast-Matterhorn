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

package org.opencastproject.composer.impl.episode;

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.impl.AbstractEncoderEngine;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.util.ConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * Wrapper for the Telestream Episode Compression Engine.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: EpisodeEngine.java 2941 2009-08-19 08:00:12Z ced $
 */
public class EpisodeEngine extends AbstractEncoderEngine {

  /** Name of the properties file */
  public static final String EPISODE_PROPERTIES = "/episode.properties";
  
  /** The episode inbox option name */
  public static final String OPT_MONITORTYPE = "episode.type";

  /** The episode monitor frequency option name */
  public static final String OPT_EPISODE_MONITOR_FREQUENCY = "episode.frequency";

  /** The episodes xmlrpc host option name */
  public static final String OPT_XMLRPC_HOST = "episode.xmlrpc.host";

  /** The episodes xmlrpc port option name */
  public static final String OPT_XMLRPC_PORT = "episode.xmlrpc.port";

  /** The episodes xmlrpc path option name */
  public static final String OPT_XMLRPC_PATH = "episode.xmlrpc.path";

  /** The episodes xmlrpc password option name */
  public static final String OPT_XMLRPC_PASSWORD = "episode.xmlrpc.password";

  /** Default monitor interval */
  public static final long DEFAULT_MONITOR_FREQUENCY = 60;

  /** Default xmlrpc port number */
  public static final int DEFAULT_XMLRPC_PORT = 40406;

  /** Default xmlrpc password */
  public static final String XMLRPC_DEFAULT_PASSWORD = "anonymous";

  /** Medium priority */
  public static final int PRIORITY_MEDIUM = 500;

  /** Encoding errors */
  protected int errors = 0;

  /** Xmlrpc hostname */
  protected String xmlrpcHostname = null;

  /** Xmlrpc port */
  protected int xmlrpcPort = DEFAULT_XMLRPC_PORT;

  /** Xmlrpc path on the server */
  protected String xmlrpcPath = null;

  /** Password used to connect to the xmlrpc service */
  protected String xmlrpcPassword = null;

  /** The monitor frequency */
  protected long monitorFrequency = DEFAULT_MONITOR_FREQUENCY;

  /** The monitor thread */
  private Thread monitorThread = null;

  /** The xmlrpc base encoder monitor */
  private XmlRpcEngineController xmlrpcController = null;

  /** the logging facility provided by log4j */
  static Logger log_ = LoggerFactory.getLogger(EpisodeEngine.class.getName());

  /**
   * Creates a new instance of the episode telestream engine wrapper.
   */
  public EpisodeEngine() {
    super(true);

    // Load the local properties file
    try {
      Properties properties = new Properties();
      properties.load(EpisodeEngine.class.getResourceAsStream(EPISODE_PROPERTIES));
      configure(properties);
    } catch (IOException e) {
      throw new ConfigurationException("Reading properties for episode engine failed: " + e.getMessage());
    }

    // Start the monitor
    monitorThread.setDaemon(true);
    monitorThread.start();
  }

  /**
   * Creates a new instance of the episode telestream engine wrapper.
   */
  public EpisodeEngine(Properties config) {
    super(true);

    // Use the passed in configuration
    configure(config);

    // Start the monitor
    monitorThread.setDaemon(true);
    monitorThread.start();
  }

  /**
   * Configures the compression engine wrapper for use with an xmlrpc controller.
   * 
   * @param properties
   *          the engine configuration
   */
  private void configure(Properties properties) throws ConfigurationException {
    try {
      // Xmlrpc hostname
      xmlrpcHostname = (String) properties.get(OPT_XMLRPC_HOST);
      log_.debug("Episode xmlrpc host is " + xmlrpcHostname);

      // Xmlrpc port
      String port = (String) properties.get(OPT_XMLRPC_PORT);
      if (port != null && !"".equals(port)) {
        try {
          xmlrpcPort = Integer.parseInt(port);
        } catch (Exception e) {
          throw new ConfigurationException("Episode sdk port number '" + port + "' is malformed: " + e.getMessage());
        }
      }
      log_.debug("Episode xmlrpc port number is " + xmlrpcPort);

      // Xmlrpc mountpoint
      xmlrpcPath = (String) properties.get(OPT_XMLRPC_PATH);
      if (xmlrpcPath == null)
        throw new ConfigurationException("Episode sdk path not specified");
      log_.debug("Episode xmlrpc path is " + xmlrpcPath);

      // Xmlrpc password
      xmlrpcPassword = (String) properties.get(OPT_XMLRPC_PASSWORD);
      if (xmlrpcPath == null) {
        xmlrpcPassword = XMLRPC_DEFAULT_PASSWORD;
        log_.debug("Episode xmlrpc password was not set, using default");
      } else {
        log_.debug("Episode xmlrpc password was set to custom value");
      }

      // Set monitor frequency
      try {
        monitorFrequency = Long.parseLong((String) properties.get(OPT_EPISODE_MONITOR_FREQUENCY));
      } catch (Exception e) {
      }
      log_.debug("Engine updates are gathered every " + monitorFrequency + " s");

      // Start the monitor
      xmlrpcController = new XmlRpcEngineController(this, xmlrpcHostname, xmlrpcPort, xmlrpcPath, xmlrpcPassword,
              monitorFrequency * 1000L);
      monitorThread = new Thread(xmlrpcController);

    } catch (Exception e) {
      throw new ConfigurationException("Episode engine setup failed: " + e.getMessage());
    }
  }

  /**
   * Stops the epsisode compression engine including all their tasks like outfolder watchdogs.
   */
  public void stop() {
    if (monitorThread != null) {
      if (xmlrpcController != null)
        xmlrpcController.stop();
      monitorThread.interrupt();
      xmlrpcController = null;
      monitorThread = null;
    }
  }

  /**
   * Returns the hostname for the xmlprc server of the episode engine.
   * <p>
   * The hostname parameter is gathered from the configuration file <code>episode.properties</code>.
   * </p>
   * 
   * @return the xmlrpc hostname
   */
  public String getXmlrpcHost() {
    return xmlrpcHostname;
  }

  /**
   * Returns the port number for the xmlprc server of the episode engine.
   * <p>
   * The port parameter is gathered from the configuration file <code>episode.properties</code>.
   * </p>
   * 
   * @return the xmlrpc hostname
   */
  public int getXmlrpcPort() {
    return xmlrpcPort;
  }

  /**
   * Returns the path for the xmlprc server of the episode engine.
   * <p>
   * The path parameter is gathered from the configuration file <code>episode.properties</code>.
   * </p>
   * 
   * @return the xmlrpc path on the server
   */
  public String getXmlrpcPath() {
    return xmlrpcPath;
  }

  /**
   * @see org.opencastproject.composer.api.api.composer.EncoderEngine#needsLocalWorkCopy()
   */
  public boolean needsLocalWorkCopy() {
    return true;
  }

  /**
   * @see ch.ethz.replay.core.composer.AbstractEncoderEngine#encodeTrack(ch.ethz.replay.core.api.common.bundle.Track,
   *      ch.ethz.replay.core.api.common.media.EncodingProfile)
   */
  @Override
  public void encodeTrack(Track track, EncodingProfile format) throws EncoderException {
    xmlrpcController.submitJob(track, format);
  }

  /**
   * Callback from the engine controllers stating that encoding of the given file has been successful.
   * 
   * @param track
   *          the track that was encoded
   * @param profile
   *          the encoding profile
   */
  void trackEncoded(Track track, EncodingProfile profile) {
    fireTrackEncoded(this, track, profile);
  }

  /**
   * Callback from the engine controllers stating that encoding of the given file has has failed for the specified
   * reason.
   * 
   * @param track
   *          the track that was encoded
   * @param profile
   *          the encoding profile
   * @param reason
   *          the reason of failure
   */
  void trackEncodingFailed(Track track, EncodingProfile profile, String reason) {
    fireTrackEncodingFailed(this, track, profile, new EncoderException(this, reason));
  }

  /**
   * Callback from the engine controllers stating that encoding of the given file has progressed to the given value.
   * 
   * @param track
   *          the track that is being encoded
   * @param profile
   *          the encoding profile
   * @param progress
   */
  void trackEncodingProgressed(Track track, EncodingProfile profile, int progress) {
    fireTrackEncodingProgressed(this, track, profile, progress);
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Telestream Episode Engine";
  }

}