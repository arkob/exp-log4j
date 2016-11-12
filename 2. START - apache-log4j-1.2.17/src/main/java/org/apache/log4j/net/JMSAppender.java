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

package org.apache.log4j.net;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.Properties;

/**
 * A simple appender that publishes events to a JMS Topic. The events
 * are serialized and transmitted as JMS message type {@link
 * ObjectMessage}.

 * <p>JMS {@link Topic topics} and {@link TopicConnectionFactory topic
 * connection factories} are administered objects that are retrieved
 * using JNDI messaging which in turn requires the retrieval of a JNDI
 * {@link Context}.

 * <p>There are two common methods for retrieving a JNDI {@link
 * Context}. If a file resource named <em>jndi.properties</em> is
 * available to the JNDI API, it will use the information found
 * therein to retrieve an initial JNDI context. To obtain an initial
 * context, your code will simply call:

   <pre>
   InitialContext jndiContext = new InitialContext();
   </pre>
  
 * <p>Calling the no-argument <code>InitialContext()</code> method
 * will also work from within Enterprise Java Beans (EJBs) because it
 * is part of the EJB contract for application servers to provide each
 * bean an environment naming context (ENC).
    
 * <p>In the second approach, several predetermined properties are set
 * and these properties are passed to the <code>InitialContext</code>
 * constructor to connect to the naming service provider. For example,
 * to connect to JBoss naming service one would write:

<pre>
   Properties env = new Properties( );
   env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
   env.put(Context.PROVIDER_URL, "jnp://hostname:1099");
   env.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
   InitialContext jndiContext = new InitialContext(env);
</pre>

   * where <em>hostname</em> is the host where the JBoss application
   * server is running.
   *
   * <p>To connect to the the naming service of Weblogic application
   * server one would write:

<pre>
   Properties env = new Properties( );
   env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
   env.put(Context.PROVIDER_URL, "t3://localhost:7001");
   InitialContext jndiContext = new InitialContext(env);
</pre>

  * <p>Other JMS providers will obviously require different values.
  * 
  * The initial JNDI context can be obtained by calling the
  * no-argument <code>InitialContext()</code> method in EJBs. Only
  * clients running in a separate JVM need to be concerned about the
  * <em>jndi.properties</em> file and calling {@link
  * InitialContext#InitialContext()} or alternatively correctly
  * setting the different properties before calling {@link
  * InitialContext#InitialContext(java.util.Hashtable)} method.


   @author Ceki G&uuml;lc&uuml; */
public class JMSAppender extends AppenderSkeleton {

  String securityPrincipalName;
  String securityCredentials;
  String initialContextFactoryName;
  String urlPkgPrefixes;
  String providerURL;
  String topicBindingName;
  String tcfBindingName;
  String userName;
  String password;
  boolean locationInfo;

  TopicConnection  topicConnection;
  TopicSession topicSession;
  TopicPublisher  topicPublisher;

  public
  JMSAppender() {
  }

  /**
   *  Options are activated and become effective only after calling
   *  this method.*/
  public void activateOptions() {
    TopicConnectionFactory  topicConnectionFactory;

    try {
      Context jndi;

      LogLog.debug("Getting initial context.");
      if(initialContextFactoryName != null) {
	Properties env = new Properties( );
	env.put(Context.INITIAL_CONTEXT_FACTORY, initialContextFactoryName);
	if(providerURL != null) {
	  env.put(Context.PROVIDER_URL, providerURL);
	} else {
	  LogLog.warn("You have set InitialContextFactoryName option but not the "
		     +"ProviderURL. This is likely to cause problems.");
	}
	if(urlPkgPrefixes != null) {
	  env.put(Context.URL_PKG_PREFIXES, urlPkgPrefixes);
	}
	
	if(securityPrincipalName != null) {
	  env.put(Context.SECURITY_PRINCIPAL, securityPrincipalName);
	  if(securityCredentials != null) {
	    env.put(Context.SECURITY_CREDENTIALS, securityCredentials);
	  } else {
	    LogLog.warn("You have set SecurityPrincipalName option but not the "
			+"SecurityCredentials. This is likely to cause problems.");
	  }
	}	
	jndi = new InitialContext(env);
      } else {
	jndi = new InitialContext();
      }

      LogLog.debug("Looking up ["+tcfBindingName+"]");
      topicConnectionFactory = (TopicConnectionFactory) lookup(jndi, tcfBindingName);
      LogLog.debug("About to create TopicConnection.");
      if(userName != null) {
	topicConnection = topicConnectionFactory.createTopicConnection(userName, 
								       password); 
      } else {
	topicConnection = topicConnectionFactory.createTopicConnection();
      }

      LogLog.debug("Creating TopicSession, non-transactional, "
		   +"in AUTO_ACKNOWLEDGE mode.");
      topicSession = topicConnection.createTopicSession(false,
							Session.AUTO_ACKNOWLEDGE);

      LogLog.debug("Looking up topic name ["+topicBindingName+"].");
      Topic topic = (Topic) lookup(jndi, topicBindingName);

      LogLog.debug("Creating TopicPublisher.");
      topicPublisher = topicSession.createPublisher(topic);
      
      LogLog.debug("Starting TopicConnection.");
      topicConnection.start();

      jndi.close();
    } catch(JMSException e) {
      errorHandler.error("Error while activating options for appender named ["+name+
			 "].", e, ErrorCode.GENERIC_FAILURE);
    } catch(NamingException e) {
      errorHandler.error("Error while activating options for appender named ["+name+
			 "].", e, ErrorCode.GENERIC_FAILURE);
    } catch(RuntimeException e) {
      errorHandler.error("Error while activating options for appender named ["+name+
			 "].", e, ErrorCode.GENERIC_FAILURE);
    }
  }

