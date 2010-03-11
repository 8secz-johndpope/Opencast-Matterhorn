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

package org.opencastproject.deliver.store;

import java.util.HashSet;
import java.util.Set;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.RollbackException;

/**
 * Store implementation that saves data as JSON strings in a JPA table.
 *
 */
public class JPAStore<ValueClass> implements Store<ValueClass> {

  /** Serializer to serialize values. */
  private Serializer<ValueClass> serializer;

  /** The entity manager used for persisting Java objects. */
  protected EntityManager em = null;
  
  /** Youtube/iTuensU */
  private String distributionChannel;

  /**
   * Public constructor
   */
  public JPAStore(Serializer serializer, EntityManager em, String distributionChannel) {
    this.serializer = serializer;
    this.em = em;
    this.distributionChannel = distributionChannel;
  }

  /**
   * Inserts or updates an entry.
   *
   * @param key the key of the entry
   * @param value the value of the entry
   */
  public void put(String key, ValueClass value) {
    // System.out.println("putting: " + key + "," + distributionChannel);

    String representation = serializer.toString(value);

    EntityTransaction tx = null;
    try {
      tx = em.getTransaction();
      tx.begin();

      TaskEntity t = em.find(TaskEntity.class, key);
    
      if (t == null) {
        t = new TaskEntity();
        t.setId(key);
      }

      t.setSerializedTask(representation);
      t.setLastModified(new Long(System.currentTimeMillis()));
      t.setDistributionChannel(distributionChannel);
      // Persist the entity
      em.persist(t);

      tx.commit();
    } catch (RollbackException e) {
      if (tx != null) {
        tx.rollback();
      }
    }
  }
  
  /**
   * Retrieves an entry by key.
   *
   * @param key the key of the entry
   * @param the entry
   */
  private TaskEntity getEntryByKey(String id) {
    // for this distribution channel only    
    Query q = em.createQuery("SELECT x FROM TaskEntity x WHERE x.distributionChannel = :channel AND x.id = :id");
    q.setParameter("channel", distributionChannel);
    q.setParameter("id", id);
    List<TaskEntity> results = (List<TaskEntity>) q.getResultList();
    for (TaskEntity task : results) {
      return task;
    }

    return null;
  }

  /**
   * Retrieves the value of an entry.
   *
   * @param key the key of the entry
   * @return value of the entry
   */
  public ValueClass get(String key) {
    TaskEntity t = getEntryByKey(key);
    
    if (t == null) {
      return null;
    }

    String text = t.getSerializedTask();

    return serializer.fromString(text);
  }

  /**
   * Deletes an entry.
   *
   * @param key the key of the entry
   * @return value of the entry
   */
  public ValueClass remove(String key) {
    // System.out.println("removing: " + key + "," + distributionChannel);

    String text = null;
    EntityTransaction tx = null;
    TaskEntity t = getEntryByKey(key);
    
    if (t == null) {
      return null;
    }
      
    try {
      tx = em.getTransaction();
      tx.begin();

      text = t.getSerializedTask();
      em.remove(t);

      tx.commit();
    } catch (RollbackException e) {
      if (tx != null) {
        tx.rollback();
      }
    }

    return serializer.fromString(text);    
  }

  /**
   * Determines if a key exists.
   *
   * @param key the key of the entry
   * @return true/false of existence
   */
  public boolean containsKey(String key) {
    TaskEntity t = getEntryByKey(key);
    
    if (t == null) {
      return false;
    }
    else {
      return true;
    }
  }

  /**
   * Gets the last modified time.
   *
   * @param key the key of the entry
   * @return the last modified time in long
   */
  public long modified(String key) {
    TaskEntity t = getEntryByKey(key);
    
    if (t == null) {
      return -1L;
    }
    else {
      return t.getLastModified();
    }
  }

  /**
   * Retrieves the set of keys.
   *
   * @return the set of keys.
   */  
  public Set<String> keySet() {
    HashSet<String> keys = new HashSet<String>();

    // for this distribution channel only    
    Query q = em.createQuery("SELECT x FROM TaskEntity x WHERE x.distributionChannel = :channel");
    q.setParameter("channel", distributionChannel);
    List<TaskEntity> results = (List<TaskEntity>) q.getResultList();
    for (TaskEntity task : results) {
      String id = task.getId();
      keys.add(id);
    }
    
    return keys;
  }
}