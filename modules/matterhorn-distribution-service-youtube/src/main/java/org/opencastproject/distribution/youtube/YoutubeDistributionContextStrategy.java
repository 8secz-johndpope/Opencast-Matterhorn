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
package org.opencastproject.distribution.youtube;

import org.opencastproject.distribution.api.DistributionContextStragety;
import org.opencastproject.media.mediapackage.MediaPackage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Strategy to determine destination of distribution.
 */
public class YoutubeDistributionContextStrategy implements DistributionContextStragety {
  /** logger instance */
  private static final Logger logger = LoggerFactory.getLogger(YoutubeDistributionContextStrategy.class);

  /**
   * Returns a series identifier of the mediapackage.
   * 
   * @param mp The mediapackage
   * @return The context ID
   */
  public String getContextId(MediaPackage mp) {
    // map a media package to an intermediate ID
    String series = mp.getSeriesTitle();

    return series;
  }

  /**
   * Gets the name for a context within a distribution channel.
   * 
   * @param mp The mediapackage
   * @return The playlist ID
   */
  public String getContextName(MediaPackage mp) {
    // get an intermediate ID
    String id = getContextId(mp);

    if (id == null) {
      // distribution service will use the default
      return null;
    }

    // use database table to map the context ID to a playlist ID
    // The table needs to be created and managed elsewhere. A playlist on Youtube needs to be
    // created manually to have permission and configuration settings properly setup, and the
    // creator of the media package determines which playlist this series title maps to.
    // The table looks like the following (in concept):
    // +------------------------------------+---------------+-------------+-----------------------+
    // | Series Title    | Playlist ID      | Playlist Name | Tab Handle  | Tab Name              |
    // +------------------------------------+---------------+-------------+-----------------------+
    // | Art History 101 | B8B47104C2C1663B | Art           | 03386773035 | Art History           |
    // | Physics 235     | A7A32342323B334A | Science       | 04388234023 | Science & Engineering |
    // | ...             | ...              | ...           | ...         | ...                   |
    // +-----------------+------------------+---------------+-------------+-----------------------+
    // CHANGE ME: return YoutubeContextStrategyMap.get(id);
    return null;
  }
}