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
package org.opencastproject.distribution.local;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.util.FileSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * An abstract class for implementing distribution services that copy files to filesystems, obtaining URLs for the
 * servers that will host the files for downloading or streaming.
 * 
 */
public abstract class AbstractLocalDistributionService implements DistributionService {
  private static final Logger logger = LoggerFactory.getLogger(DownloadDistributionService.class);
  protected Workspace workspace = null;

  /**
   * Gets the destination file to copy the contents of a mediapackage element.
   * 
   * @param element
   *          The mediapackage element being distributed
   * @return The file to copy the content to
   */
  protected abstract File getDistributionFile(MediaPackageElement element);

  /**
   * Gets the URI for the element to be distributed.
   * 
   * @param element
   *          The mediapackage element being distributed
   * @return The resulting URI after distribution
   * @throws URISyntaxException
   *           if the concrete implementation tries to create a malformed uri
   */
  protected abstract URI getDistributionUri(MediaPackageElement element) throws URISyntaxException;

  /**
   * Distributes the mediapackage's element to the location that is returned by the concrete implementation. In
   * addition, a representation of the distributed element is added to the mediapackage.
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public MediaPackage distribute(MediaPackage mediaPackage, String... elementIds) throws DistributionException {
    try {
      Arrays.sort(elementIds);
      for (MediaPackageElement element : mediaPackage.getElements()) {
        if (Arrays.binarySearch(elementIds, element.getIdentifier()) >= 0) {
          File sourceFile = workspace.get(element.getURI());
          File destination = getDistributionFile(element);

          // Put the file in place
          FileUtils.forceMkdir(destination.getParentFile());
          logger.info("Distributing {} to {}", element, destination);

          FileSupport.copy(sourceFile, destination);

          // Create a representation of the distributed file in the mediapackage
          MediaPackageElement clone = (MediaPackageElement) element.clone();
          clone.setURI(getDistributionUri(element));
          clone.setIdentifier(null);
          mediaPackage.addDerived(clone, element);
        }
      }
    } catch (Exception e) {
      throw new DistributionException(e);
    }
    return mediaPackage;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#retract(org.opencastproject.media.mediapackage.MediaPackage)
   */
  @Override
  public void retract(MediaPackage mediaPackage) throws DistributionException {
    throw new UnsupportedOperationException();
  }

}
