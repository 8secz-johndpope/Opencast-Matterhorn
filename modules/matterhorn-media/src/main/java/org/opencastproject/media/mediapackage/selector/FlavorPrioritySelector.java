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
package org.opencastproject.media.mediapackage.selector;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This selector will return one or zero <code>MediaPackageElements</code> from a <code>MediaPackage</code>, following
 * these rules:
 * <ul>
 * <li>Elements will be returned depending on tags that have been set</li>
 * <li>If no tags have been specified, all the elements will be taken into account</li>
 * <li>The result is one or zero elements</li>
 * <li>The element is selected based on the order of flavors</li>
 * </ul>
 */
public class FlavorPrioritySelector<T extends MediaPackageElement> extends SimpleMediaPackageElementSelector<T> {

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackageElementSelector#select(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public Collection<T> select(MediaPackage mediaPackage) {
    Set<T> candidates = new HashSet<T>();
    Set<T> result = new HashSet<T>();

    // Have the super implementation match type, flavor and tags
    candidates.addAll(super.select(mediaPackage));

    // Return the first element based on the flavor
    result: for (MediaPackageElementFlavor flavor : flavors) {
      for (T element : candidates) {
        if (flavor.equals(element.getFlavor())) {
          result.add(element);
          break result;
        }
      }
    }

    return result;
  }

}