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

import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Node;

import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.ValidateException;

import com.untangle.uvm.logging.EventManager;


import java.util.List;

public interface VpnNode extends Node
{
    public void setVpnSettings( VpnSettings settings );
    public VpnSettings getVpnSettings();

    /* Create a client certificate, if the client already has a certificate
     * this will automatically revoke their old one */
    public VpnClientBase generateClientCertificate( VpnSettings settings, VpnClientBase client );

    /* Revoke a client license */
    public VpnClientBase revokeClientCertificate( VpnSettings settings, VpnClientBase client );

    /* Need the address to log where the request came from */
    public String lookupClientDistributionKey( String key, IPaddr address );

    /* Send out the client distribution */
    public void distributeClientConfig( VpnClientBase client ) throws NodeException;

    public enum ConfigState { UNCONFIGURED, CLIENT, SERVER_BRIDGE, SERVER_ROUTE }
    public ConfigState getConfigState();
    public HostAddress getVpnServerAddress();

    public void startConfig(ConfigState state) throws ValidateException;
    public void completeConfig() throws Exception;

    //// the stages of the setup wizard ///
    List<String> getAvailableUsbList() throws NodeException;
    public void downloadConfig( HostAddress address, int port, String key ) throws Exception;
    public void downloadConfigUsb( String name ) throws Exception;
    public void generateCertificate( CertificateParameters parameters ) throws Exception;
    public GroupList getAddressGroups() throws Exception;
    public void setAddressGroups( GroupList parameters ) throws Exception;
    public void setExportedAddressList( ExportList parameters ) throws Exception;
    public void setClients( ClientList parameters ) throws Exception;
    public void setSites( SiteList parameters ) throws Exception;

    /**
     * Access the EventManager for ClientConnectEvents
     */
    public EventManager<ClientConnectEvent> getClientConnectEventManager();
    /**
     * Access the EventManager for VpnStatisticEvents
     */    
    public EventManager<VpnStatisticEvent> getVpnStatisticEventManager();
    /**
     * Access the EventManager for ClientDistributionEvents
     */    
    public EventManager<ClientDistributionEvent> getClientDistributionEventManager();
}
