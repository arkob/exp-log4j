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

// WARNING This class MUST not have references to the Category or
// WARNING RootCategory classes in its static initiliazation neither
// WARNING directly nor indirectly.

// Contributors:
//                Luke Blanshard <luke@quiq.com>
//                Mario Schomburg - IBM Global Services/Germany
//                Anders Kristensen
//                Igor Poteryaev

package org.apache.log4j;


import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RendererSupport;
import org.apache.log4j.or.RendererMap;
import org.apache.log4j.or.ObjectRenderer;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.ThrowableRendererSupport;
import org.apache.log4j.spi.ThrowableRenderer;

/**
   This class is specialized in retrieving loggers by name and also
   maintaining the logger hierarchy.

   <p><em>The casual user does not have to deal with this class
   directly.</em>

   <p>The structure of the logger hierarchy is maintained by the
   {@link #getLogger} method. The hierarchy is such that children link
   to their parent but parents do not have any pointers to their
   children. Moreover, loggers can be instantiated in any order, in
   particular descendant before ancestor.

   <p>In case a descendant is created before a particular ancestor,
   then it creates a provision node for the ancestor and adds itself
   to the provision node. Other descendants of the same ancestor add
   themselves to the previously created provision node.

   @author Ceki G&uuml;lc&uuml;

*/
public class Hierarchy implements LoggerRepository, RendererSupport, ThrowableRendererSupport {

  private LoggerFactory defaultFactory;
  private Vector listeners;

  Hashtable ht;
  Logger root;
  RendererMap rendererMap;

  int thresholdInt;
  Level threshold;

  boolean emittedNoAppenderWarning = false;
  boolean emittedNoResourceBundleWarning = false;

  private ThrowableRenderer throwableRenderer = null;

  /**
     Create a new logger hierarchy.

     @param root The root of the new hierarchy.

   */
  public
  Hierarchy(Logger root) {
    ht = new Hashtable();
    listeners = new Vector(1);
    this.root = root;
    // Enable all level levels by default.
    setThreshold(Level.ALL);
    this.root.setHierarchy(this);
    rendererMap = new RendererMap();
    defaultFactory = new DefaultCategoryFactory();
  }

  /**
     Add an object renderer for a specific class.
   */
  public
  void addRenderer(Class classToRender, ObjectRenderer or) {
    rendererMap.put(classToRender, or);
  }

  public
  void addHierarchyEventListener(HierarchyEventListener listener) {
    if(listeners.contains(listener)) {
      LogLog.warn("Ignoring attempt to add an existent listener.");
    } else {
      listeners.addElement(listener);
    }
  }

  /**
     This call will clear all logger definitions from the internal
     hashtable. Invoking this method will irrevocably mess up the
     logger hierarchy.

     <p>You should <em>really</em> know what you are doing before
     invoking this method.

     @since 0.9.0 */
  public
  void clear() {
    //System.out.println("\n\nAbout to clear internal hash table.");
    ht.clear();
  }

  /**
     Check if the named logger exists in the hierarchy. If so return
     its reference, otherwise returns <code>null</code>.

     @param name The name of the logger to search for.

  */
  public
  Logger exists(String name) {
    Object o = ht.get(new CategoryKey(name));
    if(o instanceof Logger) {
      return (Logger) o;
    } else {
      return null;
    }
  }

  /**
     The string form of {@link #setThreshold(Level)}.
  */
  public
  void setThreshold(String levelStr) {
    Level l = (Level) Level.toLevel(levelStr, null);
    if(l != null) {
      setThreshold(l);
    } else {
      LogLog.warn("Could not convert ["+levelStr+"] to Level.");
    }
  }


  /**
     Enable logging for logging requests with level <code>l</code> or
     higher. By default all levels are enabled.

     @param l The minimum level for which logging requests are sent to
     their appenders.  */
  public
  void setThreshold(Level l) {
    if(l != null) {
      thresholdInt = l.level;
      threshold = l;
    }
  }

  public
  void fireAddAppenderEvent(Category logger, Appender appender) {
    if(listeners != null) {
      int size = listeners.size();
      HierarchyEventListener listener;
      for(int i = 0; i < size; i++) {
	listener = (HierarchyEventListener) listeners.elementAt(i);
	listener.addAppenderEvent(logger, appender);
      }
    }
  }

