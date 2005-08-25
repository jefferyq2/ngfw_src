/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.nat;

import java.util.Iterator;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;

import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TransformContextFactory;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.firewall.MACAddress;

class DhcpMonitor implements Runnable
{    
    private static DhcpMonitor  INSTANCE = null;
    private static final int    DHCP_LEASE_ENTRY_LENGTH = 5;
    
    /* How often to poll the leases file, in milliseconds */
    private static final long   SLEEP_TIME              = 5000;
    
    /* Generate a absolute record every so many iterations */
    private static final int    ABSOLUTE_SKIP_COUNT     = 1440; // At 5 second intervals, this is 2 hours

    private static final String DHCP_LEASES_FILE        = "/var/lib/misc/dnsmasq.leases";
    private static final String DHCP_LEASE_DELIM        = " ";
    private static final String DHCP_EMPTY_HOSTNAME     = "*";

    private static final int    DHCP_LEASE_ENTRY_EOL    = 0;
    private static final int    DHCP_LEASE_ENTRY_MAC    = 1;
    private static final int    DHCP_LEASE_ENTRY_IP     = 2;
    private static final int    DHCP_LEASE_ENTRY_HOST   = 3;

    private static final Date   NEVER                   = new Date ( Long.MAX_VALUE );
        
    /* The thread the monitor is running on */
    private Thread thread = null;
    
    /* Status of the monitor */
    private volatile boolean isAlive = true;

    /* File being monitored */
    private final File dhcpFile = new File( DHCP_LEASES_FILE );

    /* Last time the event log was updated */
    private long lastUpdate = -1L;
    
    /* Next time that a lease is supposed to expire.  If there are no changes to a file
     * but a lease expires, then the leases must be reparsed */
    private Date nextExpiration = NEVER;

    /**
     * A map of all of the leases being tracked.
     */
    private final Map<IPaddr,DhcpLease> currentLeaseMap = new HashMap<IPaddr,DhcpLease>();
    
    private final Logger logger = Logger.getLogger( this.getClass());
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();
    
    private DhcpMonitor()
    {
    }
    
    public void run()
    {
        logger.debug( "Starting" );
        int c = 0;

        waitForTransformContext();

        if ( !isAlive ) {
            logger.error( "died before starting" );
            return;
        }

        while ( true ) {   
            if ( c <= 0 ) {
                logAbsolute();
                c = ABSOLUTE_SKIP_COUNT;
            }
            
            try { 
                Thread.sleep( SLEEP_TIME );
            } catch ( InterruptedException e ) {
                logger.info( "Interrupted: " );
            }
                        
            /* Check if the transform is still running */
            if ( !isAlive ) {
                break;
            }

            c--;
            
            try {
                /* The time right now to determine if leases have been expired */
                Date now = new Date();

                if ( hasDhcpChanged( now )) {
                    logChanges( now, false );
                }
            } catch ( SecurityException e ) {
                logger.error( "SecurityException when accessing file: " + DHCP_LEASES_FILE );
            }
        }

        logger.debug( "Finished" );

        /* Write an absolute lease map that is empty */
        eventLogger.info( new DhcpAbsoluteEvent());
    }

    synchronized void start()
    {
        isAlive = true;

        logger.debug( "Starting thread" );

        /* If thread is not-null, there is a running thread that thinks it is alive */
        if ( thread != null ) {
            logger.debug( "Monitor is already running" );
            return;
        }

        thread = new Thread( this );
        thread.start();
    }

    synchronized void stop()
    {
        if ( thread != null ) {
            logger.debug( "Stopping thread" );
            
            isAlive = false;
            try { 
                thread.interrupt();
                thread.join( SLEEP_TIME * 3 );
            } catch ( SecurityException e ) {
                logger.error( "security exception, impossible", e );
            } catch ( InterruptedException e ) {
                logger.error( "interrupted while stopping", e );
            }
            thread = null;
        }
    }


    private boolean hasDhcpChanged( Date now ) throws SecurityException
    {
        if ( lastUpdate < dhcpFile.lastModified()) return true;
        if ( now.after( nextExpiration )) return true;
        return false;
    }

    private void logAbsolute() throws SecurityException
    {
        /* Clear out all of the leases */
        currentLeaseMap.clear();
        
        logChanges( new Date(), true );
    }

    private void logChanges( Date now, boolean isAbsolute ) throws SecurityException
    {
        BufferedReader in = null;

        Set<IPaddr> deletedSet = new HashSet( currentLeaseMap.keySet());

        /* If there are no leases, this should never expire */
        nextExpiration = NEVER;

        try {
            in = new BufferedReader(new FileReader( DHCP_LEASES_FILE ));
            DhcpAbsoluteEvent absoluteEvent = null;
            if ( isAbsolute ) absoluteEvent = new DhcpAbsoluteEvent();

            String str;
            while (( str = in.readLine()) != null ) {
                logLease( str, now, absoluteEvent, deletedSet );
            }
            
            /* Log the absolute event */
            if ( isAbsolute ) eventLogger.info( absoluteEvent );
        } catch ( FileNotFoundException ex ) {
            logger.info( "The file: " + DHCP_LEASES_FILE + " does not exist yet" );
        } catch ( Exception ex ) {
            logger.error( "Error reading file: " + DHCP_LEASES_FILE, ex );
        } finally  {
            try { 
                if ( in != null ) in.close();
            } catch ( Exception ex ) {
                logger.error( "Error closing file: " + DHCP_LEASES_FILE, ex );
            }
        }

        for ( Iterator<IPaddr> iter = deletedSet.iterator() ; iter.hasNext() ; ) {
            IPaddr ip = iter.next();
            DhcpLease lease = currentLeaseMap.remove( ip );
            if ( lease == null ) {
                logger.error( "Logic error item only in deleted set " + ip.toString());
            }
            
            /* Log that an entry was deleted */
            eventLogger.info( new DhcpLeaseEvent( lease, DhcpLeaseEvent.RELEASE ));
        }
        
        /* Update the last time the file was modified */
        lastUpdate = now.getTime();
    }
    
