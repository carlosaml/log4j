package org.apache.log4j.pattern;

import junit.framework.TestCase;
import org.apache.log4j.*;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.util.SerializationTestHelper;

public class LogEventTest extends TestCase {

    public LogEventTest(final String name) {
            super(name);
        }

        /**
         * Serialize a simple logging event and check it against
         * a witness.
         * @throws Exception if exception during test.
         */
        public void testSerializationSimple() throws Exception {
            Logger root = Logger.getRootLogger();
            LogEvent event =
                    new LogEvent(
                            root.getClass().getName(), root, Level.INFO, "Hello, world.", null);
//    event.prepareForDeferredProcessing();

            int[] skip = new int[] { 352, 353, 354, 355, 356 };
            SerializationTestHelper.assertSerializationEquals(
                    "witness/serialization/simple.bin", event, skip, 237);
        }

        /**
         * Serialize a logging event with an exception and check it against
         * a witness.
         * @throws Exception if exception during test.
         *
         */
        public void testSerializationWithException() throws Exception {
            Logger root = Logger.getRootLogger();
            Exception ex = new Exception("Don't panic");
            LogEvent event =
                    new LogEvent(
                            root.getClass().getName(), root, Level.INFO, "Hello, world.", ex);
//    event.prepareForDeferredProcessing();

            int[] skip = new int[] { 352, 353, 354, 355, 356 };
            SerializationTestHelper.assertSerializationEquals(
                    "witness/serialization/exception.bin", event, skip, 237);
        }

        /**
         * Serialize a logging event with an exception and check it against
         * a witness.
         * @throws Exception if exception during test.
         *
         */
        public void testSerializationWithLocation() throws Exception {
            Logger root = Logger.getRootLogger();
            LogEvent event =
                    new LogEvent(
                            root.getClass().getName(), root, Level.INFO, "Hello, world.", null);
            event.getLocationInformation();
//    event.prepareForDeferredProcessing();

            int[] skip = new int[] { 352, 353, 354, 355, 356 };
            SerializationTestHelper.assertSerializationEquals(
                    "witness/serialization/location.bin", event, skip, 237);
        }

        /**
         * Serialize a logging event with ndc.
         * @throws Exception if exception during test.
         *
         */
        public void testSerializationNDC() throws Exception {
            Logger root = Logger.getRootLogger();
            NDC.push("ndc test");

            LogEvent event =
                    new LogEvent(
                            root.getClass().getName(), root, Level.INFO, "Hello, world.", null);
//    event.prepareForDeferredProcessing();

            int[] skip = new int[] { 352, 353, 354, 355, 356 };
            SerializationTestHelper.assertSerializationEquals(
                    "witness/serialization/ndc.bin", event, skip, 237);
        }

        /**
         * Serialize a logging event with mdc.
         * @throws Exception if exception during test.
         *
         */
        public void testSerializationMDC() throws Exception {
            Logger root = Logger.getRootLogger();
            MDC.put("mdckey", "mdcvalue");

            LogEvent event =
                    new LogEvent(
                            root.getClass().getName(), root, Level.INFO, "Hello, world.", null);
//    event.prepareForDeferredProcessing();

            int[] skip = new int[] { 352, 353, 354, 355, 356 };
            SerializationTestHelper.assertSerializationEquals(
                    "witness/serialization/mdc.bin", event, skip, 237);
        }

        /**
         * Deserialize a simple logging event.
         * @throws Exception if exception during test.
         *
         */
        public void testDeserializationSimple() throws Exception {
            Object obj =
                    SerializationTestHelper.deserializeStream(
                            "witness/serialization/simple.bin");
            assertTrue(obj instanceof LogEvent);

            LogEvent event = (LogEvent) obj;
            assertEquals("Hello, world.", event.getMessage());
            assertEquals(Level.INFO, event.getLevel());
        }

        /**
         * Deserialize a logging event with an exception.
         * @throws Exception if exception during test.
         *
         */
        public void testDeserializationWithException() throws Exception {
            Object obj =
                    SerializationTestHelper.deserializeStream(
                            "witness/serialization/exception.bin");
            assertTrue(obj instanceof LogEvent);

            LogEvent event = (LogEvent) obj;
            assertEquals("Hello, world.", event.getMessage());
            assertEquals(Level.INFO, event.getLevel());
        }

        /**
         * Deserialize a logging event with an exception.
         * @throws Exception if exception during test.
         *
         */
        public void testDeserializationWithLocation() throws Exception {
            Object obj =
                    SerializationTestHelper.deserializeStream(
                            "witness/serialization/location.bin");
            assertTrue(obj instanceof LogEvent);

            LogEvent event = (LogEvent) obj;
            assertEquals("Hello, world.", event.getMessage());
            assertEquals(Level.INFO, event.getLevel());
        }

        /**
         * Tests LogEvent.fqnOfCategoryClass.
         */
        public void testFQNOfCategoryClass() {
            Category root = Logger.getRootLogger();
            Priority info = Level.INFO;
            String catName = Logger.class.toString();
            LogEvent event =
                    new LogEvent(
                            catName, root, info, "Hello, world.", null);
            assertEquals(catName, event.fqnOfCategoryClass);
        }

        /**
         * Tests LogEvent.level.
         * @deprecated
         */
        public void testLevel() {
            Category root = Logger.getRootLogger();
            Priority info = Level.INFO;
            String catName = Logger.class.toString();
            LogEvent event =
                    new LogEvent(
                            catName, root, 0L,  info, "Hello, world.", null);
            Priority error = Level.ERROR;
            event.level = error;
            assertEquals(Level.ERROR, event.level);
        }

        /**
         * Tests LogEvent.getLocationInfo() when no FQCN is specified.
         * See bug 41186.
         */
        public void testLocationInfoNoFQCN() {
            Category root = Logger.getRootLogger();
            Priority level = Level.INFO;
            LogEvent event =
                    new LogEvent(
                            null, root, 0L,  level, "Hello, world.", null);
            LocationInfo info = event.getLocationInformation();
            //
            //  log4j 1.2 returns an object, its layout doesn't check for nulls.
            //  log4j 1.3 returns a null.
            //
            assertNotNull(info);
            if (info != null) {
                assertEquals("?", info.getLineNumber());
                assertEquals("?", info.getClassName());
                assertEquals("?", info.getFileName());
                assertEquals("?", info.getMethodName());
            }
        }

        /**
         * Message object that throws a RuntimeException on toString().
         * See bug 37182.
         */
        private static class BadMessage {
            public BadMessage() {
            }

            public String toString() {
                throw new RuntimeException();
            }
        }

        /**
         * Tests that an runtime exception or error during toString
         * on the message parameter does not propagate to caller.
         * See bug 37182.
         */
        public void testBadMessage() {
            Category root = Logger.getRootLogger();
            Priority info = Level.INFO;
            String catName = Logger.class.toString();
            BadMessage msg = new BadMessage();
            LogEvent event =
                    new LogEvent(
                            catName, root, 0L,  info, msg, null);
            //  would result in exception in earlier versions
            event.getRenderedMessage();
        }




}
