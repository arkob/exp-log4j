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
package com.psibt.framework.net;

import java.io.*;
import java.net.*;

/**
 * This class implements a RequestHandler for the root path "/" in the PluggableHTTPServer.
 * A simple HTML message will be replied to the client.
 *
 * @author <a HREF="mailto:V.Mentzner@psi-bt.de">Volker Mentzner</a>
 */
public class RootRequestHandler implements HTTPRequestHandler {

  private String title;
  private String description;
  private String handledPath;
  private String ReplyType = "Content-type: text/html\r\n\r\n";
  private String ReplyHTML = "<HTML><HEAD><TITLE>Root</TITLE></HEAD>\r\n"
                           + "<BODY><H1>Root</H1>\r\n"
                           + "</BODY></HTML>\r\n";

 /**
   * Creates a new RootRequestHandler object
   */
  public RootRequestHandler() {
    com.psibt.framework.net.Log4jRequestHandler.setTitle(this, "root page");
    com.psibt.framework.net.Log4jRequestHandler.setDescription(this, "root page");
    com.psibt.framework.net.Log4jRequestHandler.setHandledPath(this, "/");
  }

    /**
   * Handles the given request and writes the reply to the given out-stream.
   *
   * @param request - client browser request
   * @param out - Out stream for sending data to client browser
   * @return if the request was handled by this handler : true, else : false
   */
  public boolean handleRequest(String request, Writer out) {
    String path = "";
    String query = null;
    try {
      URL url = new URL("http://localhost"+request);
      path = url.getPath();
      query = url.getPath();
      if (path.equals(handledPath) == false) {
        return false;
      }

      out.write("HTTP/1.0 200 OK\r\n");
      if (ReplyType != null)
        out.write(ReplyType);
      if (ReplyHTML != null)
        out.write(ReplyHTML);
      out.flush();
      return true;
    } catch (Exception ex) {
      return false;
    }
  }
}