    private void logLease( String str, Date now, DhcpAbsoluteEvent absoluteEvent, Set<IPaddr> deletedSet )
    {
        str = str.trim();
        String strArray[] = str.split( DHCP_LEASE_DELIM );
        String tmp;
        HostName host;
        Date eol;
        MACAddress mac;
        IPaddr ip;

        if ( strArray.length != DHCP_LEASE_ENTRY_LENGTH ) {
            logger.error( "Invalid DHCP lease: " + str );
            return;
        }
        
        tmp  = strArray[DHCP_LEASE_ENTRY_EOL];
        try {
            eol = new Date( Long.parseLong( tmp ) * 1000 );
        } catch ( NumberFormatException e ) {
            logger.error( "Invalid DHCP date: " + tmp );
            return;
        }
        
        tmp  = strArray[DHCP_LEASE_ENTRY_MAC];
        try {
            mac = MACAddress.parse( tmp );
        } catch ( ParseException e ) {
            logger.error( "Invalid MAC address: " + tmp );
            return;
        }
        
        tmp  = strArray[DHCP_LEASE_ENTRY_IP];
        try {
            ip = IPaddr.parse( tmp );
        } catch ( ParseException e ) {
            logger.error( "Invalid IP address: " + tmp, e );
            return;
        } catch ( UnknownHostException e ) {
            logger.error( "Invalid IP address: " + tmp, e );
            return;
        }
        
        tmp = strArray[DHCP_LEASE_ENTRY_HOST];
        try {
            if ( tmp.equals( DHCP_EMPTY_HOSTNAME )) {
                host = HostName.getEmptyHostName();
            } else {
                host = HostName.parse( tmp );
            }
        } catch ( ParseException e ) {
            logger.error( "Invalid hostname: " + tmp );
            return;
        }
                
        if ( absoluteEvent == null ) {
            logDhcpLease( eol, mac, ip, host, now );
        } else {
            DhcpLease lease = new DhcpLease( eol, mac, ip, host, now );
            if ( currentLeaseMap.put( ip, lease ) != null ) {
                logger.error( "Duplicate entry in absolute leases list" );
            }

            absoluteEvent.addAbsoluteLease( new DhcpAbsoluteLease( lease, now ));
            if ( lease.isActive() && nextExpiration.after( eol )) nextExpiration = eol;
        }
                
        /* Remove the item from the set of deleted items */
        deletedSet.remove( ip );
    }
    
    private void logDhcpLease( Date eol, MACAddress mac, IPaddr ip, HostName host, Date now )
    {
        /* Determine if this lease is already being tracked */
        DhcpLease lease = currentLeaseMap.get( ip );
        
        if ( lease == null ) {
            /* Add the lease to the map */
            lease = new DhcpLease( eol, mac, ip, host, now );
            currentLeaseMap.put( ip, lease );
            
            int eventType = lease.isActive() ? DhcpLeaseEvent.REGISTER : DhcpLeaseEvent.EXPIRE;
            logger.debug( "Logging new lease: " + ip.toString());
            
            eventLogger.info( new DhcpLeaseEvent( lease, eventType ));
        } else {
            if ( lease.hasChanged( eol, mac, ip, host, now )) {
                int eventType;
                if ( eol.after( now )) {
                    if ( lease.isRenewal( mac, host )) {
                        eventType = DhcpLeaseEvent.RENEW;
                    } else {
                        eventType = DhcpLeaseEvent.REGISTER;
                    }
                } else {
                    eventType = DhcpLeaseEvent.EXPIRE;
                }
                /* must update the lease here because the previous values are determine
                 * whether this is a release or renew */
                lease.set( eol, mac, ip, host, now );

                logger.debug( "Logging updated lease: " + ip.toString());
                eventLogger.info( new DhcpLeaseEvent( lease, eventType ));
            } else {
                logger.debug( "Lease hasn't changed: " + ip.toString());
            }
        }

        if ( lease.isActive() && nextExpiration.after( eol )) nextExpiration = eol;
    }

    private void waitForTransformContext()
    {
        Tid tid  = TransformContextFactory.context().getTid();
        
        while ( true ) {
            if ( MvvmContextFactory.context().transformManager().transformContext( tid ) != null ) {
                break;
            }
            
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                logger.info( "Interrupted" );
            }
            
            if ( !isAlive ) return;          
        }
    }

    static synchronized DhcpMonitor getInstance()
    {
        if ( INSTANCE == null ) 
            INSTANCE = new DhcpMonitor();

        return INSTANCE;
    }
}
