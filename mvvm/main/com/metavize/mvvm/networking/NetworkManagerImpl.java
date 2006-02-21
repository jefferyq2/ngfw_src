/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import java.util.Set;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.metavize.jnetcap.Netcap;

import com.metavize.mvvm.NetworkManager;
import com.metavize.mvvm.IntfEnum;
import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.MvvmContextFactory;

import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.argon.ArgonException;

import com.metavize.mvvm.tran.ValidateException;
import com.metavize.mvvm.tran.script.ScriptWriter;
import com.metavize.mvvm.tran.script.ScriptRunner;

import com.metavize.mvvm.networking.internal.ServicesInternalSettings;
import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.metavize.mvvm.networking.internal.RemoteInternalSettings;
import com.metavize.mvvm.networking.internal.NetworkSpaceInternal;
import com.metavize.mvvm.networking.internal.RouteInternal;
import com.metavize.mvvm.networking.internal.InterfaceInternal;

import com.metavize.mvvm.util.DataLoader;
import com.metavize.mvvm.util.DataSaver;


/* XXX This shouldn't be public */
public class NetworkManagerImpl implements NetworkManager
{
    private static NetworkManagerImpl INSTANCE = null;

    static final String ETC_INTERFACES_FILE = "/etc/network/interfaces";
    static final String ETC_RESOLV_FILE = "/etc/resolv.conf";
    
    private static final Logger logger = Logger.getLogger( NetworkManagerImpl.class );

    static final String BUNNICULA_BASE = System.getProperty( "bunnicula.home" );
    static final String BUNNICULA_CONF = System.getProperty( "bunnicula.conf.dir" );

    /* Script to run whenever the interfaces should be reconfigured */
    private static final String NET_CONFIGURE_SCRIPT = BUNNICULA_BASE + "/networking/configure";
    
    /* Script to run whenever the iptables should be updated */
    private static final String IPTABLES_SCRIPT      = BUNNICULA_BASE + "/networking/rule-generator";

    /* Script to run renew the DHCP lease */
    private static final String DHCP_RENEW_SCRIPT    = BUNNICULA_BASE + "/networking/dhcp-renew";

    /* A flag for devel environments, used to determine whether or not 
     * the etc files actually are written, this enables/disables reconfiguring networking */
    private boolean saveSettings = true;
    
    /* Inidicates whether or not the networking manager has been initialized */
    private boolean isInitialized = false;

    /* Manager for the iptables rules */
    private RuleManager ruleManager;

    /* Manager for the DHCP/DNS server */
    private DhcpManager dhcpManager;

    /* Converter to create the initial networking configuration object if
     * network spaces has never been executed before */
    private NetworkConfigurationLoader networkConfigurationLoader;

    /* ??? Does the order matter, it shouldn't.  */
    private Set<NetworkSettingsListener> networkListeners = new HashSet<NetworkSettingsListener>();
    private Set<IntfEnumListener> intfEnumListeners = new HashSet<IntfEnumListener>();
    private Set<RemoteSettingsListener> remoteListeners = new HashSet<RemoteSettingsListener>();

    /** The nuts and bolts of networking, the real bits of panther.  this my friend
     * should never be null */
    private NetworkSpacesInternalSettings networkSettings = null;

    /** The configuration for the DHCP/DNS Server */
    private ServicesInternalSettings servicesSettings = null;

    /* These are the "networking" settings that aren't related to the nuts and bolts of
     * network spaces.  Things like SSH support */
    private RemoteInternalSettings remote = null;

    /* These are the dynamic dns settings */
    private DynamicDNSSettings ddnsSettings = null;

    /* the netcap  */
    private final Netcap netcap = Netcap.getInstance();

    /* Flag to indicate when the MVVM has been shutdown */
    private boolean isShutdown = false;

    /* Interface enum */
    private IntfEnum intfEnum;

    private NetworkManagerImpl()
    {
        this.ruleManager = RuleManager.getInstance();
        this.networkConfigurationLoader = NetworkConfigurationLoader.getInstance();
        this.dhcpManager  = new DhcpManager();
    }

