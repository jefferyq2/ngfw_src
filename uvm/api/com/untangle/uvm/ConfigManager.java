/**
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.network.InterfaceSettings;

import java.util.List;
import org.json.JSONObject;

/**
 * Manager for Configuration
 */
public interface ConfigManager
{
    Object getSystemInfo();

    Object getHostName();
    Object setHostName(String argName);

    Object getDomainName();
    Object setDomainName(String argName);

    Object doFactoryReset();

    Object doFirmwareUpdate();

    Object checkFirmwareUpdate();

    Object getDeviceTime();
    Object setDeviceTime(String argTime, String argZone);

    Object setAdminCredentials(String argUsername, String argPassword);

    List<InterfaceSettings> getNetworkInterfaces();
    Object setNetworkInterfaces(List<InterfaceSettings> faceList);

    Object getSyslogServer();
    Object setSyslogServer(boolean argEnabled, String argHost, int argPort, String argProtocol);

    Object getNetworkPortStats();

    Object getTrafficMetrics();

    SnmpSettings getSnmpSettings();
    Object setSnmpSettings(SnmpSettings argSettings);

    Object createSystemBackup();

    Object restoreSystembackup(Object argBackup);

    Object getNotifications();

    Object getDiagnosticInfo();
}
