/**
 * $Id$
 */

package com.untangle.uvm;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.http.client.utils.URIBuilder;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;

/**
 * The Manager for system-based url translations
 */
public class UriManagerImpl implements UriManager
{
    String SettingsFileName = "";
    UriManagerSettings settings = null;

    Map<String,String> UriMap = new HashMap<>();

    private final Logger logger = Logger.getLogger(this.getClass());

    /**
     * Constructor
     */
    protected UriManagerImpl()
    {
        this.SettingsFileName = System.getProperty("uvm.conf.dir") + "/" + "uris.js";
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        UriManagerSettings readSettings = null;

        try {
            readSettings = settingsManager.load(UriManagerSettings.class, this.SettingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            // this.setSettings(defaultSettings());
        } else {
            this.settings = readSettings;

            logger.debug("Loading Settings: " + this.settings.toJSONString());
        }
        buildMap();
    }

    /**
     * Get the settings
     * 
     * @return The settings
     */
    public UriManagerSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Set the settings
     * 
     * @param newSettings
     *        The new settings
     */
    public void setSettings(final UriManagerSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(this.SettingsFileName, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
        }
        buildMap();
    }

    /**
     * 
     * @param uri String of url to lookup.
     * @return String of translated url.
     */
    public String getUri(String uri)
    {
        String translatedUri = uri;
        synchronized(this.UriMap){
            translatedUri = UriMap.get(uri);
        }
        return translatedUri;
    }

    /**
     * 
     */
    private void buildMap()
    {
        synchronized(this.UriMap){
            this.UriMap = new HashMap<>();
            URIBuilder uriBuilder = null;
            if(settings.getUriTranslations() != null){
                for(UriTranslation ut : settings.getUriTranslations()){
                    try{
                        uriBuilder = new URIBuilder(ut.getUri());
                        if(ut.getScheme() != null){
                            uriBuilder.setScheme(ut.getScheme());
                        }
                        if(ut.getHost() != null){
                            uriBuilder.setHost(ut.getHost());
                        }
                        if(ut.getPort() != null){
                            uriBuilder.setPort(ut.getPort());
                        }
                        if(ut.getPath() != null){
                            uriBuilder.setPath(ut.getPath());
                        }
                        if(ut.getQuery() != null){
                            uriBuilder.setCustomQuery(ut.getQuery());
                        }
                    }catch(Exception e){
                        logger.warn("*** Unable to create URIBuilder", e);
                    }
                    this.UriMap.put(ut.getUri(), uriBuilder.toString());
                }
            }
        }
    }

}