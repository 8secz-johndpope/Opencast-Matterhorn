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

package org.opencastproject.media.mediapackage.elementbuilder;

import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.attachment.CoverImpl;

/**
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognizes attachments in the Portable Document
 * Format (pdf) and creates media package element representations for them.
 * <p>
 * The test depends solely on the mimetype.
 */
public class CoverBuilderPlugin extends AbstractAttachmentBuilderPlugin implements MediaPackageElementBuilderPlugin {

  /**
   * Creates a new attachment builder that will accept attachments of type {@link org.opencastproject.media.mediapackage.Cover}.
   */
  public CoverBuilderPlugin() {
    super(MediaPackageElements.COVER_FLAVOR);
  }

  /**
   * @see org.opencastproject.media.mediapackage.elementbuilder.AbstractAttachmentBuilderPlugin#specializeAttachment(org.opencastproject.media.mediapackage.Attachment)
   */
  @Override
  protected Attachment specializeAttachment(Attachment attachment) throws UnsupportedElementException {
    try {
      return CoverImpl.fromAttachment(attachment);
    } catch (Exception e) {
      throw new UnsupportedElementException("Failed to specialize cover " + attachment + ": " + e.getMessage());
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Cover Builder Plugin";
  }

}