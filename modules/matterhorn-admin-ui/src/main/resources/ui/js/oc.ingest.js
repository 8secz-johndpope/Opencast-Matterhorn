
var ocIngest = ocIngest || {};

ocIngest.debug = true;
ocIngest.mediaPackage = null;
ocIngest.metadata = null;

ocIngest.createMediaPackage = function() {
  log("creating MediaPackage")
  setProgress('100%','creating MediaPackge',' ', ' ');
  $.ajax({
    url        : '../ingest/rest/createMediaPackage',
    type       : 'GET',
    dataType   : 'xml',
    error      : function(XHR,status,e){
      showFailedScreen('Could not create MediaPackage on server.');
    },
    success    : function(data, status) {
      log("MediaPackage created");
      ocIngest.mediaPackage = data;
      var uploadFrame = document.getElementById("filechooser-ajax");
      uploadFrame.contentWindow.document.uploadForm.flavor.value = $('#flavor').val();
      uploadFrame.contentWindow.document.uploadForm.mediaPackage.value = ocUtils.xmlToString(data);
      uploadFrame.contentWindow.document.uploadForm.submit();
    }
  });
}

ocIngest.createDublinCoreCatalog = function(data) {
  var dc = ocUtils.createDoc('dublincore','http://www.opencastproject.org/xsd/1.0/dublincore/');
  dc.documentElement.setAttribute('xmlns:dcterms','http://purl.org/dc/terms/');
  for (key in data) {
    var elm = dc.createElement('dcterms:' + key);
    elm.appendChild(dc.createTextNode(data[key]));    // FIXME get rid of xmlns="" attribute
    dc.documentElement.appendChild(elm);
  }
  return dc;
}

ocIngest.addCatalog = function(mediaPackage, dcCatalog) {
   log("Creating DublinCore catalog");
   setProgress('100%','adding Metadata',' ', ' ');
    $.ajax({
    url        : '../ingest/rest/addDCCatalog',
    type       : 'POST',
    dataType   : 'xml',
    data       : {mediaPackage: mediaPackage, dublinCore: ocUtils.xmlToString(dcCatalog)},
    error      : function(XHR,status,e){
      showFailedScreen('Could not add DublinCore catalog to MediaPackage.');
    },
    success    : function(data, status) {
      log("DublinCore catalog added");
      ocIngest.mediaPackage = data;
     ocIngest.startIngest(data);
    }
  });
}

/*
ocIngest.addTrack = function(mediaPackage, jobId, flavor) {
    log("Adding track to MediaPackage");
    setProgress('100%','adding Track',' ', ' ');
    $.ajax({
    url        : '../fileupload/'+jobId+'/addToMediaPackage',
    type       : 'POST',
    dataType   : 'xml',
    data       : {mediaPackage: ocUtils.xmlToString(mediaPackage), flavor: flavor},
    error      : function(XHR,status,e){
      showFailedScreen();
    },
    success    : function(data, status) {
      log("Track added successfully");
      ocIngest.mediaPackage = data;
      ocIngest.startIngest(data);
    }
  });
}
*/

ocIngest.startIngest = function(mediaPackage) {
    log("Starting Ingest");
    setProgress('100%','starting Ingest',' ', ' ');
    $.ajax({
    url        : '../ingest/rest/startIngest',
    type       : 'POST',
    dataType   : 'text',
    data       : {mediaPackage: ocUtils.xmlToString(mediaPackage)},
    error      : function(XHR,status,e){
      showFailedScreen("Could not start Ingest on MediaPackage");
    },
    success    : function(data, status) {
      hideProgressStage();
      showSuccessScreen();
    }
  });
}