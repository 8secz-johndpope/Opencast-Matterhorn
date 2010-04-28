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
package org.opencast.engage.videodisplay.control.command
{
    import bridge.ExternalFunction;
    
    import flash.external.ExternalInterface;
    
    import mx.core.Application;
    
    import org.opencast.engage.videodisplay.control.event.DisplayCaptionEvent;
    import org.opencast.engage.videodisplay.control.event.InitMediaPlayerEvent;
    import org.opencast.engage.videodisplay.control.util.TimeCode;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.opencast.engage.videodisplay.state.MediaState;
    import org.opencast.engage.videodisplay.state.PlayerState;
    import org.osmf.display.ScaleMode;
    import org.osmf.elements.AudioElement;
    import org.osmf.elements.ParallelElement;
    import org.osmf.elements.VideoElement;
    import org.osmf.events.AudioEvent;
    import org.osmf.events.BufferEvent;
    import org.osmf.events.LoadEvent;
    import org.osmf.events.MediaErrorEvent;
    import org.osmf.events.TimeEvent;
    import org.osmf.layout.HorizontalAlign;
    import org.osmf.layout.LayoutMetadata;
    import org.osmf.layout.LayoutMode;
    import org.osmf.layout.VerticalAlign;
    import org.osmf.media.MediaElement;
    import org.osmf.media.URLResource;
    import org.swizframework.Swiz;

    public class InitMediaPlayerCommand
    {
        [Autowire]
        public var model:VideodisplayModel;

        private var _time:TimeCode;
        private var currentDurationString:String = "00:00:00";
        private var lastNewPositionString:String = "00:00:00";

        /** Constructor */
        public function InitMediaPlayerCommand()
        {
            Swiz.autowire( this );
        }

        /** execute
         *
         * init the video player.
         *
         * @eventType event:InitPlayerEvent
         * */
        public function execute( event:InitMediaPlayerEvent ):void
        {
			
			 _time = new TimeCode();            


            // Single Video/Audio
            if( event.mediaURLOne != '' && event.mediaURLTwo == '' )
            {
                var pos:int = event.mediaURLOne.lastIndexOf( "." );
                var fileType:String = event.mediaURLOne.substring( pos + 1 );
                
                switch ( fileType )
                {
                    case "flv":
                    case "mp4":
                        var mediaElementVideo:MediaElement =  new VideoElement ( new URLResource( event.mediaURLOne ) );
                        setMediaElement( mediaElementVideo );
                        model.mediaState = MediaState.MEDIA;
                        break;

                    case "mp3":
                        var mediaElementAudio:MediaElement = new AudioElement( new URLResource( event.mediaURLOne ) );
                        setMediaElement( mediaElementAudio );
                        model.mediaState = MediaState.MEDIA;
                        var position:int = event.mediaURLOne.lastIndexOf( '/' );
                        model.audioURL = event.mediaURLOne.substring( position + 1 );
                        Application.application.bx_audio.startVisualization();
                        break;

                    default:
                        errorMessage( "Error", "TRACK COULD NOT BE FOUND" );
                        break;
                }
            }
            else if( event.mediaURLOne != '' && event.mediaURLTwo != '')
            {
                var parallelElement:ParallelElement = new ParallelElement();
                var mediaElementOne:MediaElement = new VideoElement(new URLResource(event.mediaURLOne));
                var mediaElementTwo:MediaElement = new VideoElement(new URLResource(event.mediaURLTwo));
                parallelElement.addChild(mediaElementOne);
                parallelElement.addChild(mediaElementTwo);
                
                model.layoutMetadataParallelElement = new LayoutMetadata();
                model.layoutMetadataParallelElement.percentWidth = 100;
                model.layoutMetadataParallelElement.percentHeight = 100;
                model.layoutMetadataParallelElement.layoutMode = LayoutMode.NONE;
                parallelElement.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, model.layoutMetadataParallelElement);
                
                model.layoutMetadataOne = new LayoutMetadata();
                model.layoutMetadataOne.percentWidth = 50;
                model.layoutMetadataOne.percentHeight = 100;
                model.layoutMetadataOne.scaleMode = ScaleMode.LETTERBOX;
                model.layoutMetadataOne.verticalAlign = VerticalAlign.BOTTOM;
                model.layoutMetadataOne.horizontalAlign = HorizontalAlign.LEFT;
                mediaElementOne.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, model.layoutMetadataOne);
                
                model.layoutMetadataTwo = new LayoutMetadata();
                model.layoutMetadataTwo.percentWidth = 50;
                model.layoutMetadataTwo.percentHeight = 100;
                model.layoutMetadataTwo.scaleMode = ScaleMode.LETTERBOX;
                model.layoutMetadataTwo.verticalAlign = VerticalAlign.BOTTOM;
                model.layoutMetadataTwo.horizontalAlign = HorizontalAlign.RIGHT;
                mediaElementTwo.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, model.layoutMetadataTwo);
                
                setMediaElement( parallelElement );
                
                model.layoutMetadataParallelElement.layoutMode = LayoutMode.HORIZONTAL;
                model.layoutMetadataOne.horizontalAlign = HorizontalAlign.CENTER;
                model.layoutMetadataTwo.horizontalAlign = HorizontalAlign.CENTER;
            }
            else
            {
                errorMessage( "Error", "TRACK COULD NOT BE FOUND" );
            }

            model.currentPlayerState = PlayerState.PLAYING;
            ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PAUSING );
           
            // Set up the MediaPlayer.
            model.mediaPlayer.addEventListener( TimeEvent.DURATION_CHANGE, onDurationChange);
            model.mediaPlayer.addEventListener( AudioEvent.MUTED_CHANGE, muteChange );
			model.mediaPlayer.addEventListener( AudioEvent.VOLUME_CHANGE, volumeChange );
            model.mediaPlayer.addEventListener( TimeEvent.CURRENT_TIME_CHANGE, onCurrentTimeChange );
            model.mediaPlayer.addEventListener( TimeEvent.DURATION_CHANGE, onDurationReached);
            model.mediaPlayer.addEventListener( MediaErrorEvent.MEDIA_ERROR, onMediaError);
            
            model.mediaPlayer.addEventListener( LoadEvent.BYTES_TOTAL_CHANGE, onBytesTotalChange );
			model.mediaPlayer.addEventListener( LoadEvent.BYTES_LOADED_CHANGE, onBytesLoadedChange);
			
			model.mediaPlayer.addEventListener( BufferEvent.BUFFERING_CHANGE, onBufferingChange);
		}
        
        /**
         * setMediaElement
         * 
         * Set the media element.
         *
         * @eventType event:MediaElement
         *
         * */
        private function setMediaElement(value:MediaElement):void
		{
			if (value != null)
			{
				// If there's no explicit layout metadata, center the content. 
				var layoutMetadata:LayoutMetadata = value.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;
				if (layoutMetadata == null)
				{
					layoutMetadata = new LayoutMetadata();
					layoutMetadata.scaleMode = ScaleMode.LETTERBOX;
					layoutMetadata.horizontalAlign = HorizontalAlign.CENTER;
					layoutMetadata.verticalAlign = VerticalAlign.BOTTOM;
					layoutMetadata.percentHeight = 100;
					layoutMetadata.percentWidth = 100;
					value.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, layoutMetadata);
				}
				model.mediaContainer.addMediaElement(value);
			}
			model.mediaPlayer.media = value;
			ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, 100 );
			
		}
        
        /**
         * onDurationChange
         *
         * @eventType event:TimeEvent
         *
         * */
        private function onDurationChange(event:TimeEvent):void
		{
			// Store new duration as current duration in the videodisplay model
            model.currentDuration = event.time;
            model.currentDurationString = _time.getTC( event.time );
            ExternalInterface.call( ExternalFunction.SETDURATION, event.time );
            ExternalInterface.call( ExternalFunction.SETTOTALTIME, model.currentDurationString );
            
        }
        
        /**
         * volumeChange
         *
         * When the volume is change in the video
         * 
         * @eventType event:AudioEvent
         *
         * */
        private function volumeChange( event:AudioEvent ):void
        {
           if( model.mediaPlayer.muted == true )
           {
                model.mediaPlayer.muted = false;
           }
           if( model.mediaPlayer.volume > 0.50 )
           {
            	ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
           }
           
           if( model.mediaPlayer.volume <= 0.50 )
           {
                ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
           }
           
           if( model.mediaPlayer.volume == 0 )
           {
                ExternalInterface.call( ExternalFunction.NONESOUND, '' );
           }
           
           if( model.ccButtonBoolean == false && model.ccBoolean == true )
           {
                model.ccBoolean = false;
                ExternalInterface.call( ExternalFunction.SETCCICONOFF, '' );
           }
        }
        
        /**
         * muteChange
         *
         * When the player is mute or unmute
         * 
         * @eventType event:AudioEvent
         *
         * */
        private function muteChange( event:AudioEvent ):void
        {
           	if( event.muted )
           	{
           		ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, 0 );
           		model.playerVolume = 0;
           		ExternalInterface.call( ExternalFunction.MUTESOUND, '' );
           		if( model.ccButtonBoolean == false )
           		{
           			model.ccBoolean = true;
                    ExternalInterface.call( ExternalFunction.SETCCICONON, '' );
           		}
           		
           	}
           	else
           	{
           		ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, model.mediaPlayer.volume * 100 );	
           		model.playerVolume = model.mediaPlayer.volume;
           		
           		if( model.mediaPlayer.volume > 0.50 )
                {
                    ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
                }
           
                if( model.mediaPlayer.volume <= 0.50 )
                {
                    ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
                }
           
                if( model.mediaPlayer.volume == 0 )
                {
                    ExternalInterface.call( ExternalFunction.NONESOUND, '' );
                }
                
                if( model.ccButtonBoolean == false )
                {
                    model.ccBoolean = false;
                    ExternalInterface.call( ExternalFunction.SETCCICONOFF, '' );
                }
                
           	}
        }
        
        /**
         * onCurrentTimeChange
         *
         * @eventType event:TimeEvent
         *
         * */
        private function onCurrentTimeChange( event:TimeEvent ):void
        {
            var newPositionString:String = _time.getTC( event.time );
          
            if ( newPositionString != lastNewPositionString )
            {
                ExternalInterface.call( ExternalFunction.SETCURRENTTIME, newPositionString );
                lastNewPositionString = newPositionString;
            }

            if ( !model.mediaPlayer.seeking )
            {
                ExternalInterface.call( ExternalFunction.SETPLAYHEAD, event.time );
            }
            
            if ( model.captionsURL != null )
            {
                Swiz.dispatchEvent( new DisplayCaptionEvent( event.time ) );
            }
            
            if( lastNewPositionString == model.currentDurationString )
            {
                model.currentPlayerState = PlayerState.PAUSING;
                ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PLAYING );
            }
            
            model.currentPlayhead = event.time;
        }
        
        /**
         * onDurationReached
         *
         * @eventType event:TimeEvent
         *
         * */
        private function onDurationReached( event:TimeEvent ):void
        {
           // do nothing
        }
        
        /**
         * onMediaError
         *
         * When the media file ist not available.
         * 
         * @eventType event:MediaErrorEvent
         *
         * */
        private function onMediaError( event:MediaErrorEvent ):void
        {
            model.error = new Error();
            model.error.name = event.error.message;
            model.error.message = event.error.detail;
        }

        /**
         * errorMessage
         *
         * Set the error Message and switch the stage.
         * 
         * @param String:name, String:message
         * 
         * */
        private function errorMessage( name:String, message:String ):void
        {
            model.error = new Error();
            model.error.name = name;
            model.error.message = message;
            model.mediaState = MediaState.ERROR;
        }
        
        /**
         * onBytesTotalChange
         *
         * Save the total bytes of the video
         * 
         * @eventType event:LoadEvent
         *
         * */
        private function onBytesTotalChange( event:LoadEvent ):void
		{
			model.bytesTotal = event.bytes;
		}
		
		/**
         * onBytesLoadedChange
         *
         * Set the progress bar.
         * 
         * @eventType event:LoadEvent
         *
         * */
		private function onBytesLoadedChange( event:LoadEvent ):void
		{
			var progress:Number = 0;
            
            try
            {
            	progress = Math.round( event.bytes / model.bytesTotal * 100 );
                ExternalInterface.call( ExternalFunction.SETPROGRESS, progress );
                model.progressBar.setProgress( progress, 100 );
            }
            catch ( e:TypeError )
            {
                // ignore
            }
        }
        
        /**
         * onBufferingChange
         *
		 * @eventType event:BufferEvent
         *
         * */
        private function onBufferingChange( event:BufferEvent ):void
		{
			// do nothing
		}
    }
}