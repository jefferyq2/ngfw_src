/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * This is the implementation of the captive portal settings.
 */

@SuppressWarnings("serial")
public class CaptivePortalSettings implements Serializable
{
    public static enum AuthenticationType
    {
        NONE, LOCAL_DIRECTORY, RADIUS, ACTIVE_DIRECTORY, ANY_DIRCON, GOOGLE, FACEBOOK, MICROSOFT, ANY_OAUTH, CUSTOM, ANY
    };

    public static enum PageType
    {
        BASIC_LOGIN, BASIC_MESSAGE, CUSTOM
    };

    public static enum CertificateDetection
    {
        DISABLE_DETECTION, CHECK_CERTIFICATE, REQUIRE_CERTIFICATE
    };

    private List<CaptureRule> captureRules = new LinkedList<CaptureRule>();
    private List<PassedAddress> passedClients = new LinkedList<PassedAddress>();
    private List<PassedAddress> passedServers = new LinkedList<PassedAddress>();

    private AuthenticationType authenticationType = AuthenticationType.NONE;
    private int idleTimeout = 0;
    private int userTimeout = 3600;
    private boolean areConcurrentLoginsEnabled = true;
    private boolean alwaysUseSecureCapture = false;
    private boolean sessionCookiesEnabled = false;
    private boolean redirectUsingHostname = false;
    private boolean disableSecureRedirect = false;
    private boolean useMacAddress = false;
    private int sessionCookiesTimeout = 3600 * 24;
    private PageType pageType = PageType.BASIC_MESSAGE;
    private String customFileName = "";
    private String redirectUrl = "";

    private String basicLoginPageTitle = "";
    private String basicLoginPageWelcome = "";
    private String basicLoginUsername = "";
    private String basicLoginPassword = "";
    private String basicLoginMessageText = "";
    private String basicLoginFooter = "";

    private String basicMessagePageTitle = "";
    private String basicMessagePageWelcome = "";
    private String basicMessageMessageText = "";
    private boolean basicMessageAgreeBox = false;
    private String basicMessageAgreeText = "";
    private String basicMessageFooter = "";
    private CertificateDetection certificateDetection = CertificateDetection.DISABLE_DETECTION;

    private Boolean checkServerCertificate = null;

    private String secretKey;
    private byte[] binaryKey;

    public CaptivePortalSettings()
    {
    }

    public List<CaptureRule> getCaptureRules()
    {
        if (this.captureRules == null) {
            this.captureRules = new LinkedList<CaptureRule>();
        }

        return this.captureRules;
    }

    public void setCaptureRules(List<CaptureRule> newValue)
    {
        this.captureRules = newValue;
    }

    public List<PassedAddress> getPassedClients()
    {
        return this.passedClients;
    }

    public void setPassedClients(List<PassedAddress> newValue)
    {
        this.passedClients = newValue;
    }

    public List<PassedAddress> getPassedServers()
    {
        return this.passedServers;
    }

    public void setPassedServers(List<PassedAddress> newValue)
    {
        this.passedServers = newValue;
    }

    public AuthenticationType getAuthenticationType()
    {
        return this.authenticationType;
    }

    public void setAuthenticationType(AuthenticationType newValue)
    {
        this.authenticationType = newValue;
    }

    public int getIdleTimeout()
    {
        return this.idleTimeout;
    }

    public void setIdleTimeout(int newValue)
    {
        this.idleTimeout = newValue;
    }

    public int getUserTimeout()
    {
        return this.userTimeout;
    }

    public void setUserTimeout(int newValue)
    {
        this.userTimeout = newValue;
    }

    public boolean getUseMacAddress()
    {
        return this.useMacAddress;
    }

    public void setUseMacAddress(boolean newValue)
    {
        this.useMacAddress = newValue;
    }

    public boolean getConcurrentLoginsEnabled()
    {
        return this.areConcurrentLoginsEnabled;
    }

    public void setConcurrentLoginsEnabled(boolean newValue)
    {
        this.areConcurrentLoginsEnabled = newValue;
    }

    public boolean getAlwaysUseSecureCapture()
    {
        return this.alwaysUseSecureCapture;
    }

    public void setAlwaysUseSecureCapture(boolean newValue)
    {
        this.alwaysUseSecureCapture = newValue;
    }

    public boolean getSessionCookiesEnabled()
    {
        return this.sessionCookiesEnabled;
    }

    public void setSessionCookiesEnabled(boolean newValue)
    {
        this.sessionCookiesEnabled = newValue;
    }

    public boolean getRedirectUsingHostname()
    {
        return this.redirectUsingHostname;
    }

