/**
 * $Id: GoogleManagerImpl.java 41234 2015-09-12 00:47:13Z dmorris $
 */
package com.untangle.uvm;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.UvmContextFactory;

/**
 * GoogleManagerImpl provides the API implementation of all RADIUS related functionality
 */
public class GoogleManagerImpl implements GoogleManager
{
    private static final String GOOGLE_DRIVE_PATH = "/var/lib/google-drive/";
    private static final String GOOGLE_DRIVE_TMP_PATH = "/tmp/google-drive/";
    
    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * This is just a copy of the current settings being used
     */
    private GoogleSettings settings;

    /**
     * These hold the proc, reader, and writer for the drive process if it is active
     */
    private Process            driveProc = null;
    private OutputStreamWriter driveProcOut = null;
    private BufferedReader     driveProcIn  = null;
    
    /**
     * Initialize Google authenticator.
     */
    protected GoogleManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        GoogleSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "google.js";
        try {
            readSettings = settingsManager.load( GoogleSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            GoogleSettings newSettings = new GoogleSettings();
            this.setSettings(newSettings);
        }
        else {
            logger.debug("Loading Settings...");
            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        logger.info("Initialized GoogleManager");        
    }

    /**
     * Get Google Authenticator settings.
     *
     * @return Google autenticator settings
     */
    public GoogleSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Configure Google authenticator settings.
     *
     * @param settings  Google authenticator settings.
     */
    public void setSettings( GoogleSettings settings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "google.js", settings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        this.settings = settings;

        if ( !isGoogleDriveConnected() ) {
            try {
                File creds = new File(GOOGLE_DRIVE_PATH + ".gd/credentials.json");
                if ( creds.exists() )
                    creds.delete();
            } catch (Exception ex) {
                logger.warn("Error deleting credentials.json.", ex);
            }
        }
    }

    /**
     * Determine if Google drive is connection.
     *
     * @return true if Google drive is configured, false otherwise.
     */
    public boolean isGoogleDriveConnected()
    {
        String token = settings.getDriveRefreshToken();

        if ( token == null )
            return false;
        token = token.replaceAll("\\s+", StringUtils.EMPTY);
        return !token.isEmpty();
    }

    /**
     * This returns the URL that the user should visit and click allow for the google connector app to be authorized.
     * Once the user clicks the allow button, they will be redirected to Untangle with the redirect_url. The untangle redirect_url
     * will redirect them to their local server gdrive servlet (the IP is passed in the state variable).
     * The servlet will later call provideDriveCode() with the token
     *
     * @param windowProtocol TCP/IP protocol to use.
     * @param windowLocation domain/hostname
     * @return Built URL
     */
    public String getAuthorizationUrl( String windowProtocol, String windowLocation )
    {
        startAuthorizationProcess(GOOGLE_DRIVE_TMP_PATH);
        
        if (driveProcIn == null || driveProcOut == null || driveProc == null) {
            throw new RuntimeException("Authorization process not running.");
        }

        if (windowProtocol == null || windowLocation == null) {
            throw new RuntimeException("Invalid arguments." + windowProtocol + " " + windowLocation);
        }
        
        try {
            String line;
            while ((line=driveProcIn.readLine())!=null) {
                logger.info("drive parsing line: " + line);
                if (!line.contains("https"))
                    continue;

                URIBuilder builder = new URIBuilder(line);
                String state = windowProtocol + "//" + windowLocation + "/" + "gdrive" + "/" + "gdrive";
                builder.setParameter("state",state);
                builder.setParameter("approval_prompt","force");

                logger.info("Providing authorization URL: " + builder.toString());
                stopAuthorizationProcess();
                return builder.toString();
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to parse drive output.",e);
            return null;
        }
    }

    /**
     * Returns spp configuration of the google drive connector app
     * @return GoogleCloudApp instance
     */
    public GoogleCloudApp getGoogleCloudApp() {
        String appId = UvmContextFactory.context().execManager().execOutput(System.getProperty("uvm.bin.dir") + "/ut-google-drive-helper.sh appId " + GOOGLE_DRIVE_PATH);
        String clientId = UvmContextFactory.context().execManager().execOutput(System.getProperty("uvm.bin.dir") + "/ut-google-drive-helper.sh clientId " + GOOGLE_DRIVE_PATH);
        String clientSecret = UvmContextFactory.context().execManager().execOutput(System.getProperty("uvm.bin.dir") + "/ut-google-drive-helper.sh clientSecret " + GOOGLE_DRIVE_PATH);
        String scopes = UvmContextFactory.context().execManager().execOutput(System.getProperty("uvm.bin.dir") + "/ut-google-drive-helper.sh scopes " + GOOGLE_DRIVE_PATH);
        String redirectUrl = UvmContextFactory.context().execManager().execOutput(System.getProperty("uvm.bin.dir") + "/ut-google-drive-helper.sh redirectUrl " + GOOGLE_DRIVE_PATH);

        return new GoogleCloudApp(appId, clientId, clientSecret, scopes, redirectUrl);
    }

    /**
     * This launches the google drive command line app and provides the code
     * The google drive app will then fetch and save the refreshToken for future use.
     *
     * This also reads the refershToken and saves it in settings.
     *
     * @param code the code to send.
     * @return null on success or the error string
     */
    public String provideDriveCode( String code )
    {
        logger.info("Providing code [" + code + "] to drive");
        startAuthorizationProcess(GOOGLE_DRIVE_PATH);
        
        try {
            driveProcOut.write(code);
            driveProcOut.write("\n");
            driveProcOut.flush();
            driveProcOut.close();
        } catch (Exception e) {
            logger.error("Failed to write code to drive.",e);
            return e.toString();
        }

        /* wait up until ten seconds for drive to create credentials.json */
        String refreshToken = null;
        for ( int i = 0; i < 10 ; i++ ) {
            try { Thread.sleep(1000); } catch (Exception e) {}

            logger.info("Checking for refresh token...");
            refreshToken = UvmContextFactory.context().execManager().execOutput(System.getProperty("uvm.bin.dir") + "/ut-google-drive-helper.sh refreshToken " + GOOGLE_DRIVE_PATH);
            if ( refreshToken == null )
                continue;
            refreshToken = refreshToken.replaceAll("\\s+","");
            if (refreshToken.isEmpty())
                continue;
            break;
        }

        /**
         * save the settings with the refresh token
         */
        if ( StringUtils.isNotEmpty(refreshToken)) {
            refreshToken = refreshToken.replaceAll("\\s+",StringUtils.EMPTY);
            logger.info("Refresh Token: {}", refreshToken);
        
            GoogleSettings googleSettings = getSettings();
            googleSettings.setDriveRefreshToken( refreshToken );
            setSettings( googleSettings );
        } else {
            logger.warn("Unable to parse refreshToken");
            return "Unable to parse refresh_token";
        }

        stopAuthorizationProcess();
        return null;
    }

    /**
     * Disconnect Google drive
     */
    public void disconnectGoogleDrive()
    {
        GoogleSettings googleSettings = getSettings();
        googleSettings.setDriveRefreshToken( null );
        setSettings( googleSettings );
    }

    /**
     * Called by Directory Connector to migrate the existing configuration from
     * there to here now that Google Drive support has moved to the base system.
     *
     * @param refreshToken - The refresh token
     */
    public void migrateConfiguration(String refreshToken)
    {
        GoogleSettings googleSettings = getSettings();
        googleSettings.setDriveRefreshToken( refreshToken );
        setSettings( googleSettings );
    }

    /**
     * Start Google authorization process
     * 
     * @param dir Directory to use
     */
    private void startAuthorizationProcess( String dir )
    {
        if (driveProcIn != null || driveProcOut != null || driveProc != null) {
            logger.warn("Shutting down previously running drive.");
            stopAuthorizationProcess();
        }

        try {
            logger.info("Launching drive...");
            ProcessBuilder builder = new ProcessBuilder("/bin/sh","-c","mkdir -p " + dir + " ; cd " + dir + " ; /usr/bin/drive init");
            builder.redirectErrorStream(true);
            driveProc = builder.start();

            //driveProc = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            logger.error("Couldn't start drive", e);
            return;
        }

        driveProcOut = new OutputStreamWriter(driveProc.getOutputStream());
        driveProcIn  = new BufferedReader(new InputStreamReader(driveProc.getInputStream()));
    }

    /**
     * Stop the authorization process.
     */
    private void stopAuthorizationProcess()
    {
        try { driveProcIn.close(); } catch (Exception ex) { }
        try { driveProcOut.close(); } catch (Exception ex) { }
        try { driveProc.destroy(); } catch (Exception ex) { }
        driveProcIn = null;
        driveProcOut = null;
        driveProc = null;
    }
}
