
/*
 * Utility class for Eextract host name
 */

import java.lang.System.Logger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.logging.LogManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public class HttpUtility {

    // these are used while extracting the SNI from the SSL ClientHello packet
    private static int TLS_HANDSHAKE = 0x16;
    private static int CLIENT_HELLO = 0x01;
    private static int SERVER_NAME = 0x0000;
    private static int HOST_NAME = 0x00;

    private static final HttpUtility INSTANCE = new HttpUtility();
    
    private static final Logger logger = LogManager.getLogger(HttpUtility.class);

    private HttpUtility(){}

    /**
     * Get the global HttpUtility singleton
     * @return the INSTANCE
     */
    public static HttpUtility getInstance()
    {
        return INSTANCE;
    }

    
    /**
     * Function for extracting the SNI hostname from the client request.
     * 
     * @param data
     *        The raw data received from the client
     * @return The SNI hostname extracted from the client request, or null
     * @throw Exception
     */
    public static String extractSniHostname(ByteBuffer data, String appName){
        int counter = 0;
        int pos;
        
        if("CaptivePortal".equals(appName) || "SSLInspector".equals(appName)){
            extractSniForCaptiveAndSSL(data, pos, counter, appName);
        }
        if("WebFilter".equals(appName) || "ThreadPrevention".equals(appName)){
            extractSniForWebAndThreadPrevention(data, pos, counter, appName);
        }
        
    }
    
    /**
     * 
     * @param data
     * @return
     */
    public static String extractSniForCaptiveAndSSL(ByteBuffer data, int pos, int counter, String appName){

        // we use the first byte of the message to determine the protocol
        int recordType = Math.abs(data.get());

        // First check for an SSLv2 hello which Appendix E.2 of RFC 5246
        // says must always set the high bit of the length field
        if ((recordType & 0x80) == 0x80) {
            // skip over the next byte of the length word
            data.position(data.position() + 1);

            // get the message type
            int legacyType = Math.abs(data.get());

            // if not a valid ClientHello we throw an exception since
            // they may be blocking just this kind of invalid traffic
            if (legacyType != CLIENT_HELLO) throw new Exception("Packet contains an invalid SSL handshake");

            // looks like a valid handshake message but the protocol does
            // not support SNI so we just return null
            logger.debug("No SNI available because SSLv2Hello was detected");
            return (null);
        }

        // not SSLv2Hello so proceed with TLS based on the table describe above
        if (recordType != TLS_HANDSHAKE) throw new Exception("Packet does not contain TLS Handshake");
       
        int sslVersion = data.getShort();
        int recordLength = Math.abs(data.getShort());

        // make sure we have a ClientHello message
        int shakeType = Math.abs(data.get());
        if (shakeType != CLIENT_HELLO) throw new Exception("Packet does not contain TLS ClientHello");

        // extract all the handshake data so we can get to the extensions
        int messageExtra = data.get();
        int messageLength = data.getShort();
        int clientVersion = data.getShort();
        int clientTime = data.getInt();

        // skip over the fixed size client random data 
        if (data.remaining() < 28) throw new BufferUnderflowException();
        pos = data.position();
        data.position(pos + 28);
        
        tlsHandshakeSections(data, pos, recordLength);

        // get the total size of extension data block
        int extensionLength = Math.abs(data.getShort());

         // walk through all of the extensions looking for SNI signature
         while (counter < extensionLength) {
            if (data.remaining() < 2 && "SSLInspector".equals(appName)) throw new BufferUnderflowException();
            int extType = Math.abs(data.getShort());
            int extSize = Math.abs(data.getShort());

            // if not server name extension adjust the offset to the next
            // extension record and continue
            if (extType != SERVER_NAME) {
                if (data.remaining() < extSize && "SSLInspector".equals(appName)) throw new BufferUnderflowException();
                data.position(data.position() + extSize);
                counter += (extSize + 4);
                continue;
            }

            // we read the name list info by passing the offset location so we
            // don't modify the position which makes it easier to skip over the
            // whole extension if we bail out during name extraction
            if (data.remaining() < 6 && "SSLInspector".equals(appName)) throw new BufferUnderflowException();
            int listLength = Math.abs(data.getShort(data.position()));
            int nameType = Math.abs(data.get(data.position() + 2));
            int nameLength = Math.abs(data.getShort(data.position() + 3));

            // if we find a name type we don't understand we just abandon
            // processing the rest of the extension
            if (nameType != HOST_NAME) {
                if (data.remaining() < extSize && "SSLInspector".equals(appName)) throw new BufferUnderflowException();
                data.position(data.position() + extSize);
                counter += (extSize + 4);
                continue;
            }
            // found a valid host name so adjust the position to skip over
            // the list length and name type info we directly accessed above
            if (data.remaining() < 5 && "SSLInspector".equals(appName)) throw new BufferUnderflowException();

            extractAndNormalizeHostname(data, nameLength);
        }
        return null;
    }

   public static String extractSniForWebAndThreadPrevention(ByteBuffer data, int pos, int counter, String appName){
         
        logger.debug("Searching for SNI in " + data.toString());

        // make sure we have a TLS handshake message
        int recordType = Math.abs(data.get());

        if (recordType != TLS_HANDSHAKE) {
            logger.debug("First byte is not TLS Handshake signature");
            return (null);
        }
        int sslVersion = data.getShort();
        int recLength = Math.abs(data.getShort());

        // make sure we have a ClientHello message
        int shakeType = Math.abs(data.get());

        if (shakeType != CLIENT_HELLO) {
            logger.debug("Handshake type is not ClientHello");
            return (null);
        }

        // extract all the handshake data so we can get to the extensions
        int messHilen = data.get();
        int messLolen = data.getShort();
        int clientVersion = data.getShort();
        int clientTime = data.getInt();

        tlsHandshakeSections(data, pos, recLength);

        // get the total size of extension data block
        int extensionLength = Math.abs(data.getShort());

        while (counter < extensionLength) {
            if (data.remaining() < 2) throw new BufferUnderflowException();
            int extType = Math.abs(data.getShort());
            int extSize = Math.abs(data.getShort());

            // if not server name extension adjust the offset to the next
            // extension record and continue
            if (extType != SERVER_NAME) {
                if (data.remaining() < extSize) throw new BufferUnderflowException();
                data.position(data.position() + extSize);
                counter += (extSize + 4);
                continue;
            }

            // we read the name list info by passing the offset location so we
            // don't modify the position which makes it easier to skip over the
            // whole extension if we bail out during name extraction
            if (data.remaining() < 6) throw new BufferUnderflowException();
            int listLength = Math.abs(data.getShort(data.position()));
            int nameType = Math.abs(data.get(data.position() + 2));
            int nameLength = Math.abs(data.getShort(data.position() + 3));

            // if we find a name type we don't understand we just abandon
            // processing the rest of the extension
            if (nameType != HOST_NAME) {
                if (data.remaining() < extSize) throw new BufferUnderflowException();
                data.position(data.position() + extSize);
                counter += (extSize + 4);
                continue;
            }
            // found a valid host name so adjust the position to skip over
            // the list length and name type info we directly accessed above
            if (data.remaining() < 5) throw new BufferUnderflowException();
            
            extractAndNormalizeHostname(data, nameLength);
        }
        return null;

    }
    
    public static String tlsHandshakeSections(ByteBuffer data, int pos, int recordLength){
        // skip over the fixed size client random data 
        if (data.remaining() < 28) throw new BufferUnderflowException();
        pos = data.position();
        data.position(pos + 28);
        
        // skip over the variable size session id data
        int sessionLength = Math.abs(data.get());
        if (sessionLength > 0) {
            if (data.remaining() < sessionLength) throw new BufferUnderflowException();
            pos = data.position();
            data.position(pos + sessionLength);
        }

        // skip over the variable size cipher suites data
        int cipherLength = Math.abs(data.getShort());
        if (cipherLength > 0) {
            if (data.remaining() < cipherLength) throw new BufferUnderflowException();
            pos = data.position();
            data.position(pos + cipherLength);
        }

        // skip over the variable size compression methods data
        int compLength = Math.abs(data.get());
        if (compLength > 0) {
            if (data.remaining() < compLength) throw new BufferUnderflowException();
            pos = data.position();
            data.position(pos + compLength);
        }

        // if position equals recordLength plus five we know this is the end
        // of the packet and thus there are no extensions - will normally
        // be equal but we include the greater than just to be safe
        if (data.position() >= (recordLength + 5)){
            logger.debug("No extensions found in TLS handshake message");
            return (null);
        }
        
    }

    public static String extractAndNormalizeHostname(ByteBuffer data, int nameLength){
            data.position(data.position() + 5);
            byte[] hostData = new byte[nameLength];
            data.get(hostData, 0, nameLength);
            String hostName = new String(hostData);
            logger.debug("Extracted SNI hostname = " + hostName);
            return hostName.toLowerCase();
    }

}