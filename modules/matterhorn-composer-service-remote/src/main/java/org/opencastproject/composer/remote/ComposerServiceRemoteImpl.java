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
package org.opencastproject.composer.remote;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EmbedderException;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfileBuilder;
import org.opencastproject.composer.api.EncodingProfileImpl;
import org.opencastproject.composer.api.EncodingProfileList;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.remote.api.Job;
import org.opencastproject.remote.api.RemoteBase;
import org.opencastproject.remote.api.Job.Status;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Proxies a set of remote composer services for use as a JVM-local service. Remote services are selected at random.
 */
public class ComposerServiceRemoteImpl extends RemoteBase implements ComposerService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceRemoteImpl.class);

  public ComposerServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(Job.opencastproject.remote.Receipt.Status)
   */
  @Override
  public long countJobs(Status status) {
    return super.countJobs(status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(org.opencastproject.composer.api.Job.Status,
   *      java.lang.String)
   */
  @Override
  public long countJobs(Status status, String host) {
    return super.countJobs(status, host);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.Track,
   *      java.lang.String)
   */
  public Job encode(Track sourceTrack, String profileId) throws EncoderException {
    return encode(sourceTrack, profileId, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.Track,
   *      java.lang.String, boolean)
   */
  public Job encode(Track sourceTrack, String profileId, boolean block) throws EncoderException {
    String url = "/composer/rest/encode";
    HttpPost post = new HttpPost(url);
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("sourceTrack", getXML(sourceTrack)));
      params.add(new BasicNameValuePair("profileId", profileId));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (Exception e) {
      throw new EncoderException("Unable to assemble a remote composer request for track " + sourceTrack, e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        Job r = remoteServiceManager.parseJob(content);
        logger.info("Encoding job {} started on a remote composer", r.getId());
        if (block) {
          r = poll(r.getId());
        }
        return r;
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to encode track " + sourceTrack + " using a remote composer service", e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to encode track " + sourceTrack + " using a remote composer service");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#trim(org.opencastproject.mediapackage.Track,
   *      java.lang.String, long, long)
   */
  @Override
  public Job trim(Track sourceTrack, String profileId, long start, long duration) throws EncoderException,
          MediaPackageException {
    return trim(sourceTrack, profileId, start, duration, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#trim(org.opencastproject.mediapackage.Track,
   *      java.lang.String, long, long, boolean)
   */
  @Override
  public Job trim(Track sourceTrack, String profileId, long start, long duration, boolean block)
          throws EncoderException {
    String url = "/composer/rest/trim";
    HttpPost post = new HttpPost(url);
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("sourceTrack", getXML(sourceTrack)));
      params.add(new BasicNameValuePair("profileId", profileId));
      params.add(new BasicNameValuePair("start", Long.toString(start)));
      params.add(new BasicNameValuePair("duration", Long.toString(duration)));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (Exception e) {
      throw new EncoderException("Unable to assemble a remote composer request for track " + sourceTrack, e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        Job r = remoteServiceManager.parseJob(content);
        logger.info("Trimming job {} started on a remote composer", r.getId());
        if (block) {
          r = poll(r.getId());
        }
        return r;
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to trim track " + sourceTrack + " using a remote composer service", e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to trim track " + sourceTrack + " using a remote composer service");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#mux(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Track, java.lang.String)
   */
  public Job mux(Track sourceVideoTrack, Track sourceAudioTrack, String profileId) throws EncoderException {
    return mux(sourceVideoTrack, sourceAudioTrack, profileId, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#mux(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Track, java.lang.String, boolean)
   */
  public Job mux(Track sourceVideoTrack, Track sourceAudioTrack, String profileId, boolean block)
          throws EncoderException {
    String url = "/composer/rest/mux";
    HttpPost post = new HttpPost(url);
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("videoSourceTrack", getXML(sourceVideoTrack)));
      params.add(new BasicNameValuePair("audioSourceTrack", getXML(sourceAudioTrack)));
      params.add(new BasicNameValuePair("profileId", profileId));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (Exception e) {
      throw new EncoderException("Unable to assemble a remote composer request", e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        Job r = remoteServiceManager.parseJob(content);
        logger.info("Muxing job {} started on a remote composer", r.getId());
        if (block) {
          r = poll(r.getId());
        }
        return r;
      }
    } catch (IOException e) {
      throw new EncoderException(e);
    } finally {
      closeConnection(response);
    }
    throw new EncoderException("Unable to mux tracks " + sourceVideoTrack + " and " + sourceAudioTrack
            + " using a remote composer");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getProfile(java.lang.String)
   */
  @Override
  public EncodingProfile getProfile(String profileId) {
    String url = "/composer/rest/profile/" + profileId + ".xml";
    HttpGet get = new HttpGet(url);
    HttpResponse response = null;
    try {
      response = getResponse(get, HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND);
      if (response != null) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          return EncodingProfileBuilder.getInstance().parseProfile(response.getEntity().getContent());
        } else {
          return null;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("The remote composer service proxy could not get the profile " + profileId);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getReceipt(java.lang.String)
   */
  @Override
  public Job getReceipt(String id) {
    return remoteServiceManager.getJob(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.mediapackage.Track,
   *      java.lang.String, long)
   */
  public Job image(Track sourceTrack, String profileId, long time) throws EncoderException {
    return image(sourceTrack, profileId, time, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.mediapackage.Track,
   *      java.lang.String, long, boolean)
   */
  public Job image(Track sourceTrack, String profileId, long time, boolean block) throws EncoderException {
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    UrlEncodedFormEntity entity = null;
    String url = "/composer/rest/image";
    HttpPost post = new HttpPost(url);
    try {
      params.add(new BasicNameValuePair("sourceTrack", getXML(sourceTrack)));
      params.add(new BasicNameValuePair("profileId", profileId));
      params.add(new BasicNameValuePair("time", Long.toString(time)));
      entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (Exception e) {
      throw new EncoderException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        Job r = remoteServiceManager.parseJob(response.getEntity().getContent());
        logger.info("Image extraction job {} started on a remote composer", r.getId());
        if (block) {
          r = poll(r.getId());
        }
        return r;
      }
    } catch (Exception e) {
      throw new EncoderException(e);
    } finally {
      closeConnection(response);
    }
    throw new EncoderException("Unable to compose an image from track " + sourceTrack
            + " using the remote composer service proxy");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#captions(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Attachment, java.lang.String)
   */
  @Override
  public Job captions(Track mediaTrack, Catalog[] captions) throws EmbedderException {
    return captions(mediaTrack, captions, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#captions(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Attachment, java.lang.String, boolean)
   */
  @Override
  public Job captions(Track mediaTrack, Catalog[] captions, boolean block) throws EmbedderException {
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    UrlEncodedFormEntity entity = null;
    String url = "/composer/rest/captions";
    HttpPost post = new HttpPost(url);
    try {
      params.add(new BasicNameValuePair("mediaTrack", getXML(mediaTrack)));
      params.add(new BasicNameValuePair("captions", getXMLArray(captions, "captions")));
      entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (Exception e) {
      throw new EmbedderException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        Job r = remoteServiceManager.parseJob(response.getEntity().getContent());
        logger.info("Caption embedding job {} started on a remote composer", r.getId());
        if (block) {
          r = poll(r.getId());
        }
        return r;
      }
    } catch (Exception e) {
      throw new EmbedderException(e);
    } finally {
      closeConnection(response);
    }
    throw new EmbedderException("Unable to embed an captions from catalogs " + captions + " to track " + mediaTrack
            + " using the remote composer service proxy");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#listProfiles()
   */
  @Override
  public EncodingProfile[] listProfiles() {
    String url = "/composer/rest/profiles.xml";
    HttpGet get = new HttpGet(url);
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response != null) {
        EncodingProfileList profileList = EncodingProfileBuilder.getInstance().parseProfileList(
                response.getEntity().getContent());
        List<EncodingProfileImpl> list = profileList.getProfiles();
        return list.toArray(new EncodingProfile[list.size()]);
      }
    } catch (Exception e) {
      throw new RuntimeException(
              "Unable to list the encoding profiles registered with the remote composer service proxy", e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to list the encoding profiles registered with the remote composer service proxy");
  }

  /**
   * Serializes a mediapackage element to an xml string
   * 
   * @param element
   *          the mediapackage element
   * @return the xml string
   * @throws Exception
   *           if marshalling goes wrong
   */
  protected String getXML(MediaPackageElement element) throws Exception {
    if (element == null)
      return null;
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.newDocument();
    Node node = element.toManifest(doc, null);
    DOMSource domSource = new DOMSource(node);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    Transformer transformer;
    transformer = TransformerFactory.newInstance().newTransformer();
    transformer.transform(domSource, result);
    return writer.toString();
  }

  /**
   * Serializes media package element array to XML string.
   * 
   * @param elementArray
   *          elements to be serialized
   * @param rootName
   *          name of the root node
   * @return the xml string
   * @throws Exception
   *           if marshalling fails
   */
  protected String getXMLArray(MediaPackageElement[] elementArray, String rootName) throws Exception {

    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.newDocument();
    Element root = doc.createElement(rootName);
    for (MediaPackageElement element : elementArray) {
      Node node = element.toManifest(doc, null);
      root.appendChild(node);
    }
    DOMSource domSource = new DOMSource(root);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.transform(domSource, result);

    return writer.toString();
  }
}
