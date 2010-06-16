/*global $, Videodisplay, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
@namespace the global Opencast namespace watch
*/
Opencast.Watch = (function () 
{
    /**
     * @memberOf Opencast.Watch
     * @description Sets up the html page after the player has been initialized. The XSL files are loaded.
     */
    function onPlayerReady() 
    {

      var MULTIPLAYER             = "Multiplayer",
          SINGLEPLAYER            = "Singleplayer",
          SINGLEPLAYERWITHSLIDES  = "SingleplayerWithSlides",
          AUDIOPLAYER             = "Audioplayer",
          ADVANCEDPLAYER          = "advancedPlayer",
          EMBEDPLAYER             = "embedPlayer",
          mediaUrlOne             = "",
          mediaUrlTwo             = "",
          mediaResolutionOne      = "",
          mediaResolutionTwo      = "";

        var mediaPackageId = Opencast.engage.getMediaPackageId();

        var restEndpoint = Opencast.engage.getSearchServiceEpisodeIdURL() + mediaPackageId;

        Opencast.Player.setSessionId(Opencast.engage.getCookie("JSESSIONID"));
        Opencast.Player.setMediaPackageId(mediaPackageId);

        $('#data').xslt(restEndpoint, "xsl/player-hybrid-download.xsl", function () 
        {
          $('#oc-segments').html("");

          // set the media URLs
          var mediaUrlOne = Opencast.engage.getVideoUrl();

          if(mediaUrlOne === null)
            $('#oc-link-advanced-player').css("display", "inline");

          if(mediaUrlOne === null)
            mediaUrlOne = $('#oc-video-presenter-delivery-x-flv-rtmp').html();

          var mediaUrlTwo = $('#oc-video-presentation-delivery-x-flv-rtmp').html();

          if(mediaUrlOne === null)
          {
            mediaUrlOne = $('#oc-video-presenter-delivery-x-flv-http').html();
            mediaResolutionOne = $('#oc-resolution-presenter-delivery-x-flv-http').html();
          }

          if(mediaUrlOne === null)
          {
            mediaUrlOne = $('#oc-video-presenter-source-x-flv-rtmp').html();
            mediaResolutionOne = $('#oc-resolution-presenter-source-x-flv-rtmp').html();
          }

          if(mediaUrlOne === null)
          {
            mediaUrlOne = $('#oc-video-presenter-source-x-flv-http').html();
            mediaResolutionOne = $('#oc-resolution-presenter-source-x-flv-http').html();
          }

          if(mediaUrlTwo === null)
          {
            mediaUrlTwo = $('#oc-video-presentation-delivery-x-flv-http').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-delivery-x-flv-http').html();
          }

          if(mediaUrlTwo === null){
            mediaUrlTwo = $('#oc-video-presentation-source-x-flv-rtmp').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-source-x-flv-rtmp').html();
          }

          if(mediaUrlTwo === null)
          {
            mediaUrlTwo = $('#oc-video-presentation-source-x-flv-http').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-source-x-flv-http').html();
          }

          if(mediaUrlOne === null) 
          {
            mediaUrlOne = mediaUrlTwo;
            mediaUrlTwo = null;
            mediaResolutionOne = mediaResolutionTwo;
            mediaResolutionTwo = null;
          }

          mediaUrlOne = mediaUrlOne === null ? '' : mediaUrlOne;
          mediaUrlTwo = mediaUrlTwo === null ? '' : mediaUrlTwo;

          mediaResolutionOne = mediaResolutionOne === null ? '' : mediaResolutionOne;
          mediaResolutionTwo = mediaResolutionTwo === null ? '' : mediaResolutionTwo;

          var coverUrl = $('#oc-cover-engage').html();
          if(coverUrl === null)
          {
            coverUrl = $('#oc-cover-feed').html();
          }
          coverUrl = coverUrl === null ? '' : coverUrl;

          Opencast.Player.setMediaURL(coverUrl, mediaUrlOne, mediaUrlTwo);

          if (mediaUrlOne !== '' && mediaUrlTwo !== '')
          {
            Opencast.Player.setVideoSizeList(SINGLEPLAYERWITHSLIDES);
            Opencast.Player.videoSizeControlMultiOnlyLeftDisplay();
            Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
          }
          else if (mediaUrlOne !== '' && mediaUrlTwo === '')
          {
            var pos = mediaUrlOne.lastIndexOf(".");
            var fileType = mediaUrlOne.substring(pos + 1);
            
            //
            if (fileType === 'mp3')
            {
              Opencast.Player.setVideoSizeList(AUDIOPLAYER);
            }
            else
            {
              Opencast.Player.setVideoSizeList(SINGLEPLAYER);
              Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
              
            }
          }
          
          //
          Opencast.Initialize.doResize();
          
          // Set the caption
          // oc-captions using caption file generated by Opencaps
          var captionsUrl = $('#oc-captions').html();
          Opencast.Player.setCaptionsURL(captionsUrl);

          // init the volume scrubber
          Opencast.Scrubber.init();

          
          // bind handler 
          $('#scrubber').bind('keydown', 'left', function(evt) 
          {
            Opencast.Player.doRewind();
          });
          
          $('#scrubber').bind('keyup', 'left', function(evt) 
          {
            Opencast.Player.stopRewind();
          });
          
          $('#scrubber').bind('keydown', 'right', function(evt)
          {
            Opencast.Player.doFastForward();
          });
          
          $('#scrubber').bind('keyup', 'right', function(evt)
          {
            Opencast.Player.stopFastForward();
          });
          
          
          getClientShortcuts();
          
       });
    }
  
    /**
     * @memberOf Opencast.Watch
     * @description Toggles the class segment-holder-over
     * @param String segmentId the id of the segment
     */
    function hoverSegment(segmentId)
    {
      $("#" + segmentId).toggleClass("segment-holder");
      $("#" + segmentId).toggleClass("segment-holder-over");
    }
  
    /**
     * @memberOf Opencast.Watch
     * @description Seeks the video to the passed position. Is called when the user clicks on a segment
     * @param int seconds the position in the video
     */
    function seekSegment(seconds)
    {
      var eventSeek = Videodisplay.seek(seconds);
    }
    /**
     * @memberOf Opencast.Watch
     * @description Gets the OS-specific shortcuts of the client
     */
    function getClientShortcuts()
    {	
      $('#oc_client_shortcuts').append("Control + Alt + I = Toggle the keyboard shortcuts information between show or hide.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + P = Toggle the video between pause or play.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + S = Stop the video.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + M = Toggle between mute or unmute the video.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + U = Volume up<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + D = Volume down<br/>");
      $('#oc_client_shortcuts').append("Control + Alt 0 - 9 = Seek the time slider<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + C = Toggle between captions on or off.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + F = Forward the video.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + R = Rewind the video.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + T = the current time for the screen reader<br/>");

      switch($.client.os){
        case "Windows":
          $('#oc_client_shortcuts').append("Windows Control + = to zoom in the player<br/>"); 
          $('#oc_client_shortcuts').append("Windows Control - = to minimize in the player<br/>");
         break; 
        case "Mac":
          $('#oc_client_shortcuts').append("cmd + = to zoom in the player<br/>"); 
      	$('#oc_client_shortcuts').append("cmd - = to minimize the player<br/>");
      	break; 
         case "Linux":
      	break;
      } 	
    }

    return {
      onPlayerReady : onPlayerReady,
      hoverSegment : hoverSegment,
      seekSegment : seekSegment,
      getClientShortcuts : getClientShortcuts
    };
}());
