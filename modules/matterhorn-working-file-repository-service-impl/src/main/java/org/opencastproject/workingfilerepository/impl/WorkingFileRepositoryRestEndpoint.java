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
package org.opencastproject.workingfilerepository.impl;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.doc.rest.RestParameter.Type.FILE;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "filerepo", title = "Working File Repository", abstractText = "Stores and retrieves files for use during media processing.", notes = WorkingFileRepositoryRestEndpoint.NOTES)
public class WorkingFileRepositoryRestEndpoint extends WorkingFileRepositoryImpl {

  public static final String NOTES = "All paths above are relative to the REST endpoint base (something like "
          + "http://your.server/files).  If the service is down or not working it will return a status 503, this means the "
          + "underlying service is not working and is either restarting or has failed.  A status code 500 means a general "
          + "failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You "
          + "should file an error report with your server logs from the time when the error occurred: "
          + "<a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>";

  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryRestEndpoint.class);

  private final MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap(getClass().getClassLoader()
          .getResourceAsStream("mimetypes"));

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) throws IOException {
    super.activate(cc);
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
  @RestQuery(name = "put", description = "Store a file in working repository under ./mediaPackageID/mediaPackageElementID", returnDescription = "The URL to access the stored file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "OK, file stored") }, restParameters = { @RestParameter(name = "file", description = "the filename", isRequired = true, type = FILE) })
  public Response restPut(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID, @Context HttpServletRequest request)
          throws Exception {
    if (ServletFileUpload.isMultipartContent(request)) {
      for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        FileItemStream item = iter.next();
        if (item.isFormField()) {
          continue;

        }
        URI url = this.put(mediaPackageID, mediaPackageElementID, item.getName(), item.openStream());
        return Response.ok(url.toString()).build();
      }
    }
    return Response.serverError().status(400).build();
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}/{filename}")
  @RestQuery(name = "putWithFilename", description = "Store a file in working repository under ./mediaPackageID/mediaPackageElementID/filename", returnDescription = "The URL to access the stored file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING),
          @RestParameter(name = "filename", description = "the filename", isRequired = true, type = FILE) }, reponses = { @RestResponse(responseCode = SC_OK, description = "OK, file stored") })
  public Response restPutURLEncoded(@Context HttpServletRequest request,
          @PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID, @PathParam("filename") String filename,
          @FormParam("content") String content) throws Exception {
    String encoding = request.getCharacterEncoding();
    if (encoding == null)
      encoding = "utf-8";

    URI url = this.put(mediaPackageID, mediaPackageElementID, filename, IOUtils.toInputStream(content, encoding));
    return Response.ok(url.toString()).build();
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}")
  @RestQuery(name = "putInCollection", description = "Store a file in working repository under ./collectionId/filename", returnDescription = "The URL to access the stored file", pathParameters = { @RestParameter(name = "collectionId", description = "the colection identifier", isRequired = true, type = STRING) }, restParameters = { @RestParameter(name = "file", description = "the filename", isRequired = true, type = FILE) }, reponses = { @RestResponse(responseCode = SC_OK, description = "OK, file stored") })
  public Response restPutInCollection(@PathParam("collectionId") String collectionId,
          @Context HttpServletRequest request) throws Exception {
    if (ServletFileUpload.isMultipartContent(request)) {
      for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        FileItemStream item = iter.next();
        if (item.isFormField()) {
          continue;

        }
        URI url = this.putInCollection(collectionId, item.getName(), item.openStream());
        return Response.ok(url.toString()).build();
      }
    }
    return Response.serverError().status(400).build();
  }

  @DELETE
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
  @RestQuery(name = "delete", description = "Remove the file from the working repository under /mediaPackageID/mediaPackageElementID", returnDescription = "No content", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "File deleted"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "File did not exist") })
  public Response restDelete(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID) {
    try {
      if (delete(mediaPackageID, mediaPackageElementID))
        return Response.ok().build();
      else
        return Response.status(HttpStatus.SC_NOT_FOUND).build();
    } catch (Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @DELETE
  @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}/{fileName}")
  @RestQuery(name = "deleteFromCollection", description = "Remove the file from the working repository under /collectionId/filename", returnDescription = "No content", pathParameters = {
          @RestParameter(name = "collectionId", description = "the collection identifier", isRequired = true, type = STRING),
          @RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING) }, reponses = { @RestResponse(responseCode = SC_NO_CONTENT, description = "File deleted") })
  public Response restDeleteFromCollection(@PathParam("collectionId") String collectionId,
          @PathParam("fileName") String fileName) {
    try {
      this.deleteFromCollection(collectionId, fileName);
      return Response.noContent().build();
    } catch (Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
  @RestQuery(name = "get", description = "Gets the file from the working repository under /mediaPackageID/mediaPackageElementID", returnDescription = "The file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "File returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response restGet(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID,
          @HeaderParam("If-None-Match") String ifNoneMatch) throws NotFoundException, IOException {

    String md5 = null;
    InputStream in = null;

    // Check the If-None-Match header first
    try {
      md5 = getMediaPackageElementDigest(mediaPackageID, mediaPackageElementID);
      if (StringUtils.isNotBlank(ifNoneMatch) && md5.equals(ifNoneMatch)) {
        return Response.notModified(md5).build();
      }
    } catch (IOException e) {
      logger.warn("Error reading digest of {}/{}", new Object[] { mediaPackageElementID, mediaPackageElementID });
    }

    String contentType = null;
    try {
      File theFile = getFile(mediaPackageID, mediaPackageElementID);
      in = new FileInputStream(theFile);
      contentType = extractContentType(in);
      return Response.ok(get(mediaPackageID, mediaPackageElementID)).header("Content-Type", contentType)
              .header("Content-Length", theFile.length()).build();
    } catch (IllegalStateException e) {
      IOUtils.closeQuietly(in);
      throw new NotFoundException();
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Determines the content type of an input stream. This method reads part of the stream, so it is typically best to
   * close the stream immediately after calling this method.
   * 
   * @param in
   *          the input stream
   * @return the content type
   */
  protected String extractContentType(InputStream in) {
    try {
      // Find the content type, based on the stream content
      BodyContentHandler contenthandler = new BodyContentHandler();
      Metadata metadata = new Metadata();
      Parser parser = new AutoDetectParser();
      ParseContext context = new ParseContext();
      parser.parse(in, contenthandler, metadata, context);
      return metadata.get(HttpHeaders.CONTENT_TYPE);
    } catch (Exception e) {
      logger.warn("Unable to extract mimetype from input stream, ", e);
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }

  @GET
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}/{fileName}")
  @RestQuery(name = "getWithFilename", description = "Gets the file from the working repository under /mediaPackageID/mediaPackageElementID/filename", returnDescription = "The file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING),
          @RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "File returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response restGet(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID, @PathParam("fileName") String fileName,
          @HeaderParam("If-None-Match") String ifNoneMatch) throws NotFoundException {

    String md5 = null;
    File in = null;

    // Check the If-None-Match header first
    try {
      md5 = getMediaPackageElementDigest(mediaPackageID, mediaPackageElementID);
      if (StringUtils.isNotBlank(ifNoneMatch) && md5.equals(ifNoneMatch)) {
        return Response.notModified(md5).build();
      }
    } catch (IOException e) {
      logger.warn("Error reading digest of {}/{}/{}", new Object[] { mediaPackageElementID, mediaPackageElementID,
              fileName });
    }

    // No If-Non-Match header provided, or the file changed in the meantime
    try {
      in = getFile(mediaPackageID, mediaPackageElementID);
      String contentType = mimeMap.getContentType(fileName);
      long contentLength = in.length();
      return Response.ok(in).header("Content-disposition", "attachment; filename=" + fileName)
              .header("Content-Type", contentType).header("Content-length", contentLength).tag(md5).build();
    } catch (IllegalStateException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } 
  }

  @GET
  @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}/{fileName}")
  @RestQuery(name = "getFromCollection", description = "Gets the file from the working repository under /collectionId/filename", returnDescription = "The file", pathParameters = {
          @RestParameter(name = "collectionId", description = "the collection identifier", isRequired = true, type = STRING),
          @RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "File returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response restGetFromCollection(@PathParam("collectionId") String collectionId,
          @PathParam("fileName") String fileName) throws NotFoundException, IOException {
    
    File in = getFileFromCollection(collectionId, fileName);
    String contentType = mimeMap.getContentType(fileName);
    long contentLength = in.length();
        
    return Response.ok(in).header("Content-disposition", "attachment; filename=" + fileName)
            .header("Content-Type", contentType).header("Content-length", contentLength).build();
  }

  @GET
  @Path("/collectionuri/{collectionID}/{fileName}")
  @RestQuery(name = "getUriFromCollection", description = "Gets the URL for a file to be stored in the working repository under /collectionId/filename", returnDescription = "The url to this file", pathParameters = {
          @RestParameter(name = "collectionID", description = "the collection identifier", isRequired = true, type = STRING),
          @RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "URL returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response restGetCollectionUri(@PathParam("collectionID") String collectionId,
          @PathParam("fileName") String fileName) {
    URI uri = this.getCollectionURI(collectionId, fileName);
    return Response.ok(uri.toString()).build();
  }

  @GET
  @Path("/uri/{mediaPackageID}/{mediaPackageElementID}")
  @RestQuery(name = "getUri", description = "Gets the URL for a file to be stored in the working repository under /mediaPackageID", returnDescription = "The url to this file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediaPackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediaPackage element identifier", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "URL returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response restGetUri(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID) {
    URI uri = this.getURI(mediaPackageID, mediaPackageElementID);
    return Response.ok(uri.toString()).build();
  }

  @GET
  @Path("/uri/{mediaPackageID}/{mediaPackageElementID}/{fileName}")
  @RestQuery(name = "getUriWithFilename", description = "Gets the URL for a file to be stored in the working repository under /mediaPackageID", returnDescription = "The url to this file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediaPackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediaPackage element identifier", isRequired = true, type = STRING),
          @RestParameter(name = "fileName", description = "the filename", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "URL returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response restGetUri(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID, @PathParam("fileName") String fileName) {
    URI uri = this.getURI(mediaPackageID, mediaPackageElementID, fileName);
    return Response.ok(uri.toString()).build();
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/list/{collectionId}.json")
  @RestQuery(name = "filesInCollection", description = "Lists files in a collection", returnDescription = "Links to the URLs in a collection", pathParameters = { @RestParameter(name = "collectionId", description = "the collection identifier", isRequired = true, type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "URLs returned") })
  public Response restGetCollectionContents(@PathParam("collectionId") String collectionId) {
    try {
      URI[] uris = super.getCollectionContents(collectionId);
      JSONArray jsonArray = new JSONArray();
      for (URI uri : uris) {
        jsonArray.add(uri.toString());
      }
      return Response.ok(jsonArray.toJSONString()).build();
    } catch (Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @POST
  @Path("/copy/{fromCollection}/{fromFileName}/{toMediaPackage}/{toMediaPackageElement}/{toFileName}")
  @RestQuery(name = "copy", description = "Copies a file from a collection to a mediapackage", returnDescription = "A URL to the copied file", pathParameters = {
          @RestParameter(name = "fromCollection", description = "the collection identifier hosting the source", isRequired = true, type = STRING),
          @RestParameter(name = "fromFileName", description = "the source file name", isRequired = true, type = STRING),
          @RestParameter(name = "toMediaPackage", description = "the destination mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "toMediaPackageElement", description = "the destination mediapackage element identifier", isRequired = true, type = STRING),
          @RestParameter(name = "toFileName", description = "the destination file name", isRequired = true, type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "URL returned") })
  public Response restCopyTo(@PathParam("fromCollection") String fromCollection,
          @PathParam("fromFileName") String fromFileName, @PathParam("toMediaPackage") String toMediaPackage,
          @PathParam("toMediaPackageElement") String toMediaPackageElement, @PathParam("toFileName") String toFileName) {
    try {
      URI uri = super.copyTo(fromCollection, fromFileName, toMediaPackage, toMediaPackageElement, toFileName);
      return Response.ok().entity(uri.toString()).build();
    } catch (Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @POST
  @Path("/move/{fromCollection}/{fromFileName}/{toMediaPackage}/{toMediaPackageElement}/{toFileName}")
  @RestQuery(name = "move", description = "Moves a file from a collection to a mediapackage", returnDescription = "A URL to the moved file", pathParameters = {
          @RestParameter(name = "fromCollection", description = "the collection identifier hosting the source", isRequired = true, type = STRING),
          @RestParameter(name = "fromFileName", description = "the source file name", isRequired = true, type = STRING),
          @RestParameter(name = "toMediaPackage", description = "the destination mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "toMediaPackageElement", description = "the destination mediapackage element identifier", isRequired = true, type = STRING),
          @RestParameter(name = "toFileName", description = "the destination file name", isRequired = true, type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "URL returned") })
  public Response restMoveTo(@PathParam("fromCollection") String fromCollection,
          @PathParam("fromFileName") String fromFileName, @PathParam("toMediaPackage") String toMediaPackage,
          @PathParam("toMediaPackageElement") String toMediaPackageElement, @PathParam("toFileName") String toFileName) {
    try {
      URI uri = super.moveTo(fromCollection, fromFileName, toMediaPackage, toMediaPackageElement, toFileName);
      return Response.ok().entity(uri.toString()).build();
    } catch (Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("storage")
  @RestQuery(name = "storage", description = "Returns a report on the disk usage and availability", returnDescription = "Plain text containing the report", reponses = { @RestResponse(responseCode = SC_OK, description = "Report returned") })
  public Response restGetTotalStorage() {
    long total = this.getTotalSpace();
    long usable = this.getUsableSpace();
    String summary = this.getDiskSpace();
    JSONObject json = new JSONObject();
    json.put("size", total);
    json.put("usable", usable);
    json.put("summary", summary);
    return Response.ok(json.toJSONString()).build();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/baseUri")
  @RestQuery(name = "baseuri", description = "Returns a base URI for this repository", returnDescription = "Plain text containing the base URI", reponses = { @RestResponse(responseCode = SC_OK, description = "Base URI returned") })
  public Response restGetBaseUri() {
    return Response.ok(super.getBaseUri().toString()).build();
  }
}
