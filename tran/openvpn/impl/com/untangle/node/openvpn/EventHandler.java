/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.openvpn;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.tapi.AbstractEventHandler;
import com.untangle.uvm.tapi.IPNewSessionRequest;
import com.untangle.uvm.tapi.MPipeException;
import com.untangle.uvm.tapi.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.tapi.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import org.apache.log4j.Logger;

class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger( EventHandler.class );

    /* Are the VPNs bridged with the other networks */
    private boolean isBridge = false;

    /* Is this a VPN client, a VPN client passes all traffic */
    private boolean isUntanglePlatformClient = false;

    /* Any client can connect to any exported address and vice versa */
    private List <IPMatcher> clientAddressList = new LinkedList<IPMatcher>();
    private List <IPMatcher> exportedAddressList = new LinkedList<IPMatcher>();

    /* Firewall Node */
    private final VpnNodeImpl node;

    EventHandler( VpnNodeImpl node )
    {
        super(node);

        this.node = node;
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequestEvent event )
        throws MPipeException
    {
        handleNewSessionRequest( event.sessionRequest());
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequestEvent event )
        throws MPipeException
    {
        handleNewSessionRequest( event.sessionRequest());
    }

    private void handleNewSessionRequest( IPNewSessionRequest request )
    {
        if ( logger.isDebugEnabled()) logger.debug( "New session: [" + request.id() + "]" );

        if ( request.clientIntf() != IntfConstants.VPN_INTF &&
             request.serverIntf() != IntfConstants.VPN_INTF ) {
            /* Nothing to do */
            request.release();
            return;
        }
        else if ( request.clientIntf() == IntfConstants.VPN_INTF &&
                  request.serverIntf() == IntfConstants.VPN_INTF ) {
            /* XXXXXXXXXX sort this out */
            request.release();
            return;
        }
        /* XXXX Handle bridging */
        else if ( request.clientIntf() == IntfConstants.VPN_INTF ) {
            /* VPN client going to another interface */
            checkAddress( request, request.clientAddr(), request.serverAddr());
        }
        else {
            /* Local user trying to reach a VPN client */
            checkAddress( request, request.serverAddr(), request.clientAddr());
        }
    }

    /**
     * Check whether vpn is allowed to talk to local
     */
    private void checkAddress( IPNewSessionRequest request, InetAddress vpn, InetAddress local )
    {
        boolean isValid = false;

        /* Clients pass all traffic */
        if ( this.isUntanglePlatformClient ) {
            node.incrementCount( Constants.PASS_COUNTER );
            request.release();

            return;
        }

        for ( IPMatcher matcher : this.clientAddressList ) {
            if ( logger.isDebugEnabled()) {
                logger.debug( "Testing vpn " + vpn.getHostAddress() + " against " + matcher );
            }
            if ( matcher.isMatch( vpn )) {
                isValid = true;
                break;
            }
        }

        if ( !isValid ) {
            reject( request );
            return;
        }

        /* The local address must be one of the exported addresses */
        isValid = false;

        for ( IPMatcher matcher : this.exportedAddressList ) {
            if ( logger.isDebugEnabled()) {
                logger.debug( "Testing local " + local.getHostAddress() + " against " + matcher );
            }
            if ( matcher.isMatch( local )) {
                isValid = true;
                break;
            }
        }

        if ( !isValid ) {
            reject( request );
            return;
        }

        /* Accept the request */
        if ( logger.isDebugEnabled()) {
            logger.debug( "Accepted VPN session: [" + request.id() + "]" );
        }
        node.incrementCount( Constants.PASS_COUNTER );
        request.release();

        /* XXX Probably want to create an event */
        // node.statisticManager.incrRequest( protocol, request, reject );
    }

    private void reject( IPNewSessionRequest request )
    {
        /* XXX Should this always reject silently */
        request.rejectSilently();

        node.incrementCount( Constants.BLOCK_COUNTER );

        /* XXX Probably want to create an event */
        if ( logger.isDebugEnabled()) {
            logger.debug( "Blocked VPN session: [" + request.id() + "]" );
        }

        // node.statisticManager.incrRequest( protocol, request, reject );
    }

    void configure( VpnSettings settings )
    {
        logger.debug( "Configuring handler" );

        if ( settings.isUntanglePlatformClient()) {
            isUntanglePlatformClient = settings.isUntanglePlatformClient();
            return;
        }

        /* Create temporary lists */
        List <IPMatcher> clientAddressList = new LinkedList<IPMatcher>();
        List <IPMatcher> exportedAddressList = new LinkedList<IPMatcher>();

        IPMatcherFactory imf = IPMatcherFactory.getInstance();

        for ( VpnGroup group : (List<VpnGroup>)settings.getGroupList()) {
            /* Don't insert inactive groups */
            if ( !group.isLive()) continue;
            IPMatcher matcher = imf.makeSubnetMatcher( group.getAddress(), group.getNetmask());

            clientAddressList.add( matcher );
            if (logger.isDebugEnabled()) {
                logger.debug( "clientAddressList: [" + matcher + "]" );
            }
        }

        for ( VpnSite site : (List<VpnSite>)settings.getSiteList()) {
            /* Continue if the client isn't live or the group the client is in isn't live */
            if ( !site.isEnabled()) continue;

            for ( ClientSiteNetwork siteNetwork : site.getExportedAddressList()) {
                if ( !siteNetwork.isLive()) continue;
                IPMatcher matcher =
                    imf.makeSubnetMatcher( siteNetwork.getNetwork(), siteNetwork.getNetmask());

                clientAddressList.add( matcher );
                if (logger.isDebugEnabled()) {
                    logger.debug( "clientAddressList: [" + matcher + "]" );
                }
            }
        }

        for ( ServerSiteNetwork siteNetwork : settings.getExportedAddressList()) {
            if ( !siteNetwork.isLive()) continue;
            IPMatcher matcher =
                imf.makeSubnetMatcher( siteNetwork.getNetwork(), siteNetwork.getNetmask());

            exportedAddressList.add( matcher );
            if (logger.isDebugEnabled()) {
                logger.debug( "exportedAddressList: [" + matcher + "]" );
            }
        }

        this.clientAddressList   = clientAddressList;
        this.exportedAddressList = exportedAddressList;

        logger.debug( "" );
    }
}
