/**
 * $Id$
 */
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
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
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
        Process process = UvmContextFactory.context().execManager().execEvil( cmd );
        Properties properties = new Properties();
        properties.load( process.getInputStream());
        return properties;
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
}
