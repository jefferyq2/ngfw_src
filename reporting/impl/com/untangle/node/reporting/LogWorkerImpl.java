/**
 * $Id: LogWorker.java,v 1.00 2011/12/18 19:09:03 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogWorker;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogManager;
import com.untangle.uvm.util.TransactionWork;

/**
 * Worker that batches and flushes events to the database.
 */
public class LogWorkerImpl implements Runnable, LogWorker
{
    private static final int SYNC_TIME = 60*1000; /* 60 seconds */

    private final Logger logger = Logger.getLogger(getClass());

    private static boolean forceFlush = false;

    private static boolean running = false;

    private volatile Thread thread;
    
    private long lastLoggedWarningTime = System.currentTimeMillis();

    /**
     * This is a queue of incoming events
     */
    private final BlockingQueue<LogEvent> inputQueue = new LinkedBlockingQueue<LogEvent>();

    public LogWorkerImpl(ReportingNode node) { }

    public void run()
    {
        thread = Thread.currentThread();

        List<LogEvent> logQueue = new LinkedList<LogEvent>();
        LogEvent event = null;
        
        /**
         * Wait for all SQL conversions to complete
         */
        do {
            try {Thread.sleep(1000);} catch (Exception e) {}
        } while (!UvmContextFactory.context().loggingManager().isConversionComplete());
        
        /**
         * Loop indefinitely and continue logging events
         */
        while (thread != null) {
            /**
             * Sleep until next log time
             */
            if (!forceFlush)
                try {Thread.sleep(SYNC_TIME);} catch (Exception e) {}

            synchronized( this ) {
                /**
                 * Copy all events out of the queue
                 */
                while ((event = inputQueue.poll()) != null) {
                    logQueue.add(event);
                }
                
                /**
                 * If there is anything to log, log it to the database
                 */
                if (logQueue.size() > 0)
                    persist(logQueue);

                /**
                 * If the forceFlush flag was set, reset it and wake any interested parties
                 */
                if (forceFlush) {
                    forceFlush = false; //reset global flag
                    notifyAll(); /* notify any waiting threads that the flush is done */
                }
            }
        }

        /**
         * Log remaining events and exit
         */
        while ((event = inputQueue.poll()) != null) {
            logQueue.add(event);
        }
        if ( logQueue.size() > 0 ) {
            persist(logQueue);
        }
    }

    /**
     * Force currently queued events to the DB
     */
    public void forceFlush()
    {
        if (thread == null || running == false) {
            logger.warn("forceFlush() called, but reporting not running.");
            return;
        }

        /**
         * Wait on the flush to finish - we will get notified)
         */
        synchronized( this ) {
            forceFlush = true;
            logger.info("forceFlush()");
            thread.interrupt();

            while (true) {
                try {wait();} catch (java.lang.InterruptedException e) {}

                if (!forceFlush)
                    return;
            }
        }
    }
    
    public void logEvent(LogEvent event)
    {
        if (!running) {
            if (System.currentTimeMillis() - this.lastLoggedWarningTime > 10000) {
                logger.warn("Reporting node not found, discarding event");
                this.lastLoggedWarningTime = System.currentTimeMillis();
            }
            return;
        }
        
        String tag = "uvm[0]: ";
        event.setTag(tag);
        
        /**
         * Send to queue for database logging
         */
        if (!inputQueue.offer(event)) {
            logger.warn("dropping logevent: " + event);
        }

        /**
         * Send it to syslog (make best attempt - ignore errors)
         */
        try {
            UvmContextFactory.context().syslogManager().sendSyslog(event, event.getTag());
        } catch (Exception exn) { 
            logger.warn("failed to send syslog", exn);
        }
    }

