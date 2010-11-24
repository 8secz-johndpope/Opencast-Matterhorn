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
package org.opencastproject.capture.pipeline.bins.producers;

import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import java.util.Properties;

public class ProducerFactory {
  /** The actual singleton factory **/
  private static ProducerFactory factory;
  
  /** Singleton factory pattern ensuring only one instance of the factory is created even with multiple threads. **/
  public static synchronized ProducerFactory getInstance(){
    if(factory == null){
      factory = new ProducerFactory();
    }
    return factory;
  }
  
  /** Constructor made private so that the number of Factories can be kept to one. **/
  private ProducerFactory(){
    
  }

  /**
   * Returns the Producer corresponding to the ProducerType
   * 
   * @param captureDevice
   *          The properties of the capture device such as container, codec, bitrate, enabled confidence monitoring etc.
   *          used to initialize the Producer.
   * @param properties
   *          Some individual properties of the confidence monitoring.
   * @throws UnableToLinkGStreamerElementsException
   *           If the ProducerBin has problems linking its Elements together this Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If ghostpads cannot be created to link Producers to Consumers this Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the creation of the Consumer somehow calls the setElementProperties before it calls createElements the
   *           Elements will be null and this Exception will be thrown.
   * @throws CaptureDeviceNullPointerException
   *           If a null CaptureDevice is passed to this ProducerFactory then this Exception will be thrown since there
   *           are certain settings that are needed to create a Consumer in this parameter.
   * @throws UnableToCreateElementException
   *           If an Element for the ProducerBin cannot be created because this machine does not have the necessary
   *           GStreamer module installed or it is an OS that this Producer doesn't support this Exception is thrown.
   * @throws NoProducerFoundException
   *           If no Producer can be found for this particular ProducerType then this Exception will be thrown.
   **/
  public ProducerBin getProducer(CaptureDevice captureDevice, Properties properties, CaptureAgent captureAgent)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException, NoProducerFoundException {
    if (captureDevice.getName() == ProducerType.EPIPHAN_VGA2USB)
      return new EpiphanVGA2USBV4LProducer(captureDevice, properties, captureAgent);
    else if (captureDevice.getName() == ProducerType.HAUPPAUGE_WINTV)
      return new HauppaugePVR350VideoProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.FILE_DEVICE)
      return new FileProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.BLUECHERRY_PROVIDEO)
      return new BlueCherryBT878Producer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.ALSASRC)
      return new AlsaProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.PULSESRC)
      return new PulseAudioProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.AUDIOTESTSRC)
      return new AudioTestSrcProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.DV_1394)
      return new DV1394Producer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.VIDEOTESTSRC)
      return new VideoTestSrcProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.V4LSRC)
      return new V4LProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.V4L2SRC)
      return new V4L2Producer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.CUSTOM_VIDEO_SRC)
      return new CustomVideoProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.CUSTOM_AUDIO_SRC)
      return new CustomAudioProducer(captureDevice, properties);
    else{
      throw new NoProducerFoundException("No valid Producer found for device " + captureDevice.getName());
    }
  }
}