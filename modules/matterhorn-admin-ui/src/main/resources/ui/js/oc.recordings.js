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

var ocRecordings = ocRecordings || {};

ocRecordings.statsInterval = null;
ocRecordings.updateRequested = false;
ocRecordings.currentState = null;
ocRecordings.currentXSL = null;
ocRecordings.sortBy = "startDate";
ocRecordings.sortOrder = "Descending";
ocRecordings.lastCount = null;
ocRecordings.tableInterval = null;
ocRecordings.tableUpdateRequested = false;
ocRecordings.changedMediaPackage = null;
ocRecordings.configuration = null;

/** Initialize the Recordings page.
 *  Register event handlers.
 *  Set up parser for the date/time field so that tablesorter can sort this col to.
 *  Init periodical update of recording statistics.
 *  Display the recordings specified by the URL param show or upcoming recordings otherwise.
 */
ocRecordings.init = function() {
  //Do internationalization of text
  jQuery.i18n.properties({
    name:'recordings',
    path:'i18n/'
  });
  ocAdmin.internationalize(i18n, 'i18n');
  
  // get config
  $.getJSON("/info/rest/components.json", function(data) {
    ocRecordings.configuration = data;
    $('#engagelink').attr('href', data.engage + '/engage/ui');
  });

  // get 'me'
  $.getJSON("/info/rest/me.json", function(data) {
    ocRecordings.me = data;
    $('#logout').append(" '" + data.username + "'");
  });

  // Event: clicked somewhere
  //  $('body').click( function() {
  //    $('#holdActionPanel-container').fadeOut('fast');
  //  });

  // Buttons style
  $("button").hover(function(){
    $(this).css({
      'background-color': 'white',
      'border-top': '1px solid #ccc',
      'border-left': '1px solid #ccc'
    });
  },
  function(){
    $(this).css({
      'background-color': '',
      'border-top': '',
      'border-left': ''
    })
  });

  /* Event: Scheduler button clicked */
  $('#button_schedule').click( function() {
    window.location.href = '../../admin/scheduler.html';
  });

  /* Event: Upload button clicked */
  $('#button_upload').click( function() {
    window.location.href = '../../admin/upload.html';
  });

  /* Event: Recording State selector clicked */
  $('.state-selector').click( function() {
    var state = $(this).attr('state');
    window.location.href = 'recordings.html?show='+state+'&sortBy='+ocRecordings.sortBy+'&sortOrder='+ocRecordings.sortOrder+'&pageSize='+ocPager.pageSize;
    return false;
  });

  $('#refresh-enabled').click( function() {
    if ($(this).is(':checked')) {
      $('#refresh-interval').removeAttr('disabled');
      $('.refresh-text').removeClass('refresh-text-disabled').addClass('refresh-text-enabled');
      ocRecordings.initTableRefresh($('#refresh-interval').val());
    } else {
      $('#refresh-interval').attr('disabled','true');
      $('.refresh-text').removeClass('refresh-text-enabled').addClass('refresh-text-disabled');
      window.clearInterval(ocRecordings.tableInterval);
      ocRecordings.tableUpdateRequested = false;
    }
  });

  $('#refresh-interval').change(function() {
    ocRecordings.initTableRefresh($(this).val());
  });

  var sort = ocUtils.getURLParam('sortBy');
  if (sort == '') {
    sort='StartDate';
  }
  ocRecordings.sortBy = sort;

  var order = ocUtils.getURLParam('sortOrder');
  if (order == '') {
    order='Descending';
  }
  ocRecordings.sortOrder = order;

  var psize = ocUtils.getURLParam('pageSize');
  if (psize == '') {
    psize = 10;
  }
  ocPager.pageSize = psize;
  ocPager.init();

  var show = ocUtils.getURLParam('show');
  if (show == '') {
    show='upcoming';
  }
  ocRecordings.currentState = show;
  ocRecordings.displayRecordingStats();

  if (ocRecordings.currentState == "upcoming" || ocRecordings.currentState == "finished") {
    $('#info-box').css("display", "block");
    $("#table-info-box-"+ocRecordings.currentState).css("display","block");
  }

  // init update interval for recording stats
  ocRecordings.statsInterval = window.setInterval( 'ocRecordings.displayRecordingStats();', 3000 );
  if (show == 'all' || show == 'capturing' || show == 'processing') {
    $('#refresh-controls-container').css('display','block');
    if ($('#refresh-enabled').is(':visible') && $('#refresh-enabled').is(':checked')) {
      $('.refresh-text').removeClass('refresh-text-disabled').addClass('refresh-text-enabled');
      ocRecordings.initTableRefresh($('#refresh-interval').val());
    }
  } else {
    $('#refresh-controls-container').css('display','none');
  }
}

