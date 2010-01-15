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
package org.opencastproject.distribution.local;

import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Dictionary;

/**
 * Distributes media to the local media delivery directory. TODO: Add metadata to the search service.
 */
public class DistributionServiceImpl implements DistributionService, ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(DistributionServiceImpl.class);
  public static final String DEFAULT_DISTRIBUTION_DIR = "opencast" + File.separator + "static";
  protected Workspace workspace = null;
  protected File distributionDirectory = null;
  protected String serverUrl = null;

  /**
   * Creates a local distribution service publishing to the default directory {@link #DEFAULT_DISTRIBUTION_DIR} located
   * in <code>java.io.tmmpdir</code>.
   */
  public DistributionServiceImpl() {
    this(new File(System.getProperty("java.io.tmpdir") + File.separator + DEFAULT_DISTRIBUTION_DIR));
  }

  /**
   * Creates a local distribution service that will move files to the given directory.
   * 
   * @param distributionRoot
   *          the distribution directory
   */
  public DistributionServiceImpl(File distributionRoot) {
    this.distributionDirectory = distributionRoot;
  }

  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL + "/static";
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("serverUrl");
      logger.info("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL + "/static";
      } else {
        serverUrl = ccServerUrl + "/static";
      }
    }
  }

  /**
   * Creates directories for the media package and subdirectories for media, metadata, and attachments. {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public MediaPackage distribute(MediaPackage mediaPackage, String... elementIds) {
    Arrays.sort(elementIds);
    File mediaPackageDirectory = new File(distributionDirectory, mediaPackage.getIdentifier().compact());
    File mediaDirectory = new File(mediaPackageDirectory, "media");
    File metadataDirectory = new File(mediaPackageDirectory, "metadata");
    File attachmentsDirectory = new File(mediaPackageDirectory, "attachments");
    try {
      FileUtils.forceMkdir(mediaPackageDirectory);
      logger.info("created directory {}", mediaPackageDirectory);
      FileUtils.forceMkdir(mediaDirectory);
      FileUtils.forceMkdir(metadataDirectory);
      FileUtils.forceMkdir(attachmentsDirectory);
      for (String id : elementIds) {
        MediaPackageElement element = mediaPackage.getElementById(id);
        File sourceFile = workspace.get(element.getURI());
        File destination = new File(mediaDirectory, getTargetFileName(element));
        FileUtils.copyFile(sourceFile, destination);
        URI newTrackUri = new URI(serverUrl + "/" + mediaPackageDirectory.getName() + "/" + mediaDirectory.getName()
                + "/" + destination.getName());
        Track newTrack = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
                newTrackUri, element.getElementType(), element.getFlavor());
        newTrack.setIdentifier(element.getIdentifier() + "-dist");
        mediaPackage.addDerived(newTrack, element);
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return mediaPackage;
  }

  protected String getTargetFileName(MediaPackageElement element) {
    return element.getIdentifier() + "." + FilenameUtils.getExtension(element.getURI().toString());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("unchecked")
  public void updated(Dictionary properties) throws ConfigurationException {
    String updatedRootDir = (String) properties.get("distributionDirectory");
    if (updatedRootDir != null) {
      File f = new File(updatedRootDir);
      if (f.exists()) {
        this.distributionDirectory = f;
        logger.info("Set distribution directory to {}", updatedRootDir);
      } else {
        logger.warn("Can not set distribution directory to {}.  Directory does not exist", updatedRootDir);
      }
    }
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

}
