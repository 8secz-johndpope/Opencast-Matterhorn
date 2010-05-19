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

package org.opencastproject.series.api;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity(name="SeriesMetadata")
@Table(name="SeriesMetadata")
public class SeriesMetadata {
  @Id
  @GeneratedValue
  protected long id;
  
  @Column(name="key")
  protected String key;
  @Column(name="value")
  protected String value;
  
  public SeriesMetadata () {
    super();
  }
  
  public SeriesMetadata (String key, String value) {
    this.key = key;
    this.value = value;
  }
  
  public long getId() {
    return id;
  }
  public void setId(long id) {
    this.id = id;
  }
  public String getKey() {
    return key;
  }
  public void setKey(String key) {
    this.key = key;
  }
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }  
  
  public String toString () {
    return "("+id+") "+key+":"+value;
  }
  
  public boolean equals (Object o) {
    if (o == null) return false;
    if (! (o instanceof SeriesMetadata)) return false;
    SeriesMetadata m = (SeriesMetadata) o;
    if (m.getKey().equals(getKey()) && m.getValue().equals(getValue())) return true;
    return false;
  }
  
  public int hashCode () {
    return getKey().hashCode();
  } 
}
