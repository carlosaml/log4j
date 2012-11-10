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
import org.apache.log4j.helpers.Loader;
import org.apache.log4j.helpers.LogLog;

import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

  /** Fully qualified name of the calling category class. */
  transient public final String fqnOfCategoryClass;

  /** 
   * <p>The category (logger) name.
   *   
   * @deprecated This field will be marked as private in future
   * releases. Please do not access it directly. Use the {@link
   * #getLoggerName} method instead.

   * */
  final public String categoryName;

  /** 
   * Level of logging event. Level cannot be serializable because it
   * is a flyweight.  Due to its special seralization it cannot be
   * declared final either.
   *   
   * <p> This field should not be accessed directly. You shoud use the
   * {@link #getLevel} method instead.
   *
   * @deprecated This field will be marked as private in future
   * releases. Please do not access it directly. Use the {@link
   * #getLevel} method instead.
   * */
  transient public Priority level;

  /** The number of milliseconds elapsed from 1/1/1970 until logging event
      was created. */
  public final long timeStamp;
  /** Location information for the caller. */
  private LocationInfo locationInfo;

  // Serialization
  static final long serialVersionUID = -868428216207166145L;

  static final Integer[] PARAM_ARRAY = new Integer[1];
  static final String TO_LEVEL = "toLevel";
  static final Class[] TO_LEVEL_PARAMS = new Class[] {int.class};
  static final Hashtable methodCache = new Hashtable(3); // use a tiny table

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
      locationInfo = new LocationInfo(new Throwable(), fqnOfCategoryClass);
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

  private
  void readLevel(ObjectInputStream ois)
                      throws java.io.IOException, ClassNotFoundException {

    int p = ois.readInt();
    try {
      String className = (String) ois.readObject();
      if(className == null) {
	level = Level.toLevel(p);
      } else {
	Method m = (Method) methodCache.get(className);
	if(m == null) {
	  Class clazz = Loader.loadClass(className);
	  // Note that we use Class.getDeclaredMethod instead of
	  // Class.getMethod. This assumes that the Level subclass
	  // implements the toLevel(int) method which is a
	  // requirement. Actually, it does not make sense for Level
	  // subclasses NOT to implement this method. Also note that
	  // only Level can be subclassed and not Priority.
	  m = clazz.getDeclaredMethod(TO_LEVEL, TO_LEVEL_PARAMS);
	  methodCache.put(className, m);
	}
	level = (Level) m.invoke(null,  new Integer[] { new Integer(p) } );
      }
    } catch(InvocationTargetException e) {
        if (e.getTargetException() instanceof InterruptedException
                || e.getTargetException() instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
        }
    LogLog.warn("Level deserialization failed, reverting to default.", e);
	level = Level.toLevel(p);
    } catch(NoSuchMethodException e) {
	LogLog.warn("Level deserialization failed, reverting to default.", e);
	level = Level.toLevel(p);
    } catch(IllegalAccessException e) {
	LogLog.warn("Level deserialization failed, reverting to default.", e);
	level = Level.toLevel(p);
    } catch(RuntimeException e) {
	LogLog.warn("Level deserialization failed, reverting to default.", e);
	level = Level.toLevel(p);
    }
  }

  private void readObject(ObjectInputStream ois)
                        throws java.io.IOException, ClassNotFoundException {
    ois.defaultReadObject();
    readLevel(ois);

    // Make sure that no location info is available to Layouts
    if(locationInfo == null)
      locationInfo = new LocationInfo(null, null);
  }

  private
  void writeObject(ObjectOutputStream oos) throws java.io.IOException {
    // Aside from returning the current thread name the wgetThreadName
    // method sets the threadName variable.
    this.getThreadName();

    // This sets the renders the message in case it wasn't up to now.
    this.getRenderedMessage();

    // This call has a side effect of setting this.ndc and
    // setting ndcLookupRequired to false if not already false.
    this.getNDC();

    // This call has a side effect of setting this.mdcCopy and
    // setting mdcLookupRequired to false if not already false.
    this.getMDCCopy();

    // This sets the throwable sting representation of the event throwable.
    this.getThrowableStrRep();

    oos.defaultWriteObject();

    // serialize this event's level
    writeLevel(oos);
  }

  private
  void writeLevel(ObjectOutputStream oos) throws java.io.IOException {

    oos.writeInt(level.toInt());

    Class clazz = level.getClass();
    if(clazz == Level.class) {
      oos.writeObject(null);
    } else {
      // writing directly the Class object would be nicer, except that
      // serialized a Class object can not be read back by JDK
      // 1.1.x. We have to resort to this hack instead.
      oos.writeObject(clazz.getName());
    }
  }

    /**
     * Set value for MDC property.
     * This adds the specified MDC property to the event.
     * Access to the MDC is not synchronized, so this
     * method should only be called when it is known that
     * no other threads are accessing the MDC.
     * @since 1.2.15
     * @param propName
     * @param propValue
     */
  public final void setProperty(final String propName,
                          final String propValue) {
        if (mdcCopy == null) {
            getMDCCopy();
        }
        if (mdcCopy == null) {
            mdcCopy = new Hashtable();
        }
        mdcCopy.put(propName, propValue);      
  }

    /**
     * Return a property for this event. The return value can be null.
     *
     * Equivalent to getMDC(String) in log4j 1.2.  Provided
     * for compatibility with log4j 1.3.
     *
     * @param key property name
     * @return property value or null if property not set
     * @since 1.2.15
     */
    public final String getProperty(final String key) {
        Object value = getMDC(key);
        String retval = null;
        if (value != null) {
            retval = value.toString();
        }
        return retval;
    }

    /**
     * Check for the existence of location information without creating it
     * (a byproduct of calling getLocationInformation).
     * @return true if location information has been extracted.
     * @since 1.2.15
     */
    public final boolean locationInformationExists() {
      return (locationInfo != null);
    }

    /**
     * Getter for the event's time stamp. The time stamp is calculated starting
     * from 1970-01-01 GMT.
     * @return timestamp
     *
     * @since 1.2.15
     */
    public final long getTimeStamp() {
      return timeStamp;
    }

    /**
     * Returns the set of the key values in the properties
     * for the event.
     *
     * The returned set is unmodifiable by the caller.
     *
     * Provided for compatibility with log4j 1.3
     *
     * @return Set an unmodifiable set of the property keys.
     * @since 1.2.15
     */
    public Set getPropertyKeySet() {
      return getProperties().keySet();
    }

    /**
     * Returns the set of properties
     * for the event.
     *
     * The returned set is unmodifiable by the caller.
     *
     * Provided for compatibility with log4j 1.3
     *
     * @return Set an unmodifiable map of the properties.
     * @since 1.2.15
     */
    public Map getProperties() {
      getMDCCopy();
      Map properties;
      if (mdcCopy == null) {
         properties = new HashMap();
      } else {
         properties = mdcCopy;
      }
      return Collections.unmodifiableMap(properties);
    }

    /**
     * Get the fully qualified name of the calling logger sub-class/wrapper.
     * Provided for compatibility with log4j 1.3
     * @return fully qualified class name, may be null.
     * @since 1.2.15
     */
    public String getFQNOfLoggerClass() {
      return fqnOfCategoryClass;
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