    /**
     * The init function cannot fail, if it does, reasonable defaults
     * must be used, so if initPriv fails(which is why it throws
     * Exception), then this function grabs reasonable defaults and
     * moves on
     */
    public synchronized void init()
    {
        if ( isInitialized ) {
            logger.error( "Attempt to reinitialize the networking manager", new Exception());
            return;
        }
        
        try {
            initPriv();
        } catch ( Exception e ) {
            logger.error( "Exception initializing settings, using reasonable defaults", e );

            /* !!!!!!!! use reasonable defaults */
        }

        this.isInitialized = true;
    }

    public NetworkingConfiguration getNetworkingConfiguration()
    {
        return NetworkUtilPriv.getPrivInstance().
            toConfiguration( this.networkSettings, this.remote.toSettings());
    }
    
    public synchronized void setNetworkingConfiguration( NetworkingConfiguration configuration )
        throws NetworkException, ValidateException
    {
        setNetworkSettings( NetworkUtilPriv.getPrivInstance().
                            toInternal( configuration, this.networkSettings ));
        setRemoteSettings( configuration );
    }

    public NetworkSpacesSettingsImpl getNetworkSettings()
    {
        return NetworkUtilPriv.getPrivInstance().toSettings( this.networkSettings );
    }

    public synchronized void setNetworkSettings( NetworkSpacesSettings settings )
        throws NetworkException, ValidateException
    {
        logger.debug( "Loading the new network settings: " + settings );
        setNetworkSettings( NetworkUtilPriv.getPrivInstance().toInternal( settings ));
        
        /* Have to update the remote settings */
        updateRemoteSettings();
    }

    private synchronized void setNetworkSettings( NetworkSpacesInternalSettings newSettings )
        throws NetworkException, ValidateException
    {
        try {
            newSettings = NetworkUtilPriv.getPrivInstance().updateDhcpAddresses( newSettings );
        } catch ( Exception e ) {
            logger.error( "Unable to update dhcp addresses", e );
        }

        logger.debug( "Loading the new network settings: " + newSettings );
        /* XXXX implement me */
        // throw new IllegalStateException( "implement me" );

        /* Write the settings */
        writeConfiguration( newSettings );
        
        /* Actually load the new settings */
        
        this.networkSettings = newSettings;

        /* call the listeners */
        callNetworkListeners();        
    }

    public RemoteSettings getRemoteSettings()
    {
        return this.remote.toSettings();
    }

    public synchronized void setRemoteSettings( RemoteSettings remote )
        throws NetworkException
    {
        /* XXXXXXXXX Implement me */
        NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();
        String pa = nup.getPublicAddress( this.networkSettings, remote, this.ddnsSettings );

        this.remote = new RemoteInternalSettings( remote, pa );

        if ( logger.isDebugEnabled()) logger.debug( "Loaded remote settings: " + this.remote );

        callRemoteListeners();
    }

    public NetworkSpacesInternalSettings getNetworkInternalSettings()
    {
        return this.networkSettings;
    }
    
    /* XXXX This is kind of busted since you can't change the services on/off switch from here */
    /* XXXX This is just kind of busted because it passes the same argument twice */
    public synchronized void setServicesSettings( ServicesSettings servicesSettings )
        throws NetworkException
    {
        setServicesSettings( servicesSettings, servicesSettings );
    }

    public synchronized void setServicesSettings( DhcpServerSettings dhcp, DnsServerSettings dns )
        throws NetworkException
    {
        logger.debug( "Loading the new dhcp settings: " + dhcp );
        logger.debug( "Loading the new dns settings: " + dns );

        this.servicesSettings = 
            NetworkUtilPriv.getPrivInstance().toInternal( this.networkSettings, dhcp, dns );

        this.dhcpManager.configure( this.servicesSettings );
        // !!!!!! this.dhcpManager.startDnsMasq();
    }

    public ServicesInternalSettings getServicesInternalSettings()
    {
        return this.servicesSettings;
    }

    public synchronized void startServices() throws NetworkException
    {
        this.dhcpManager.configure( servicesSettings );
        // !!!!!!! this.dhcpManager.startDnsMasq();
    }

    public synchronized void stopServices()
    {
        this.dhcpManager.deconfigure();
    }

    public synchronized DynamicDNSSettings getDynamicDnsSettings()
    {
        if ( this.ddnsSettings == null ) {
            logger.error( "null ddns settings, returning fresh object." );
            this.ddnsSettings = new DynamicDNSSettings();
            this.ddnsSettings.setEnabled( false );
        }

        logger.debug( "getting ddns settings: " + this.ddnsSettings );
        
        return this.ddnsSettings;
    }
            
