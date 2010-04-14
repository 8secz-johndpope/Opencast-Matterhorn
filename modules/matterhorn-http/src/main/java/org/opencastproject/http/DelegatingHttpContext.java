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
package org.opencastproject.http;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Matterhorn's shared {@link HttpContext}. This delegates resource lookups in the configured /static URL space to the
 * filesystem. All {@link Servlet} and {@link StaticResource} registrations should use the {@link HttpContext} that is
 * registered with the OSGi service registry.
 */
public class DelegatingHttpContext implements HttpContext {
  private static final Logger logger = LoggerFactory.getLogger(DelegatingHttpContext.class);

  protected String filesystemPath = null;
  protected BundleContext bundleContext = null;
  protected HttpService httpService;

  public void setHttpService(HttpService httpService) {
    this.httpService = httpService;
  }

  public void activate(ComponentContext cc) {
    this.bundleContext = cc.getBundleContext();
    filesystemPath = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "static";
    logger.info("Registering resources at {} at URL /static", filesystemPath);
    try {
      httpService.registerResources("/static", "/", this);
    } catch (NamespaceException e) {
      throw new RuntimeException(e);
    }
  }

  public void deactivate() {
    httpService.unregister("/static");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.http.HttpContext#getMimeType(java.lang.String)
   */
  @Override
  public String getMimeType(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.http.HttpContext#getResource(java.lang.String)
   */
  @Override
  public URL getResource(String path) {
    String normalized = path == null ? null : path.trim().replaceAll("/+", "/");
    if (normalized != null && normalized.startsWith("/") && normalized.length() > 1) {
      normalized = normalized.substring(1);
    }

    File f = new File(filesystemPath, normalized);
    if (!f.isFile()) {
      return null;
    }
    try {
      return f.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ServiceReference[] refs;
    try {
      refs = bundleContext.getServiceReferences(Filter.class.getName(), "(org.opencastproject.filter=true)");
    } catch (InvalidSyntaxException e) {
      logger.warn(e.getMessage(), e);
      return false;
    }
    if (refs == null || refs.length == 0) {
      // logger.warn("Requests are not permitted without a registered matterhorn security filter.");
      // return false;
      return true; // TODO: this must be changed to false for the next release
    }
    Filter[] filters = new Filter[refs.length];
    for (int i = 0; i < refs.length; i++)
      filters[i] = (Filter) bundleContext.getService(refs[i]);
    try {
      new Chain(filters).doFilter(request, response);
      return !response.isCommitted();
    } catch (ServletException e) {
      logger.warn(e.getMessage(), e);
      return false;
    }
  }

  /**
   * A {@link FilterChain} composed of {@link Filter}s with the
   */
  class Chain implements FilterChain {
    int current = 0;
    Filter[] filters;

    Chain(Filter[] filters) {
      this.filters = filters;
    }

    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
      if (current < filters.length && !response.isCommitted()) {
        Filter filter = filters[current++];
        logger.debug("doFilter() on " + filter);
        filter.doFilter(request, response, this);
      }
    }
  }
}