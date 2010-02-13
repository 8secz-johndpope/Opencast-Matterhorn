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

package org.opencastproject.search.api;

import org.opencastproject.media.mediapackage.MediaPackage;

/**
 * Provides search capabilities, possibly to the engage tools, possibly to other services.
 * 
 * TODO: Improve documentation
 */
public interface SearchService {

  /**
   * Adds the media package to the search index.
   * 
   * @param mediaPackage
   *          the media package
   * @throws SearchException
   *           if an error occurs while adding the media package
   */
  void add(MediaPackage mediaPackage) throws SearchException;

  /**
   * Removes the media package identified by <code>mediaPackageId</code> from the search index.
   * 
   * @param mediaPackageId
   *          id of the media package to remove
   * @throws SearchException
   *           if an error occurs while removing the media package
   */
  void delete(String mediaPackageId) throws SearchException;

  /**
   * Processes a regular search for a given user query, creates and returns a regular search result.
   * 
   * @param text
   *          The user query.
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the search result
   * @throws SearchException
   */
  SearchResult getEpisodesAndSeriesByText(String text, int offset, int limit) throws SearchException;

  SearchResult getByQuery(SearchQuery q) throws SearchException;
  
  /**
   * Processes a series search for a given series id, creates and returns a regular search result.
   * 
   * @param seriesId
   *          The series id.
   * @return the search result
   * @throws SearchException
   */
  SearchResult getEpisodesBySeries(String seriesId) throws SearchException;

  /**
   * Processes a series search for a given series id, creates and returns a regular search result.
   * 
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the search result
   * @throws SearchException
   */
  SearchResult getSeriesByDate(int offset, int limit) throws SearchException;

  /**
   * Processes a series search for a given series id, creates and returns the series metadata.
   * 
   * @param id
   *          of the series to search for
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the search result
   * @throws SearchException
   */
  SearchResult getSeriesById(String seriesId) throws SearchException;

  /**
   * Processes a regular search for a given user query, returning all series that match the given query.
   * 
   * @param text
   *          The user query.
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return The search result
   * @throws SearchException
   */
  SearchResult getSeriesByText(String text, int limit, int offset) throws SearchException;

  /**
   * Processes a series search for a given series id, creates and returns a regular search result. The result contains
   * the series metadata as well as all the episodes that are part of that series.
   * 
   * @param seriesId
   *          The series id.
   * @return The series result.
   * @throws SearchException
   */
  SearchResult getEpisodeAndSeriesById(String seriesId) throws SearchException;

  /**
   * Processes a series search for a given series id, creates and returns the series metadata.
   * 
   * @param id
   *          of the series to search for
   * @return the search result
   * @throws SearchException
   */
  SearchResult getEpisodeById(String episodeId) throws SearchException;

  /**
   * Processes a recent items search. Lists all items descending ordered by last modification date.
   * 
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the search result
   * @throws SearchException
   */
  SearchResult getEpisodesByDate(int limit, int offset) throws SearchException;

  /**
   * Processes a regular search for a given user query, creates and returns a regular search result.
   * 
   * @param text
   *          The user query.
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the search result
   * @throws SearchException
   */
  SearchResult getEpisodesByText(String text, int limit, int offset) throws SearchException;

  /**
   * Sends a query to the search service. Depending on the service implementation, the query might be an sql statement a
   * solr query or something similar. In the future, a higher level query language might be a better solution.
   * 
   * @param query
   *          the search query
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the search result
   * @throws SearchException
   */
  SearchResult getByQuery(String query, int limit, int offset) throws SearchException;

}