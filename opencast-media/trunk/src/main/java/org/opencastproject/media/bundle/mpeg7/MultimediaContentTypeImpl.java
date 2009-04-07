/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

package org.opencastproject.media.bundle.mpeg7;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MultimediaContentTypeImpl.java 102 2009-04-03 20:05:36Z wunden
 *          $
 */
public class MultimediaContentTypeImpl<S extends ContentSegment> implements
    MultimediaContentType, Audio, Video, AudioVisual {

  /** The content type */
  protected MultimediaContentType.Type type = null;

  /** The content element identifier */
  protected String id = null;

  /** Media locator pointing to the actual file */
  protected MediaLocator mediaLocator = null;

  /** The content time contraints */
  protected MediaTime mediaTime = null;

  /** The content time contraints */
  protected TemporalDecomposition<S> temporalDecomposition = null;

  /**
   * Creates a new media content element.
   * 
   * @param type
   *          the content element type
   * @param id
   *          the content element identifier
   */
  public MultimediaContentTypeImpl(MultimediaContentType.Type type, String id) {
    this.type = type;
    this.id = id;
    this.temporalDecomposition = createTemporalDecomposition(type);
  }

  /**
   * Sets the content's media time constraints.
   * 
   * @param time
   *          the media time
   */
  public void setMediaTime(MediaTime time) {
    this.mediaTime = time;
  }

  /**
   * Sets the content's media locator.
   * 
   * @param locator
   *          the media locator
   */
  public void setMediaLocator(MediaLocator locator) {
    this.mediaLocator = locator;
  }

  /**
   * Creates a temporal decomposition for the given content type.
   * 
   * @param contentType
   *          the content type
   * @return the temporal decomposition
   */
  private TemporalDecomposition<S> createTemporalDecomposition(
      MultimediaContentType.Type contentType) {
    if (type.equals(MultimediaContentType.Type.AudioVisual))
      return new TemporalDecompositionImpl<S>(
          ContentSegment.Type.AudioVisualSegment);
    else if (type.equals(MultimediaContentType.Type.Audio))
      return new TemporalDecompositionImpl<S>(ContentSegment.Type.AudioSegment);
    else if (type.equals(MultimediaContentType.Type.Video))
      return new TemporalDecompositionImpl<S>(ContentSegment.Type.VideoSegment);
    throw new IllegalStateException(
        "Unknown multimedia content type detected: " + contentType.toString());
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.MultimediaContentType#getId()
   */
  public String getId() {
    return id;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.MultimediaContentType#getMediaLocator()
   */
  public MediaLocator getMediaLocator() {
    return mediaLocator;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.MultimediaContentType#getMediaTime()
   */
  public MediaTime getMediaTime() {
    return mediaTime;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.MultimediaContentType#getTemporalDecomposition()
   */
  public TemporalDecomposition<S> getTemporalDecomposition() {
    return temporalDecomposition;
  }

  /**
   * @see org.opencastproject.media.bundle.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = document.createElement(type.toString());
    node.setAttribute("id", id);
    if (mediaLocator != null)
      node.appendChild(mediaLocator.toXml(document));
    if (mediaTime != null)
      node.appendChild(mediaTime.toXml(document));
    if (temporalDecomposition.hasSegments())
      node.appendChild(temporalDecomposition.toXml(document));
    return node;
  }

}