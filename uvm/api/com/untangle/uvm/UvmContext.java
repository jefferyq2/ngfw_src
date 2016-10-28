/*
 * $Id$
 */
package com.untangle.uvm;

import javax.servlet.http.HttpServletRequest;

import org.jabsorb.JSONSerializer;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.LoggingManager;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.servlet.ServletFileManager;
import com.untangle.uvm.vnet.PipelineFoundry;

/**
 * The top-level untangle-vm API
 */
public interface UvmContext
{
    /**
     * Get the <code>NodeManager</code> singleton.
     *
     * @return the NodeManager.
     */
    NodeManager nodeManager();

    /**
     * Get the <code>LoggingManager</code> singleton.
     *
     * @return the LoggingManager.
     */
    LoggingManager loggingManager();

    /**
     * Get the <code>AdminManager</code> singleton.
     *
     * @return the AdminManager.
     */
    AdminManager adminManager();

    /**
     * Get the <code>SystemManager</code> singleton.
     *
     * @return the SystemManager.
     */
    SystemManager systemManager();
    
    /**
     * Get the <code>NetworkManager</code> singleton.
     *
     * @return the NetworkManager.
     */
    NetworkManager networkManager();
    
    /**
     * Get the <code>ConnectivityTester</code> singleton.
     *
     * @return the ConnectivityTester
     */
    ConnectivityTester getConnectivityTester();

    /**
     * get the <code>MailSender</code> - Used for sending mail
     *
     * @return the MailSender
     */
    MailSender mailSender();

    /**
     * Get the CertificateManager singleton for this instance
     *
     * @return the singleton
     */
    CertificateManager certificateManager();

    /**
     * Get the GeographyManager singleton for this instance
     *
     * @return the singleton
     */
    GeographyManager geographyManager();

    /**
     * Get the DaemonManager singleton for this instance
     *
     * @return the singleton
     */
    DaemonManager daemonManager();

    /**
     * The LocalDirectory for managing/authenticating users
     *
     * @return the local directory
     */
    LocalDirectory localDirectory();
    
    /**
     * The BrandingManager allows for customization of logo and
     * branding information.
     *
     * @return the BrandingManager.
     */
    BrandingManager brandingManager();

    /**
     * Get the <code>SkinManager</code> singleton.
     *
     * @return the SkinManager.
     */
    SkinManager skinManager();

    /**
     * Get the <code>MessageManager</code> singleton.
     *
     * @return the MessageManager
     */
    MetricManager metricManager();

    /**
     * Get the <code>LanguageManager</code> singleton.
     *
     * @return the LanguageManager.
     */
    LanguageManager languageManager();

    /**
     * The license manager.
     *
     * @return the LicenseManager
     */
    LicenseManager licenseManager();

    /**
     * The settings manager.
     * 
     * @return the SettingsManager
     */
    SettingsManager settingsManager();

    /**
     * The certificate cache manager.
     * 
     * @return the CertCacheManager
     */
    CertCacheManager certCacheManager();

    /**
     * Get the DashboardManager
     *
     * @return the DashboardManager
     */
    DashboardManager dashboardManager();

    /**
     * The session monitor
     * This can be used for getting information about current sessions
     *
     * @return the SessionMonitor
     */
    SessionMonitor sessionMonitor();

    /**
     * Get the <code>OemManager</code> singleton.
     *
     * @return the OemManager.
     */
    OemManager oemManager();

    /**
     * Get the <code>AlertManager</code> singleton.
     *
     * @return the AlertManager.
     */
    AlertManager alertManager();

    /**
     * Get the <code>BackupManager</code> singleton.
     *
     * @return the BackupManager.
     */
    BackupManager backupManager();

    /**
     * Get the <code>HookManager</code> singleton.
     *
     * @return the HookManager.
     */
    HookManager hookManager();
    
    /**
     * Get the NetcapManager
     *
     * @return the NetcapManager
     */
    NetcapManager netcapManager();

    /**
     * Get the UploadManager
     *
     * @return the UploadManager
     */
    ServletFileManager servletFileManager();

    /**
     * Get the HTTP Servelet thread request
     */
    InheritableThreadLocal<HttpServletRequest> threadRequest = null;

