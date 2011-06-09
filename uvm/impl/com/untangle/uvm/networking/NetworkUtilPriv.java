/* $HeadURL$ */
package com.untangle.uvm.networking;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.HostNameList;
import com.untangle.uvm.node.IPNullAddr;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.MACAddress;

/* Utilities that are only required inside of this package */
class NetworkUtilPriv extends NetworkUtil
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final NetworkUtilPriv INSTANCE = new NetworkUtilPriv();

    private static final String HOST_NAME_FILE        = "/etc/hostname";

    /* Index of the first network space */
    public static final int SPACE_INDEX_BASE = 0;

    /* Size of an ip addr byte array */
    public static final int IP_ADDR_SIZE_BYTES = 4;

    private NetworkUtilPriv()
    {
    }

    /* Load the network properties, these are used for some of the settings. */
    Properties loadProperties() throws IOException
    {
        String cmd = System.getProperty( "uvm.bin.dir" ) + "/ut-net-properties";
        Process process = LocalUvmContextFactory.context().exec( cmd );
        Properties properties = new Properties();
        properties.load( process.getInputStream());
        return properties;
    }

    List<IPAddress> getDnsServers()
    {
        List<IPAddress> dnsServers = new LinkedList<IPAddress>();

        BufferedReader in = null;

        /* Open up the interfaces file */
        try {
            in = new BufferedReader( new FileReader( NetworkManagerImpl.ETC_RESOLV_FILE ));
            String str;
            while (( str = in.readLine()) != null ) {
                str = str.trim();
                if ( str.startsWith( "nameserver" )) {
                    String server = str.substring( "nameserver".length() ).trim();
                    
                    /* ignore anything that uses the localhost */
                    if ( "127.0.0.1".equals( server )) continue;
                    dnsServers.add( IPAddress.parse( server ));
                }
            }
        } catch ( Exception ex ) {
            logger.error( "Error reading file: ", ex );
        }

        try {
            if ( in != null ) in.close();
        } catch ( Exception ex ) {
            logger.error( "Unable to close file", ex );
        }

        return dnsServers;
    }

    /* Get the hostname of the box from the /etc/hostname file */
    HostName loadHostname()
    {
        HostName hostname = NetworkUtil.DEFAULT_HOSTNAME;

        BufferedReader in = null;

        /* Open up the interfaces file */
        try {
            in = new BufferedReader(new FileReader( HOST_NAME_FILE ));
            String str;
            str = in.readLine().trim();

            /* Try to parse the hostname, throws an exception if it fails */
            hostname = HostName.parse( str );
        } catch ( Exception ex ) {
            /* Go to the default */
            hostname = NetworkUtil.DEFAULT_HOSTNAME;
        }

        try {
            if ( in != null ) in.close();
        } catch ( Exception e ) {
            logger.error( "Error closing file: " + e );
        }

        return hostname;
    }

    static NetworkUtilPriv getPrivInstance()
    {
        return INSTANCE;
    }

    private IPAddress parseIPAddress( Properties properties, String property )
    {
        return parseIPAddress( properties.getProperty( property ));
    }

    private IPAddress parseIPAddress( String value )
    {
        if ( value == null ) return null;
        
        value = value.trim();
        if ( value.length() == 0 ) return null;
        try {
            return IPAddress.parse( value );
        } catch ( ParseException e ) {
            logger.debug( "Unable to parse: '" + value + "'" );
        } catch ( UnknownHostException e ) {
            logger.debug( "Unable to parse: '" + value + "'" );
        }
        
        return null;
    }

    private boolean parseBoolean( Properties properties, String property )
    {
        return Boolean.parseBoolean( properties.getProperty( property ));
    }

    private HostName parseHostname( Properties properties, String property )
    {
        String value = properties.getProperty( property );
        if ( value == null ) return null;

        try {
            return HostName.parse( value );
        } catch ( ParseException e ) {
            logger.warn( "Unable to parse hostname: '" + value + "'", e );
        }
        
        return null;
    }
}
