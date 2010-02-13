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
package org.opencastproject.engage.api;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A JAXB-anotated implementation of {@link EpisodeView}
 */
@XmlType(name="episode", namespace="http://searchui.opencastproject.org/")
@XmlRootElement(name="episode", namespace="http://searchui.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class EpisodeViewImpl implements EpisodeView {

  @XmlElement(name="title")
  private String title;

  @XmlElement(name="mediaPackageId")
  private String mediaPackageId;

  public EpisodeViewImpl() {}

  static class Adapter extends XmlAdapter<EpisodeViewImpl, EpisodeView> {
    public EpisodeViewImpl marshal(EpisodeView op) throws Exception {return (EpisodeViewImpl)op;}
    public EpisodeView unmarshal(EpisodeViewImpl op) throws Exception {return op;}
  }

  @Override
  public String getTitle() {
    return this.title;
  }

  @Override
  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String getURLEncodedMediaPackageId() {
    return this.mediaPackageId;
  }

  @Override
  public void setURLEncodedMediaPackageId(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
    
  }
}