  protected Object lookup(Context ctx, String name) throws NamingException {
    try {
      return ctx.lookup(name);
    } catch(NameNotFoundException e) {
      LogLog.error("Could not find name ["+name+"].");
      throw e;
    }
  }

  protected boolean checkEntryConditions() {
    String fail = null;

    if(this.topicConnection == null) {
      fail = "No TopicConnection";
    } else if(this.topicSession == null) {
      fail = "No TopicSession";
    } else if(this.topicPublisher == null) {
      fail = "No TopicPublisher";
    }

    if(fail != null) {
      errorHandler.error(fail +" for JMSAppender named ["+name+"].");
      return false;
    } else {
      return true;
    }
  }

  /**
     Close this JMSAppender. Closing releases all resources used by the
     appender. A closed appender cannot be re-opened. */
  public synchronized void close() {
    // The synchronized modifier avoids concurrent append and close operations

    if(this.closed)
      return;

    LogLog.debug("Closing appender ["+name+"].");
    this.closed = true;

    try {
      if(topicSession != null)
	topicSession.close();
      if(topicConnection != null)
	topicConnection.close();
    } catch(JMSException e) {
      LogLog.error("Error while closing JMSAppender ["+name+"].", e);
    } catch(RuntimeException e) {
      LogLog.error("Error while closing JMSAppender ["+name+"].", e);
    }
    // Help garbage collection
    topicPublisher = null;
    topicSession = null;
    topicConnection = null;
  }

  /**
     This method called by {@link AppenderSkeleton#doAppend} method to
     do most of the real appending work.  */
  public void append(LoggingEvent event) {
    if(!checkEntryConditions()) {
      return;
    }

    try {
      ObjectMessage msg = topicSession.createObjectMessage();
      if(locationInfo) {
	event.getLocationInformation();
      }
      msg.setObject(event);
      topicPublisher.publish(msg);
    } catch(JMSException e) {
      errorHandler.error("Could not publish message in JMSAppender ["+name+"].", e,
			 ErrorCode.GENERIC_FAILURE);
    } catch(RuntimeException e) {
      errorHandler.error("Could not publish message in JMSAppender ["+name+"].", e,
			 ErrorCode.GENERIC_FAILURE);
    }
  }

  String getURLPkgPrefixes( ) {
    return urlPkgPrefixes;
  }

  /**
   * Returns the TopicConnection used for this appender.  Only valid after
   * activateOptions() method has been invoked.
   */
  protected TopicConnection  getTopicConnection() {
    return topicConnection;
  }

  /**
   * Returns the TopicSession used for this appender.  Only valid after
   * activateOptions() method has been invoked.
   */
  protected TopicSession  getTopicSession() {
    return topicSession;
  }

  /**
   * Returns the TopicPublisher used for this appender.  Only valid after
   * activateOptions() method has been invoked.
   */
  protected TopicPublisher  getTopicPublisher() {
    return topicPublisher;
  }
  
  /** 
   * The JMSAppender sends serialized events and consequently does not
   * require a layout.
   */
  public boolean requiresLayout() {
    return false;
  }
}