  void fireRemoveAppenderEvent(Category logger, Appender appender) {
    if(listeners != null) {
      int size = listeners.size();
      HierarchyEventListener listener;
      for(int i = 0; i < size; i++) {
	listener = (HierarchyEventListener) listeners.elementAt(i);
	listener.removeAppenderEvent(logger, appender);
      }
    }
  }

  /**
     Returns a {@link Level} representation of the <code>enable</code>
     state.

     @since 1.2 */
  public
  Level getThreshold() {
    return threshold;
  }

  /**
     Returns an integer representation of the this repository's
     threshold.

     @since 1.2 */
  //public
  //int getThresholdInt() {
  //  return thresholdInt;
  //}


  /**
     Return a new logger instance named as the first parameter using
     the default factory.

     <p>If a logger of that name already exists, then it will be
     returned.  Otherwise, a new logger will be instantiated and
     then linked with its existing ancestors as well as children.

     @param name The name of the logger to retrieve.

 */
  public
  Logger getLogger(String name) {
    return getLogger(name, defaultFactory);
  }

 /**
     Returns all the currently defined categories in this hierarchy as
     an {@link java.util.Enumeration Enumeration}.

     <p>The root logger is <em>not</em> included in the returned
     {@link Enumeration}.  */
  public
  Enumeration getCurrentLoggers() {
    // The accumlation in v is necessary because not all elements in
    // ht are Logger objects as there might be some ProvisionNodes
    // as well.
    Vector v = new Vector(ht.size());

    Enumeration elems = ht.elements();
    while(elems.hasMoreElements()) {
      Object o = elems.nextElement();
      if(o instanceof Logger) {
	v.addElement(o);
      }
    }
    return v.elements();
  }

  /**
     @deprecated Please use {@link #getCurrentLoggers} instead.
   */
  public
  Enumeration getCurrentCategories() {
    return getCurrentLoggers();
  }


  /**
     Get the renderer map for this hierarchy.
  */
  public
  RendererMap getRendererMap() {
    return rendererMap;
  }


  /**
     Get the root of this hierarchy.

     @since 0.9.0
   */
  public
  Logger getRootLogger() {
    return root;
  }

  /**
     This method will return <code>true</code> if this repository is
     disabled for <code>level</code> object passed as parameter and
     <code>false</code> otherwise. See also the {@link
     #setThreshold(Level) threshold} emthod.  */
  public
  boolean isDisabled(int level) {
    return thresholdInt > level;
  }

  /**
     @deprecated Deprecated with no replacement.
  */
  public
  void overrideAsNeeded(String override) {
    LogLog.warn("The Hiearchy.overrideAsNeeded method has been deprecated.");
  }

  /**
     Does nothing.

     @deprecated Deprecated with no replacement.
   */
  public
  void setDisableOverride(String override) {
    LogLog.warn("The Hiearchy.setDisableOverride method has been deprecated.");
  }



  /**
     Used by subclasses to add a renderer to the hierarchy passed as parameter.
   */
  public
  void setRenderer(Class renderedClass, ObjectRenderer renderer) {
    rendererMap.put(renderedClass, renderer);
  }

    /**
     * {@inheritDoc}
     */
  public void setThrowableRenderer(final ThrowableRenderer renderer) {
      throwableRenderer = renderer;
  }

    /**
     * {@inheritDoc}
     */
  public ThrowableRenderer getThrowableRenderer() {
      return throwableRenderer;
  }


  /**
     Shutting down a hierarchy will <em>safely</em> close and remove
     all appenders in all categories including the root logger.

     <p>Some appenders such as {@link org.apache.log4j.net.SocketAppender}
     and {@link AsyncAppender} need to be closed before the
     application exists. Otherwise, pending logging events might be
     lost.

     <p>The <code>shutdown</code> method is careful to close nested
     appenders before closing regular appenders. This is allows
     configurations where a regular appender is attached to a logger
     and again to a nested appender.


     @since 1.0 */
  public
  void shutdown() {
    Logger root = getRootLogger();

    // begin by closing nested appenders
    root.closeNestedAppenders();

    synchronized(ht) {
      Enumeration cats = this.getCurrentLoggers();
      while(cats.hasMoreElements()) {
	Logger c = (Logger) cats.nextElement();
	c.closeNestedAppenders();
      }

      // then, remove all appenders
      root.removeAllAppenders();
      cats = this.getCurrentLoggers();
      while(cats.hasMoreElements()) {
	Logger c = (Logger) cats.nextElement();
	c.removeAllAppenders();
      }
    }
  }

}