    /**
     * write the logQueue to the database
     */
    private void persist(List<LogEvent> logQueue)
    {
        /**
         * These map stores the type of objects being written and stats purely for debugging output
         */
        Map<String,Integer> countMap = new HashMap<String, Integer>(); // Map from Event type to count of this type of event
        Map<String,Long> timeMap     = new HashMap<String, Long>();    // Map from Event type to culumalite time to write these events
        if (logger.isInfoEnabled()) {
            for (Iterator<LogEvent> i = logQueue.iterator(); i.hasNext(); ) {
                LogEvent event = i.next();

                /**
                 * Update the stats
                 */
                String eventTypeName = event.getClass().getSimpleName();
                Integer currentCount = countMap.get(eventTypeName);
                if (currentCount == null)
                    currentCount = 1;
                else
                    currentCount = currentCount+1;
                countMap.put(eventTypeName, currentCount);
            }
        }

        int count = logQueue.size();
        long t0 = System.currentTimeMillis();

        Session session = null; 
        Connection conn = null;
        Statement statement = null;
        try {
            session = UvmContextFactory.context().makeHibernateSession();
            conn = UvmContextFactory.context().getDBConnection();
            if (conn != null) 
                statement = conn.createStatement();
        } catch (Exception e) {
            logger.warn("Unable to create connection to DB",e);
        }
        
        logger.debug("Writing events to database... (size: " + logQueue.size() + ")");
        for (Iterator<LogEvent> i = logQueue.iterator(); i.hasNext(); ) {
            LogEvent event = i.next();

            if (event.isDirectEvent()) {
                if (conn != null && statement != null) {
                    /**
                     * Write event to database using SQL
                     * If fails, just move on
                     */
                    String sqlStr = event.getDirectEventSql();
                    if (sqlStr != null) {
                        logger.debug("Write direct event: " + sqlStr);
                        try {
                            long write_t0 = System.currentTimeMillis();
                            statement.execute(sqlStr);
                            long write_t1 = System.currentTimeMillis();

                            if (logger.isInfoEnabled()) {
                                /**
                                 * Update the stats
                                 */
                                String eventTypeName = event.getClass().getSimpleName();
                                Long currentTime = timeMap.get(eventTypeName);
                                if (currentTime == null)
                                    currentTime = 0L;
                                currentTime = currentTime+(write_t1-write_t0); //add time to write this instances
                                timeMap.put(eventTypeName, currentTime);
                            }
                        } catch (SQLException e) {
                            logger.warn("Failed SQL query: \"" + sqlStr + "\": " + e.getMessage());
                        }
                    }
                    List<String> sqls = event.getDirectEventSqls();
                    if (sqls != null) {
                        for (String sql : sqls) {
                            logger.debug("Write direct event: " + sql);
                            try {
                                statement.execute(sql);
                            } catch (SQLException e) {
                                logger.warn("Failed SQL query [" + event.getClass().toString() + "] : \"" + sql + "\": ",e);
                            }
                        }
                    }
                } 
            } else {
                if (session != null) {
                    /**
                     * Write event to database using hibernate
                     * If fails, just move on
                     */
                    try {
                        session.save(event);
                    } catch (Exception exc) {
                        logger.error("could not log event: ", exc);
                    }
                }
            }

            i.remove();
        }

        if (session != null) {
            try { session.flush(); } catch (Exception e) { logger.warn("Exception during session flush",e); }
            try { session.close(); } catch (Exception e) { logger.warn("Exception during session close",e); }
        }
        if (conn != null) {
            try {conn.close();} catch (SQLException e) { logger.warn("Exception during conn close",e); }
        }

        logger.debug("Writing events to database... Complete");

        /**
         * This looks at the event queue to be written and builds a summary string of the type of objects about to be written
         * Example: "SessionEvent[45,10ms] WebFilterEvent[10,20ms]
         */
        if (logger.isInfoEnabled()) {
            /**
             * Sort the list
             */
            LinkedList<EventTypeMap> eventTypeMapList = new LinkedList<EventTypeMap>();
            for ( String key : countMap.keySet() ) {
                eventTypeMapList.add(new EventTypeMap(key, countMap.get(key)));
            }
            Collections.sort(eventTypeMapList, new EventTypeMapComparator());

            /**
             * Build the output string
             */
            String mapOutput = "";
            for ( EventTypeMap item : eventTypeMapList ) {
                Long totalTimeMs = timeMap.get(item.name);
                if (totalTimeMs == null)
                    mapOutput += " " + item.name + "[" + item.count + "]";
                else
                    mapOutput += " " + item.name + "[" + item.count + "," + totalTimeMs + "ms" + "," + String.format("%.1f",((float)totalTimeMs/((float)(item.count)))) + "avg]";
            }

            long t1 = System.currentTimeMillis();
            logger.info("persist(): " + String.format("%5d",count) + " events [" + String.format("%5d",(t1-t0)) + " ms]" + mapOutput);
        }
    }

    protected void start()
    {
        this.running = true;
        UvmContextFactory.context().newThread(this).start();
    }

    protected void stop()
    {
        this.running = false;
        Thread t = thread;
        thread = null; /* thread will exit if thread is null */
        if (t != null) {
            t.interrupt();
        }
    }

    class EventTypeMap
    {
        public String name;
        public int count;
        
        public EventTypeMap(String name, int count)
        {
            this.name = name;
            this.count = count;
        }
    }
    
    class EventTypeMapComparator implements Comparator<EventTypeMap>
    {
        public int compare( EventTypeMap a, EventTypeMap b )
        {
            if (a.count < b.count) {
                return 1;
            } else if (a.count == b.count) {
                return 0;
            } else {
                return -1;
            }
        }
    }

}