    public synchronized void setDynamicDnsSettings( DynamicDNSSettings newSettings )
    {
        logger.debug( "Saving new ddns settings: " + newSettings );
        saveDynamicDnsSettings( newSettings );
        
        /* XXX Do whatever Dynamic DNS has to do */
    }

    public synchronized void disableNetworkSpaces()
    {
        
    }

    public synchronized void enableNetworkSpaces()
    {
        
    }

    /* Get the external HTTPS port */
    public int getPublicHttpsPort()
    {
        return this.remote.getPublicHttpsPort();
    }
    
    /* Renew the DHCP address and return a new network settings with the updated address */
    public synchronized NetworkingConfiguration renewDhcpLease() throws NetworkException
    {
        renewDhcpLease( 0 );
        
        return getNetworkingConfiguration();
    }

    /* Renew the DHCP address for a network space. */
    public synchronized DhcpStatus renewDhcpLease( int index ) throws NetworkException
    {
        if (( index < 0 ) || ( index >= this.networkSettings.getNetworkSpaceList().size())) {
            throw new NetworkException( "There isn't a network space at index " + index );
        }

        boolean isPrimary = ( index == 0 );
        
        NetworkSpaceInternal space = this.networkSettings.getNetworkSpaceList().get( index );
        
        if ( !space.getIsDhcpEnabled()) {
            throw new NetworkException( "DHCP is not enabled on this network space." );
        }

        /* Renew the address */
        try {
            String flags = "";
            
            if ( !isPrimary ) flags = InterfacesScriptWriter.DHCP_FLAG_ADDRESS_ONLY;
            
            ScriptRunner.getInstance().exec( DHCP_RENEW_SCRIPT, space.getDeviceName(), 
                                             String.valueOf( space.getIndex()), flags );
        } catch ( Exception e ) { 
            logger.warn( "Error renewing DHCP address", e );
            throw new NetworkException( "Unable to renew the DHCP lease" );
        }

        /* Update the address and generate new rules */
        updateAddress();

        /* Get the new space (the settings get updated by updateAddress) */
        if (( index < 0 ) || ( index >= this.networkSettings.getNetworkSpaceList().size())) {
            throw new NetworkException( "There is no longer a network space at index " + index );
        }
        
        space = this.networkSettings.getNetworkSpaceList().get( index );
        
        if ( !space.getIsDhcpEnabled()) {
            throw new NetworkException( "DHCP is no longer enabled on this network space." );
        }
        
        IPNetwork network = space.getPrimaryAddress();
        if ( !isPrimary ) return new DhcpStatus( network.getNetwork(), network.getNetmask());
        
        return new DhcpStatus( network.getNetwork(), network.getNetmask(), 
                               this.networkSettings.getDefaultRoute(),
                               this.networkSettings.getDns1(), this.networkSettings.getDns2());
    }

    /* Retrieve the enumeration of all of the active interfaces */
    public IntfEnum getIntfEnum()
    {
        return this.intfEnum;
    }

    public String getHostname()
    {
        return this.remote.getHostname();
    }

    public String getPublicAddress()
    {
        return this.remote.getCurrentPublicAddress();
    }
    
    public synchronized void updateAddress()
    {
        /* Get the new address for any dhcp spaces */
        NetworkSpacesInternalSettings previous = this.networkSettings;
        
        try {
            /* Update the netcap address */
            Netcap.getInstance().updateAddress();

            this.networkSettings = NetworkUtilPriv.getPrivInstance().updateDhcpAddresses( previous );
        } catch ( Exception e ) {
            logger.error( "Exception updating address, reverting to previous settings", e );
            this.networkSettings = previous;
        }
    }

    public void disableDhcpForwarding()
    {
        this.ruleManager.dhcpEnableForwarding( false );
    }
    
    public void enableDhcpForwarding()
    {
         this.ruleManager.dhcpEnableForwarding( true );
    }

    /* This relic really should go away.  In production environments, none of the
     * interfaces are antisubscribed (this is the way it should be).
     * the antisubscribes are then for specific traffic protocols, such as HTTP, */
    public void subscribeLocalOutside( boolean newValue )
    {
        this.ruleManager.subscribeLocalOutside( newValue );
    }
    
    /** Public (private, only in impl) methods  */
    
    /* Update all of the iptables rules and the inside address database */
    private void generateRules() throws NetworkException
    {
        this.ruleManager.generateIptablesRules();
    }