/** (re-)initialize reloading of recordings table
 *
 */
ocRecordings.initTableRefresh = function(time) {
  ocRecordings.tableInterval = window.setInterval('ocRecordings.displayRecordings("' + ocRecordings.currentState + '",true);', time*1000);
}

/** get and display recording statistics. If the number of recordings in the
 * currently displayed state changes, the table is updated via displayRecordings().
 */
ocRecordings.displayRecordingStats = function() {
  if (!ocRecordings.updateRequested) {
    ocRecordings.updateRequested = true;
    $.ajax({
      url: "rest/countRecordings",
      type: "GET",
      cache: false,
      success: function(data) {
        ocRecordings.updateRequested = false;
        for (key in data) {
          if (ocRecordings.currentState == key) {
            if (ocRecordings.lastCount !== data[key]) {
              ocRecordings.lastCount = data[key];
              ocPager.update(ocPager.pageSize, ocPager.currentPageIdx);
              ocRecordings.displayRecordings(ocRecordings.currentState, true);
            } else {
              ocRecordings.lastCount = data[key];
            }
          }
          var elm = $('#count-' + key);
          if (elm) {
            elm.text('(' + data[key] + ')');
          }
        }
      }
    });
  }
}

/** Request a list of recordings in a certain state and render the response as a table.
 *  While we are waiting for a response, a a little animation is displayed.
 */
ocRecordings.displayRecordings = function(state, reload) {
  if (!ocRecordings.tableUpdateRequested) {
    ocRecordings.tableUpdateRequested = true;
    ocRecordings.currentState = state;
    $('.state-selector').removeClass('state-selector-active');
    $('.selector-'+state).addClass('state-selector-active');
    if (!reload) {
      ocRecordings.injectLoadingAnimation($('#recordings-table-container'));
    }
    if (ocRecordings.currentXML == null) {
      $.ajax({
        type       : 'GET',
        url        : 'xsl/recordings_'+ocRecordings.currentState+'.xsl',
        cache      : false,
        dataType   : 'text',
        error      : function(XHR,status,e){
          //alert('Not able to load XSL for ' + ocRecordings.currentState);
        },
        success    : function(data) {
          ocRecordings.currentXSL = data;
          ocRecordings.loadRecordingsXML();
        }
      });
    } else {
      ocRecordings.loadRecordingsXML();
    }
  }
}

ocRecordings.loadRecordingsXML = function() {
  var page = ocPager.currentPageIdx;
  var psize = ocPager.pageSize;
  var sort = ocRecordings.sortBy;
  var order = ocRecordings.sortOrder;
  var url = "rest/recordings/"+ocRecordings.currentState+"?ps="+psize+"&pn="+page+"&sb="+sort+"&so="+order;
  $.ajax({
    type       : 'GET',
    url        : url,
    cache      : false,
    dataType   : 'text',
    error      : function(XHR,status,e){
      //alert('Not able to load recordings list for state ' + ocRecordings.currentState);
    },
    success    : function(data) {
      ocRecordings.renderTable(data, ocRecordings.currentXSL);
    }
  });
}

ocRecordings.renderTable = function(xml, xsl) {
  $('#recordings-table-container').xslt(xml, xsl, function() {
    ocRecordings.tableUpdateRequested = false;
    // prepare table heads
    $('.recording-Table-head').removeClass('sortable-Ascending').removeClass('sortable-Descending');
    $('#th-'+ocRecordings.sortBy).addClass('sortable-'+ocRecordings.sortOrder);
    $('.recording-Table-head').click(function(){
      ocRecordings.sortBy = $(this).attr('id').substr(3);
      if ($(this).is('.sortable-Descending')) {
        ocRecordings.sortOrder = 'Ascending';
      } else {
        ocRecordings.sortOrder = 'Descending';
      }
      ocPager.currentPageIdx = 0;
      ocRecordings.displayRecordings(ocRecordings.currentState, true);
    });
    // format dates
    if ($('.date-column').length > 0) {
      // if date date/time column is present
      $('.td-TimeDate').each( function() {     // format date/time
        var startTime = $(this).children(".date-start").text();
        var endTime = $(this).children(".date-end").text();
        //alert(startTime + " - " + endTime);
        if (startTime) {
          var sd = new Date();
          sd.setTime(startTime);

          var sday  = sd.getDate();
          var smon  = sd.getMonth()+1;

          if (sday < 10) sday = "0" + sday;
          if (smon < 10) smon = "0" + smon;

          startTime = sd.getFullYear() + '-' + smon + '-' + sday + ' ' + sd.getHours() + ':' + ocUtils.padString(sd.getMinutes(), '0', 2);
        } else {
          startTime = "NA";
        }
        if (endTime) {
          var ed = new Date();
          ed.setTime(endTime);
          endTime = ' - ' + ed.getHours() + ':' + ocUtils.padString(ed.getMinutes(), '0', 2);
        } else {
          endTime = "";
        }
        $(this).append($(document.createElement('span')).text(startTime + endTime));
      });
    }
    //header underline
    $(".header").hover(function(){
      $(this).css('text-decoration', 'underline');
    }, function(){
      $(this).css('text-decoration', 'none')
    });
  });
}

