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
package org.opencastproject.scheduler.impl;

import org.opencastproject.media.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.scheduler.api.SchedulerEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


/**
 * converts the metadata from an scheduler event to Dublin Core metadata   
 *
 */
public class DublinCoreGenerator {
  
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreGenerator.class);
  
  MetadataMapper mapper;

  
  /**
   * Constructor needs the reference to the metadata mapping information. 
   * @param dcMappingFile Properties File with mapping for Dublin Core metadata. This may come from different resources, so that is must be provided.
   * @throws FileNotFoundException
   * @throws IOException
   */
  public DublinCoreGenerator (InputStream dcMappingFile) throws FileNotFoundException, IOException {
    logger.debug("Initialising Dublin Core Generator");
    mapper = new MetadataMapper(dcMappingFile);   
  }
  
  /**
   * Generates a DublinCoreCatalog with the metadata from the provided event
   * @param event The SchedulerEvent from which the metadata should be generated as Dublin Core 
   * @return The DublinCoreCatalog
   */
  public DublinCoreCatalog generate (SchedulerEvent event) {
    logger.debug("creating Dublin Core  information for event {}", event.getID());
    Hashtable<String, String> dcMetadata =  mapper.convert(event.getMetadata());
    
    DublinCoreCatalog dcCatalog = DublinCoreCatalogImpl.newInstance();

    dcCatalog.add(DublinCoreCatalog.PROPERTY_IDENTIFIER, new DublinCoreValue(event.getID()));
    dcCatalog.add(DublinCoreCatalog.PROPERTY_CREATED, new DublinCoreValue(event.getStartdate().toString()));
    Enumeration<String> keys = dcMetadata.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      if (validDcKey(key)) {
        DublinCoreValue value = new DublinCoreValue(dcMetadata.get(key));
        EName property = new EName("http://purl.org/dc/terms/", key);
        dcCatalog.add(property, value);
      } else {
        logger.debug("Key {} is not a valid Dublin Core identifier", key );
      }
    }   
    return dcCatalog;

  }
  
  /**
   * Generates a XML with the Dublin Core metadata from the provided event
   * @param event The SchedulerEvent from which the metadata should be generated as Dublin Core 
   * @return A String with a XML representation of the Dublin Core metadata
   */
  public String generateAsString (SchedulerEvent event) {
    try {
      Document doc = generate(event).toXml();
      Source source = new DOMSource(doc);
      StringWriter stringWriter = new StringWriter();
      Result result = new StreamResult(stringWriter);
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer();
      transformer.transform(source, result);
      return stringWriter.getBuffer().toString().trim(); 
    } catch (ParserConfigurationException e) {
      logger.error("Could not parse DublinCoreCatalog: {}", e.getMessage());
    } catch (IOException e) {
      logger.error("Could not open DublinCoreCatalog to parse it: {}", e.getMessage());
    } catch (TransformerException e) {
      logger.error("Could not transform DublinCoreCatalog: {}", e.getMessage());
    }
    return null;
  }

  /**
   * Checks if the provided key is a valid Dublin Core XML tag
   * @param key The key that should be checked
   * @return true if the key is valid
   */
  private boolean validDcKey (String key) {
    String [] validKeys = new String [] {"bibliographicCitation", "license", "conformsTo", "valid", "isFormatOf", "source", "coverage", "tableOfContents", "dateCopyrighted",
                           "isVersionOf", "isReferencedBy", "accrualPolicy", "date", "publisher", "creator", "accessRights", "subject", "temporal", "accrualMethod", 
                           "hasPart", "medium", "abstract", "title", "audience", "spatial", "dateSubmitted", "relation", "format", "hasFormat", "references", "accrualPeriodicity", 
                           "created", "educationLevel", "rightsHolder", "language", "identifier", "isReplacedBy", "rights", "mediator", "hasVersion", "isPartOf", 
                           "type", "provenance", "dateAccepted", "alternative", "instructionalMethod","available", "isRequiredBy", "requires", "modified",
                           "replaces", "contributor", "description", "any","issued"};
    List<String> vk = Arrays.asList(validKeys);
    return vk.contains(key);
  }  

}