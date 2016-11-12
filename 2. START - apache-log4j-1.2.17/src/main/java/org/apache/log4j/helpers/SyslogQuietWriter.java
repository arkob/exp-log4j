/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.helpers;



import java.io.Writer;
import java.util.Date;

import org.apache.log4j.spi.ErrorHandler;

/**
   SyslogQuietWriter extends QuietWriter by prepending the syslog
   level code before each printed String.

   @since 0.7.3
*/
public class SyslogQuietWriter extends QuietWriter {

  int syslogFacility;
  int level;

  public
  SyslogQuietWriter(Writer writer, int syslogFacility, ErrorHandler eh) {
    super(writer, eh);
    this.syslogFacility = syslogFacility;
  }

  public
  void setLevel(int level) {
    this.level = level;
  }

  public
  void setSyslogFacility(int syslogFacility) {
    this.syslogFacility = syslogFacility;
  }

  public
  void write(String string) {
    super.write("<"+(syslogFacility | level)+">" + string);
  }

/**
     * Set header or footer of layout.
     * @param msg message body, may not be null.
     */
  private void sendLayoutMessage(final String msg) {
      if (sqw != null) {
          String packet = msg;
          String hdr = getPacketHeader(new Date().getTime());
          if(facilityPrinting || hdr.length() > 0) {
              StringBuffer buf = new StringBuffer(hdr);
              if(facilityPrinting) {
                  buf.append(facilityStr);
              }
              buf.append(msg);
              packet = buf.toString();
          }
          sqw.setLevel(6);
          sqw.write(packet);
      }
  }

/**
     Set the syslog facility. This is the <b>Facility</b> option.

     <p>The <code>facilityName</code> parameter must be one of the
     strings KERN, USER, MAIL, DAEMON, AUTH, SYSLOG, LPR, NEWS, UUCP,
     CRON, AUTHPRIV, FTP, LOCAL0, LOCAL1, LOCAL2, LOCAL3, LOCAL4,
     LOCAL5, LOCAL6, LOCAL7. Case is unimportant.

     @since 0.8.1 */
  public
  void setFacility(String facilityName) {
    if(facilityName == null)
      return;

    syslogFacility = getFacility(facilityName);
    if (syslogFacility == -1) {
      System.err.println("["+facilityName +
                  "] is an unknown syslog facility. Defaulting to [USER].");
      syslogFacility = LOG_USER;
    }

    this.initSyslogFacilityStr();

    // If there is already a sqw, make it use the new facility.
    if(sqw != null) {
      sqw.setSyslogFacility(this.syslogFacility);
    }
  }
}
