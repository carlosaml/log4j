package org.apache.log4j;

import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RendererSupport;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.Hashtable;

/**
 * Created with IntelliJ IDEA.
 * User: carlosaml
 * Date: 11/10/12
 * Time: 12:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class LogEventBase implements java.io.Serializable {
    /**
     * The category of the logging event. This field is not serialized
     * for performance reasons.
     *
     * <p>It is set by the LoggingEvent constructor or set by a remote
     * entity after deserialization.
     *
     * @deprecated This field will be marked as private or be completely
     * removed in future releases. Please do not use it.
     * */
    protected transient Category logger;
    /** The nested diagnostic context (NDC) of logging event. */
    protected String ndc;
    /** The mapped diagnostic context (MDC) of logging event. */
    protected Hashtable mdcCopy;
    /** Have we tried to do an NDC lookup? If we did, there is no need
     *  to do it again.  Note that its value is always false when
     *  serialized. Thus, a receiving SocketNode will never use it's own
     *  (incorrect) NDC. See also writeObject method. */
    protected boolean ndcLookupRequired = true;
    /** Have we tried to do an MDC lookup? If we did, there is no need
     *  to do it again.  Note that its value is always false when
     *  serialized. See also the getMDC and getMDCCopy methods.  */
    protected boolean mdcCopyLookupRequired = true;
    /** The application supplied message of logging event. */
    protected transient Object message;
    /** The application supplied message rendered through the log4j
        objet rendering mechanism.*/
    private String renderedMessage;
    /** The name of thread in which this logging event was generated. */
    protected String threadName;
    /** This
        variable contains information about this event's throwable
    */
    protected ThrowableInformation throwableInfo;

    public LogEventBase() {
        super();
    }

    /**
       Return the message for this logging event.

       <p>Before serialization, the returned object is the message
       passed by the user to generate the logging event. After
       serialization, the returned value equals the String form of the
       message possibly after object rendering.

       @since 1.1 */
    public
    Object getMessage() {
      if(message != null) {
        return message;
      } else {
        return getRenderedMessage();
      }
    }

    /**
     * This method returns the NDC for this event. It will return the
     * correct content even if the event was generated in a different
     * thread or even on a different machine. The {@link org.apache.log4j.NDC#get} method
     * should <em>never</em> be called directly.  */
    public
    String getNDC() {
      if(ndcLookupRequired) {
        ndcLookupRequired = false;
        ndc = NDC.get();
      }
      return ndc;
    }

    /**
        Returns the the context corresponding to the <code>key</code>
        parameter. If there is a local MDC copy, possibly because we are
        in a logging server or running inside AsyncAppender, then we
        search for the key in MDC copy, if a value is found it is
        returned. Otherwise, if the search in MDC copy returns a null
        result, then the current thread's <code>MDC</code> is used.

        <p>Note that <em>both</em> the local MDC copy and the current
        thread's MDC are searched.

    */
    public
    Object getMDC(String key) {
      Object r;
      // Note the mdcCopy is used if it exists. Otherwise we use the MDC
      // that is associated with the thread.
      if(mdcCopy != null) {
        r = mdcCopy.get(key);
        if(r != null) {
          return r;
        }
      }
      return MDC.get(key);
    }

    /**
       Obtain a copy of this thread's MDC prior to serialization or
       asynchronous logging.
    */
    public
    void getMDCCopy() {
      if(mdcCopyLookupRequired) {
        mdcCopyLookupRequired = false;
        // the clone call is required for asynchronous logging.
        // See also bug #5932.
        Hashtable t = MDC.getContext();
        if(t != null) {
      mdcCopy = (Hashtable) t.clone();
        }
      }
    }

    public
    String getRenderedMessage() {
       if(renderedMessage == null && message != null) {
         if(message instanceof String)
       renderedMessage = (String) message;
         else {
       LoggerRepository repository = logger.getLoggerRepository();

       if(repository instanceof RendererSupport) {
         RendererSupport rs = (RendererSupport) repository;
         renderedMessage= rs.getRendererMap().findAndRender(message);
       } else {
         renderedMessage = message.toString();
       }
         }
       }
       return renderedMessage;
    }

    public
    String getThreadName() {
      if(threadName == null)
        threadName = (Thread.currentThread()).getName();
      return threadName;
    }

    /**
       Return this event's throwable's string[] representaion.
    */
    public
    String[] getThrowableStrRep() {

      if(throwableInfo ==  null)
        return null;
      else
        return throwableInfo.getThrowableStrRep();
    }
}