/** inject a 'loading' animation in the specified element
 * @param elm elmement into which the animation should be injected
 */
ocRecordings.injectLoadingAnimation = function(elm) {
  var anim = document.createElement('div');
  $(anim).addClass('loadingAnimation');
  $(elm).empty().append(anim);
}

/** Displays Hold Operation UI
 * @param URL of the hold action UI
 * @param wfId Id of the hold operations workflow
 * @param callerElm HTML element that invoked the UI (so that information from the recordings table row can be gathered
 */
ocRecordings.displayHoldActionPanel = function(URL, wfId, callerElm) {
  $('#holdActionPanel-container iframe').attr('src', URL);
  $('#holdWorkflowId').val(wfId);
  var parentRow = $(callerElm).parent().parent();
  $('#holdStateHeadRow-title').html($($(parentRow).children().get(0)).html());
  $('#holdStateHeadRow-presenter').html($($(parentRow).children().get(1)).html());
  $('#holdStateHeadRow-series').html($($(parentRow).children().get(2)).html());
  $('#holdStateHeadRow-date').html($($(parentRow).children().get(3)).html());
  $('#holdStateHeadRow-status').html($($(parentRow).children().get(4)).html());
  $('#holdActionPanel-container').toggle();
  $('#recordings-table-container').hide();
  $('#category-selector-container').parent().hide();
  $('#oc_recordingmenu').hide();
  $('.paging-nav-container').hide();
  $('#refresh-controls-container').hide();
}

/** Adjusts the height of the panel holding the Hold Operation UI
 *
 */
ocRecordings.adjustHoldActionPanelHeight = function() {
  var height = $("#holdActionPanel-iframe").contents().find("html").height();
  $('#holdActionPanel-iframe').height(height+10);
//alert("Hold action panel height: " + height);
}

/** Calls workflow endpoint to end hold operation and continue the workflow
 *
 */
ocRecordings.continueWorkflow = function(postData) {
  var workflowId = $('#holdWorkflowId').val();
  if(postData===null) {
    postData = {id : workflowId};
  }
  if (ocRecordings.changedMediaPackage != null) {
    postData['mediapackage'] = ocRecordings.changedMediaPackage;
    ocRecordings.changedMediaPackage = null;
  }
  
  // resume expects a "properties" form field with one key=value pair per line
  var props = {};
  props.id = workflowId;
  props.properties = "";
  $.each(postData, function(key, value) {
    if(key != 'id') {
      props.properties = props.properties + key + "=" + value + "\n"; 
    }
  });
  
  $.ajax({
    type       : 'POST',
    url        : '../workflow/rest/resume/',
    data       : props,
    error      : function(XHR,status,e){
      alert('Could not resume Workflow: ' + status);
    },
    success    : function(data) {
      $('#holdActionPanel-container').toggle();
      $('#recordings-table-container').toggle();
      $('#oc_recordingmenu').toggle();
      $('.paging-nav-container').toggle();
      $('#refresh-controls-container').toggle();
      location.reload();
    }
  });
}

/** Show the recording editor
 *
 */
ocRecordings.retryRecording = function(workflowId) {
  location.href = "upload.html?retry=" + workflowId;
}

ocRecordings.removeRecording = function(workflowId) {
  $.ajax({
    url        : '../workflow/rest/stop',
    data       : {
      id: workflowId
    },
    type       : 'POST',
    error      : function(XHR,status,e){
      alert('Could not remove Workflow ' + workflowId);
    },
    success    : function(data) {
      location.reload();
    }
  });
}

ocRecordings.removeScheduledRecording = function(eventId, title) {
  if(confirm('Are you sure you wish to delete ' + title + '?')){
    $.ajax({
      url        : '/scheduler/rest/event/'+eventId,
      type       : 'DELETE',
      error      : function(XHR,status,e){
        alert('Could not remove Scheduler Event ' + workflowId);
      },
      success    : function(data) {
        location.reload();
      }
    });
  }
}