    private void updateRemoteSettings() throws NetworkException
    {
        /* This just reinitializes the public address */
        setRemoteSettings( this.remote.toSettings());
    }
    
    public void isShutdown()
    {
        this.isShutdown = true;
        this.ruleManager.isShutdown();
    }
    
    public void flushIPTables() throws NetworkException
    {
        this.ruleManager.destroyIptablesRules();
    }

    /* Listener functions */
    private void callNetworkListeners()
    {
        for ( NetworkSettingsListener listener : this.networkListeners ) {
            try {
                listener.event( this.networkSettings );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
    }

    public void registerListener( NetworkSettingsListener networkListener )
    {
        this.networkListeners.add( networkListener );
    }

    public void unregisterListener( NetworkSettingsListener networkListener )
    {
        this.networkListeners.remove( networkListener );
    }

    /* Remote settings */
    private void callRemoteListeners()
    {
        for ( RemoteSettingsListener listener : this.remoteListeners ) {
            try {
                listener.event( this.remote );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
    }

    public void registerListener( RemoteSettingsListener remoteListener )
    {
        this.remoteListeners.add( remoteListener );
    }

    public void unregisterListener( RemoteSettingsListener remoteListener )
    {
        this.remoteListeners.remove( remoteListener );
    }

    /* Intf enum settings */
    private void callIntfEnumListeners()
    {
        for ( IntfEnumListener listener : this.intfEnumListeners ) {
            try {
                listener.event( this.intfEnum );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
    }

    public void registerListener( IntfEnumListener intfEnumListener )
    {
        this.intfEnumListeners.add( intfEnumListener );
    }

    public void unregisterListener( IntfEnumListener intfEnumListener )
    {
        this.intfEnumListeners.remove( intfEnumListener );
    }

    /**** private methods ***/
    private void initPriv() throws NetworkException, ValidateException
    {
        /* !!!! Load settings */

        String disableSaveSettings = System.getProperty( "bunnicula.devel.nonetworking" );

        if ( Boolean.valueOf( disableSaveSettings ) == true ) this.saveSettings = false;
        
        NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();
                
        /* If there are no settings, get the settings from the database */
        if ( this.networkSettings == null ) {
            /* Need to create new settings, (The method setNetworkingConfiguration assumes that
             * settings is already set, and cannot be used here) */
            NetworkingConfiguration configuration = networkConfigurationLoader.getNetworkingConfiguration();
            
            /* Save these settings */
            NetworkSpacesInternalSettings internal = nup.toInternal( configuration );
            

            if ( logger.isDebugEnabled()) {
                logger.debug( "Loaded the configuration: \n" + configuration );
                logger.debug( "Converted to: \n" + internal );
            }
            
            /* Save the network settings */
            setNetworkSettings( internal );
            setRemoteSettings( configuration );

            /* Get new settings for the services */
            setServicesSettings( nup.getDefaultServicesSettings());

            /* Create new dynamic dns settings */
            DynamicDNSSettings ddns = new DynamicDNSSettings();
            ddns.setEnabled( false );
            setDynamicDnsSettings( ddns );
        }

        updateAddress();

        /* Generate rules */
        generateRules();
    }    
    
    /* Methods for writing the configuration files */
    private void writeConfiguration( NetworkSpacesInternalSettings newSettings ) throws NetworkException
    {
        
        if ( this.saveSettings == false ) {
            /* Set to a warn, because if this gets emailed out, something has gone terribly awry */
            logger.warn( "Not writing configuration files because the debug property was set" );
            return;
        }

        try {
            writeEtcFiles( newSettings );
        } catch ( ArgonException e ) {
            logger.error( "Unable to write network settings" );
        }
    }

    private void writeEtcFiles( NetworkSpacesInternalSettings newSettings ) 
        throws NetworkException, ArgonException
    {
        writeInterfaces( newSettings );
        writeResolvConf( newSettings );
    }

    /* This is for /etc/network/interfaces interfaces */
    private void writeInterfaces( NetworkSpacesInternalSettings newSettings )
        throws NetworkException, ArgonException
    {
        /* This is a script writer customized to generate etc interfaces files */
        InterfacesScriptWriter isw = new InterfacesScriptWriter( newSettings );
        isw.addNetworkSettings();
        isw.writeFile( ETC_INTERFACES_FILE );
    }

    private void writeResolvConf( NetworkSpacesInternalSettings newSettings )
    {
        /* This is a script writer customized to generate etc resolv.conf files */
        ResolvScriptWriter rsw = new ResolvScriptWriter( newSettings );
        rsw.addNetworkSettings();
        rsw.writeFile( ETC_RESOLV_FILE );
    }

    /* Methods for saving and loading the settings files to the database */
    private void loadAllSettings()
    {
        this.ddnsSettings = loadDynamicDnsSettings();

        try {
            this.networkSettings = loadNetworkSettings();
        } catch ( Exception e ) {
            logger.error( "Error loading network settings, setting to null to be initialized later" );
            this.networkSettings = null;
        }

        /* These must load after the dynamic dns and the network settings in order to initialze
         * the public address. */
        this.remote = loadRemoteSettings();
        
        if ( logger.isDebugEnabled()) logger.debug( "Loaded remote settings: " + this.remote );
    }

    private RemoteInternalSettings loadRemoteSettings()
    {
        RemoteSettings remote = new RemoteSettingsImpl();
        /* These come from files */
        networkConfigurationLoader.loadRemoteSettings( remote );

        NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();
        String pa = nup.getPublicAddress( this.networkSettings, remote, this.ddnsSettings );

        return new RemoteInternalSettings( remote, pa );
    }

    private NetworkSpacesInternalSettings loadNetworkSettings() throws NetworkException, ValidateException
    {
        DataLoader<NetworkSpacesSettingsImpl> loader = 
            new DataLoader<NetworkSpacesSettingsImpl>( "NetworkSpacesSettingsImpl", 
                                                       MvvmContextFactory.context());
        NetworkSpacesSettings dbSettings = loader.loadData();
        
        /* No database settings */
        if ( dbSettings == null ) {
            logger.info( "There are no network database settings" );
            return null;
        }
        
        return NetworkUtilPriv.getPrivInstance().toInternal( dbSettings );
    }

    private DynamicDNSSettings loadDynamicDnsSettings()
    {
        DataLoader<DynamicDNSSettings> loader = 
            new DataLoader<DynamicDNSSettings>( "DynamicDNSSettings", MvvmContextFactory.context());
        
        return loader.loadData();
    }

    private void saveNetworkSettings( NetworkSpacesSettingsImpl newSettings )
        throws NetworkException, ValidateException
    {
        DataSaver<NetworkSpacesSettingsImpl> saver = 
            new DataSaver<NetworkSpacesSettingsImpl>( MvvmContextFactory.context());
        
        /* Have to reuse idz */
        /* !!!!!!! save the id inside of the internal object */
        NetworkSpacesSettings dbSettings = saver.saveData( newSettings );
        
        /* No database settings */
        if ( dbSettings == null ) {
            logger.error( "Unable to save the network settings." );
            return;
        }
        
        this.networkSettings = NetworkUtilPriv.getPrivInstance().toInternal( dbSettings );
    }

    private void saveRemoteSettings()
    {
    }

    private void saveDynamicDnsSettings( DynamicDNSSettings newSettings )
    {
        DataSaver<DynamicDNSSettings> saver = 
            new DataSaver<DynamicDNSSettings>( MvvmContextFactory.context());
        
        /* Have to reuse ids in order to avoid settings proliferation.
         * reusing ids doesn't seem to work (or at least it didn't for ovpn.  have
         * fortunately, hibernate does have a delete method
         * to delete then save. */
        // if ( this.ddnsSettings != null ) newSettings.setId( this.ddnsSettings.getId());

        newSettings = saver.saveData( newSettings );
        if ( newSettings == null ) {
            logger.error( "Unable to save the dynamic dns settings." );
            return;
        }
        
        this.ddnsSettings = newSettings;
    }


    /* Create a networking manager, this is a first come first serve
     * basis.  The first class to create the network manager gets a
     * networking manager, all other classes get AccessException.  Done
     * this way so only the MvvmContextImpl can create a networking manager
     * and then give out access to those classes (argon) that need it.
     * RBS (2/19/06) this is kind of silly, and annoying, switching to getInstance.
     */
    public synchronized static NetworkManagerImpl getInstance()
    {
        if ( INSTANCE != null ) return INSTANCE;

        INSTANCE = new NetworkManagerImpl();
        
        return INSTANCE;
    }
}