    public void setRedirectUsingHostname(boolean newValue)
    {
        this.redirectUsingHostname = newValue;
    }

    public boolean getDisableSecureRedirect()
    {
        return this.disableSecureRedirect;
    }

    public void setDisableSecureRedirect(boolean newValue)
    {
        this.disableSecureRedirect = newValue;
    }

    public int getSessionCookiesTimeout()
    {
        return this.sessionCookiesTimeout;
    }

    public void setSessionCookiesTimeout(int newValue)
    {
        this.sessionCookiesTimeout = newValue;
    }

    public PageType getPageType()
    {
        return this.pageType;
    }

    public void setPageType(PageType newValue)
    {
        this.pageType = newValue;
    }

    public String getRedirectUrl()
    {
        return this.redirectUrl;
    }

    public void setRedirectUrl(String newValue)
    {
        this.redirectUrl = newValue;
    }

    public String getCustomFilename()
    {
        return this.customFileName;
    }

    public void setCustomFilename(String newValue)
    {
        this.customFileName = newValue;
    }

    public String getBasicLoginPageTitle()
    {
        return this.basicLoginPageTitle;
    }

    public String getBasicLoginPageWelcome()
    {
        return this.basicLoginPageWelcome;
    }

    public String getBasicLoginUsername()
    {
        return this.basicLoginUsername;
    }

    public String getBasicLoginPassword()
    {
        return this.basicLoginPassword;
    }

    public String getBasicLoginMessageText()
    {
        return this.basicLoginMessageText;
    }

    public String getBasicLoginFooter()
    {
        return this.basicLoginFooter;
    }

    public String getBasicMessagePageTitle()
    {
        return this.basicMessagePageTitle;
    }

    public String getBasicMessagePageWelcome()
    {
        return this.basicMessagePageWelcome;
    }

    public String getBasicMessageMessageText()
    {
        return this.basicMessageMessageText;
    }

    public boolean getBasicMessageAgreeBox()
    {
        return this.basicMessageAgreeBox;
    }

    public String getBasicMessageAgreeText()
    {
        return this.basicMessageAgreeText;
    }

    public String getBasicMessageFooter()
    {
        return this.basicMessageFooter;
    }

    public CertificateDetection getCertificateDetection()
    {
        return this.certificateDetection;
    }

    public Boolean getCheckServerCertificate()
    {
        return this.checkServerCertificate;
    }

    public void setBasicLoginPageTitle(String newValue)
    {
        this.basicLoginPageTitle = newValue;
    }

    public void setBasicLoginPageWelcome(String newValue)
    {
        this.basicLoginPageWelcome = newValue;
    }

    public void setBasicLoginUsername(String newValue)
    {
        this.basicLoginUsername = newValue;
    }

    public void setBasicLoginPassword(String newValue)
    {
        this.basicLoginPassword = newValue;
    }

    public void setBasicLoginMessageText(String newValue)
    {
        this.basicLoginMessageText = newValue;
    }

    public void setBasicLoginFooter(String newValue)
    {
        this.basicLoginFooter = newValue;
    }

    public void setBasicMessagePageTitle(String newValue)
    {
        this.basicMessagePageTitle = newValue;
    }

    public void setBasicMessagePageWelcome(String newValue)
    {
        this.basicMessagePageWelcome = newValue;
    }

    public void setBasicMessageMessageText(String newValue)
    {
        this.basicMessageMessageText = newValue;
    }

    public void setBasicMessageAgreeBox(boolean newValue)
    {
        this.basicMessageAgreeBox = newValue;
    }

    public void setBasicMessageAgreeText(String newValue)
    {
        this.basicMessageAgreeText = newValue;
    }

    public void setBasicMessageFooter(String newValue)
    {
        this.basicMessageFooter = newValue;
    }

    public void setCertificateDetection(CertificateDetection newValue)
    {
        this.certificateDetection = newValue;
    }

    public void setCheckServerCertificate(Boolean newValue)
    {
        this.checkServerCertificate = newValue;
    }

    public String getSecretKey()
    {
        return (secretKey);
    }

    public void setSecretKey(String key)
    {
        secretKey = key;
    }

    public void initBinaryKey(byte[] key)
    {
        // first we save the argumented key in our binary version
        binaryKey = key;

        // now we generate the string version from the binary version
        StringBuilder local = new StringBuilder();

        for (int x = 0; x < key.length; x++) {
            char lo_nib = (char) ((key[x] & 0x0F) + 'A');
            char hi_nib = (char) (((key[x] >> 4) & 0x0F) + 'A');
            local.append(lo_nib);
            local.append(hi_nib);
        }

        setSecretKey(local.toString());
    }
}
