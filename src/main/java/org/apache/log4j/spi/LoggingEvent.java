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

package org.apache.log4j.spi;

import org.apache.log4j.*;

import java.util.*;

// Contributors:   Nelson Minar <nelson@monkey.org>
//                 Wolf Siberski
//                 Anders Kristensen <akristensen@dynamicsoft.com>

/**
   The internal representation of logging events. When an affirmative
   decision is made to log then a <code>LoggingEvent</code> instance
   is created. This instance is passed around to the different log4j
   components.

   <p>This class is of concern to those wishing to extend log4j.

   @author Ceki G&uuml;lc&uuml;
   @author James P. Cakalic

   @since 0.8.2 */
public class LoggingEvent extends LogEventBase {

  private static long startTime = System.currentTimeMillis();

    /**
   * <p>The category (logger) name.
   *   
   * @deprecated This field will be marked as private in future
   * releases. Please do not access it directly. Use the {@link
   * #getLoggerName} method instead.

   * */
  final public String categoryName;

    // Serialization
  static final long serialVersionUID = -868428216207166145L;

  static final Integer[] PARAM_ARRAY = new Integer[1];

    /**
     Instantiate a LoggingEvent from the supplied parameters.

     <p>Except {@link #timeStamp} all the other fields of
     <code>LoggingEvent</code> are filled when actually needed.
     <p>
     @param logger The logger generating this event.
     @param level The level of this event.
     @param message  The message of this event.
     @param throwable The throwable of this event.  */
  public LoggingEvent(String fqnOfCategoryClass, Category logger,
		      Priority level, Object message, Throwable throwable) {
    this.fqnOfCategoryClass = fqnOfCategoryClass;
    this.logger = logger;
    this.categoryName = logger.getName();
    this.level = level;
    this.message = message;
    if(throwable != null) {
      this.throwableInfo = new ThrowableInformation(throwable, logger);
    }
    timeStamp = System.currentTimeMillis();
  }

  /**
     Instantiate a LoggingEvent from the supplied parameters.

     <p>Except {@link #timeStamp} all the other fields of
     <code>LoggingEvent</code> are filled when actually needed.
     <p>
     @param logger The logger generating this event.
     @param timeStamp the timestamp of this logging event
     @param level The level of this event.
     @param message  The message of this event.
     @param throwable The throwable of this event.  */
  public LoggingEvent(String fqnOfCategoryClass, Category logger,
		      long timeStamp, Priority level, Object message,
		      Throwable throwable) {
    this.fqnOfCategoryClass = fqnOfCategoryClass;
    this.logger = logger;
    this.categoryName = logger.getName();
    this.level = level;
    this.message = message;
    if(throwable != null) {
      this.throwableInfo = new ThrowableInformation(throwable, logger);
    }

    this.timeStamp = timeStamp;
  }

    /**
       Create new instance.
       @since 1.2.15
       @param fqnOfCategoryClass Fully qualified class name
                 of Logger implementation.
       @param logger The logger generating this event.
       @param timeStamp the timestamp of this logging event
       @param level The level of this event.
       @param message  The message of this event.
       @param threadName thread name
       @param throwable The throwable of this event.
       @param ndc Nested diagnostic context
       @param info Location info
       @param properties MDC properties
     */
    public LoggingEvent(final String fqnOfCategoryClass,
                        final Category logger,
                        final long timeStamp,
                        final Level level,
                        final Object message,
                        final String threadName,
                        final ThrowableInformation throwable,
                        final String ndc,
                        final LocationInfo info,
                        final java.util.Map properties) {
      super();
      this.fqnOfCategoryClass = fqnOfCategoryClass;
      this.logger = logger;
      if (logger != null) {
          categoryName = logger.getName();
      } else {
          categoryName = null;
      }
      this.level = level;
      this.message = message;
      if(throwable != null) {
        this.throwableInfo = throwable;
      }

      this.timeStamp = timeStamp;
      this.threadName = threadName;
      ndcLookupRequired = false;
      this.ndc = ndc;
      this.locationInfo = info;
      mdcCopyLookupRequired = false;
      if (properties != null) {
        mdcCopy = new java.util.Hashtable(properties);
      }
    }


  /**
     Set the location information for this logging event. The collected
     information is cached for future use.
   */
  public LocationInfo getLocationInformation() {
    if(locationInfo == null) {
      locationInfo = new LocationInfo(new Throwable(), getFQNOfLoggerClass());
    }
    return locationInfo;
  }

  /**
   * Return the level of this event. Use this form instead of directly
   * accessing the <code>level</code> field.  */
  public Level getLevel() {
    return (Level) level;
  }

  /**
   * Return the name of the logger. Use this form instead of directly
   * accessing the <code>categoryName</code> field.  
   */
  public String getLoggerName() {
    return categoryName;
  }

    /**
     * Gets the logger of the event.
     * Use should be restricted to cloning events.
     * @since 1.2.15
     */
    public Category getLogger() {
      return logger;
    }


  /**
     Returns the time when the application started, in milliseconds
     elapsed since 01.01.1970.  */
  public static long getStartTime() {
    return startTime;
  }


  /**
     Returns the throwable information contained within this
     event. May be <code>null</code> if there is no such information.

     <p>Note that the {@link Throwable} object contained within a
     {@link ThrowableInformation} does not survive serialization.

     @since 1.1 */
  public
  ThrowableInformation getThrowableInformation() {
    return throwableInfo;
  }


    /**
     * This removes the specified MDC property from the event.
     * Access to the MDC is not synchronized, so this
     * method should only be called when it is known that
     * no other threads are accessing the MDC.
     * @param propName the property name to remove
     * @since 1.2.16
     */
    public Object removeProperty(String propName) {
        if (mdcCopy == null) {
            getMDCCopy();
        }
        if (mdcCopy == null) {
            mdcCopy = new Hashtable();
        }
        return mdcCopy.remove(propName);
    }
}
