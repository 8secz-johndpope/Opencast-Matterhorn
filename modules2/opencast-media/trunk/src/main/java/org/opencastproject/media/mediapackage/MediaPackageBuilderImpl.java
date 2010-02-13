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

package org.opencastproject.media.mediapackage;

import org.opencastproject.media.mediapackage.identifier.Id;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * This class provides factory methods for the creation of media packages from manifest files, directories or from
 * scratch.
 */
public class MediaPackageBuilderImpl implements MediaPackageBuilder {

  /** The media package serializer */
  protected MediaPackageSerializer serializer = null;

  /**
   * Creates a new media package builder.
   * 
   * @throws IllegalStateException
   *           if the temporary directory cannot be created or is not accessible
   */
  public MediaPackageBuilderImpl() {
  }

  /**
   * Creates a new media package builder that uses the given serializer to resolve urls while reading manifests and
   * adding new elements.
   * 
   * @param serializer
   *          the media package serializer
   * @throws IllegalStateException
   *           if the temporary directory cannot be created or is not accessible
   */
  public MediaPackageBuilderImpl(MediaPackageSerializer serializer) {
    if (serializer == null)
      throw new IllegalArgumentException("Serializer may not be null");
    this.serializer = serializer;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageBuilder#createNew()
   */
  public MediaPackage createNew() throws MediaPackageException {
    return new MediaPackageImpl();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageBuilder#createNew(org.opencastproject.org.opencastproject.media.mediapackage.identifier.Id)
   */
  public MediaPackage createNew(Id identifier) throws MediaPackageException {
    return new MediaPackageImpl(identifier);
  }

  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackageBuilder#loadFromXml(java.io.InputStream)
   */
  public MediaPackage loadFromXml(InputStream is) throws MediaPackageException {
    if (serializer != null) {
      // FIXME This code runs if *any* serializer is present, regardless of the serializer implementation
      try {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document xml = docBuilder.parse(is);

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList)xPath.evaluate("//url", xml, XPathConstants.NODESET);
        for (int i=0; i < nodes.getLength(); i++) {
          Node uri = nodes.item(i).getFirstChild();
          if (uri != null)
            uri.setNodeValue(serializer.resolvePath(uri.getNodeValue()).toString());
        }

        // Serialize the media package
        DOMSource domSource = new DOMSource(xml);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(out);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(domSource, result);
        InputStream in = new ByteArrayInputStream(out.toByteArray());
        return MediaPackageImpl.valueOf(in);
      } catch (Exception e) {
        throw new MediaPackageException("Error deserializing paths in media package", e);
      }
    } else {
      return MediaPackageImpl.valueOf(is);
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageBuilder#loadFromPackage(org.opencastproject.media.mediapackage.MediaPackagePackager,
   *      java.io.InputStream)
   */
  public MediaPackage loadFromPackage(MediaPackagePackager packager, InputStream in) throws IOException,
          MediaPackageException {
    if (packager == null)
      throw new IllegalArgumentException("The packager must not be null");
    if (in == null)
      throw new IllegalArgumentException("The input stream must not be null");
    return packager.unpack(in);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageBuilder#getSerializer()
   */
  public MediaPackageSerializer getSerializer() {
    return serializer;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageBuilder#setSerializer(org.opencastproject.media.mediapackage.MediaPackageSerializer)
   */
  public void setSerializer(MediaPackageSerializer serializer) {
    this.serializer = serializer;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.media.mediapackage.MediaPackageBuilder#loadFromXml(java.lang.String)
   */
  @Override
  public MediaPackage loadFromXml(String xml) throws MediaPackageException {
    InputStream in = null;
    try {
      in = IOUtils.toInputStream(xml, "UTF-8");
      return loadFromXml(in);
    } catch (IOException e) {
      throw new MediaPackageException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}