    /**
     * Get the TomcatManager
     *
     * @return the TomcatManager
     */
    TomcatManager tomcatManager();

    /**
     * get the execManager for launching subprocesses
     */
    ExecManager execManager();

    /**
     * Create a new singleton exec manager
     * This is usual for nodes that need their own exec managers
     * You must call close on the execmanager!
     */
    ExecManager createExecManager();

    /**
     * The host table
     *
     * @return the global host table.
     */
    HostTable hostTable();

    /**
     * The device table
     *
     * @return the global device table.
     */
    DeviceTable deviceTable();
    
    /**
     * Shut down the untangle-vm
     *
     */
    void shutdown();

    /**
     * Reboots the Untangle Server. Note that this currently will not reboot a
     * dev box.
     */
    void rebootBox();

    /**
     * Shutdown the Untangle Server
     */
    void shutdownBox();

    /**
     * Forces NTP to synchronize the time immediately
     * @returns exit code from NTP (0 means success)
     */
    int forceTimeSync();
    
    /**
     * Return the Version
     */
    String version();

    /**
     * Return the Full Version
     */
    String getFullVersion();
    
    /**
     * Return true if running in a development environment.
     */
    boolean isDevel();

    /**
     * Returns true if this is a netbooted install on Untangle internal network
     */
    boolean isNetBoot();
    
    /**
     * Returns the UID of the server
     * Example: aaaa-bbbb-cccc-dddd
     */
    String getServerUID();
    
    /**
     * mark the wizard as complete
     * Called by the setup wizard
     */
    void wizardComplete();
    
    /**
     * Gets the current state of the UVM
     *
     * @return a <code>UvmState</code> enumerated value
     */
    UvmState state();

    /**
     * Run a full System.gc()
     */
    void gc();
    
    /**
     * Thread utilities
     */
    Thread newThread( Runnable runnable );
    Thread newThread( Runnable runnable, String name );

    /**
     * The pipeline compiler.
     * Used by the apps.
     *
     * @return a <code>PipelineFoundry</code> value
     */
    PipelineFoundry pipelineFoundry();

    /**
     * Returns true if the setup wizard has been completed
     *
     * @return a <code>boolean</code> valuett
     */
    boolean isWizardComplete();

    /**
     * Returns true if the system is in "expert mode"
     */
    boolean isExpertMode();
    
    /**
     * Returns the current wizard settings
     * This initializes settings if none exist
     *
     * @return a <code>WizardSettings</code> value
     */
    WizardSettings getWizardSettings();

    /**
     * Sets the wizard settings
     */
    void setWizardSettings( WizardSettings wizardSettings );

    /**
     * Returns true if this server is registered with store account
     *
     * @return a <code>boolean</code> value
     */
    boolean isRegistered();
    void    setRegistered();

    /**
     * Returns true if this server is installed on an official Untangle appliance
     *
     * @return a <code>boolean</code> value
     */
    boolean isAppliance();
    
    /**
     * Returns the appliance model "u50" (if it exists) 
     */
    String getApplianceModel();

    /**
     * Returns true if this server was has an activation code
     *
     * @return a <code>boolean</code> value
     */
    boolean isActivationCode();

    /**
     * blocks until startup is complete
     */
    void waitForStartup();

    /**
     * Get URLs
     */
    String getStoreUrl();
    String getHelpUrl();
    String getLegalUrl();

    /**
     * Check if store is available
     */
    boolean isStoreAvailable();

    /**
     * Convenience method, log an event to the database
     */
    void logEvent(LogEvent evt);

    /**
     * Convenience method to load all the object the webUI needs in one object to avoid
     * multiple calls
     */
    org.json.JSONObject getWebuiStartupInfo();

    /**
     * Get the quick add hints for conditions in reports/events viewer
     */
    org.json.JSONObject getConditionQuickAddHints();
    
    /**
     * Convenience method to load all the object the webUI needs in one object to avoid
     * multiple calls
     */
    org.json.JSONObject getSetupStartupInfo();

    /**
     * Get the global json serializer
     */
    JSONSerializer getSerializer();

}
