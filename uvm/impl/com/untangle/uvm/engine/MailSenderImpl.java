/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.jar.JarFile;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.*;
import javax.mail.internet.*;

import com.sun.mail.smtp.SMTPTransport;
import com.untangle.uvm.MailSender;
import com.untangle.uvm.MailSettings;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.networking.AddressSettingsListener;
import com.untangle.uvm.networking.NetworkManagerImpl;
import com.untangle.uvm.networking.internal.AddressSettingsInternal;
import com.untangle.uvm.security.AdminSettings;
import com.untangle.uvm.security.User;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.util.ConfigFileUtil;
import com.untangle.uvm.util.TransactionRunner;
import com.untangle.uvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SessionFactory;


/**
 * Note that this class is designed to be used <b>BOTH</b> inside the UVM and
 * as a stand-alone application. The stand-alone mode is used for mailing out
 * Untangle Reports.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
class MailSenderImpl implements MailSender
{
    // All error log emails go here.
    public static final String ERROR_LOG_RECIPIENT = "exceptions@untangle.com";

    public static final String UNTANGLE_SMTP_RELAY = "mail.untangle.com";

    public static final String Mailer = "UVM MailSender";

    public static final String TEST_MESSAGE_SUBJECT = "Untangle Server test message";
    public static final String TEST_MESSAGE_BODY =
        "Success!\r\n\r\n" +
        "This automated message was generated by the Untangle Server\r\n" +
        "to confirm that your email settings are correct.\r\n\r\n";

    // --

    // JavaMail constants
    private static final String MAIL_HOST_PROP = "mail.host";
    private static final String MAIL_ENVELOPE_FROM_PROP = "mail.smtp.from";
    private static final String MAIL_FROM_PROP = "mail.from";
    private static final String MAIL_TRANSPORT_PROTO_PROP = "mail.transport.protocol";

    private static final String BUNNICULA_BASE = System.getProperty( "bunnicula.home" );

    // exim conf
    private static final String EXIM_CMD_RESTART  = "/etc/init.d/exim4 restart";
    private static final String EXIM_CMD_UPDATE_CONF  = "/usr/sbin/update-exim4.conf";
    private static final String EXIM_CONF_DIR     = "/etc/exim4";
    private static final String EXIM_CONF_FILE    = "/etc/exim4/update-exim4.conf.conf";
    private static final String EXIM_TEMPLATE_FILE    = "/etc/exim4/exim4.conf.template";
    private static final String EXIM_AUTH_FILE    = "/etc/exim4/passwd.client";

    private static final String EXIM_CONF_START =
        "# AUTOGENERATED BY UNTANGLE DO NOT MODIFY MANUALLY\n\n" +
        "dc_local_interfaces='127.0.0.1'\n" + 
        "dc_relay_domains=''\n" + 
        "dc_minimaldns='false'\n" + 
        "dc_relay_nets=''\n" + 
        "CFILEMODE='644'\n" + 
        "dc_use_split_config='false'\n" + 
        "dc_hide_mailname='true'\n" + 
        "dc_mailname_in_oh='true'\n";

    private static final Object LOCK = new Object();

    private static MailSenderImpl MAIL_SENDER;

    public static boolean SessionDebug = false;

    private static final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    private MailSettings mailSettings;

    // This is the session used to send alert mail inside the organization, including
    // SMTP spam/virus notification.
    private Session alertSession;

    // This is the session used to send report mail inside the organization
    private Session reportSession;

    // This is the session used to send mail to Untangle, Inc.
    private Session utSession;

    private final Logger logger = Logger.getLogger(getClass());

    // NOTE: Only used for stand-alone operation.
    private final TransactionRunner transactionRunner;

    private MailSenderImpl()
    {
        transactionRunner = null;
        init();
    }

    private MailSenderImpl(org.hibernate.SessionFactory sessionFactory)
    {
        transactionRunner = new TransactionRunner(sessionFactory);
        init();
    }

    private void init()
    {
        mimetypesFileTypeMap.addMimeTypes("application/pdf pdf PDF");
        mimetypesFileTypeMap.addMimeTypes("text/css css CSS");

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(org.hibernate.Session s)
                {
                    Query q = s.createQuery("from MailSettings");
                    mailSettings = (MailSettings)q.uniqueResult();

                    if (null == mailSettings) {
                        logger.info("Creating initial default mail settings");
                        mailSettings = new MailSettings();
                        mailSettings.setFromAddress(MailSender.DEFAULT_FROM_ADDRESS);
                        s.save(mailSettings);
                    }
                    return true;
                }

                public Object getResult() { return null; }
            };
        runTransaction(tw);

        // This is safe to do early, and it allows exception emails early.
        refreshSessions();
    }

    // Called from UvmContextImpl at postInit time, after the networking manager
    // is up and runing
    void postInit() {
        reconfigure();
        ((NetworkManagerImpl)LocalUvmContextFactory.context().networkManager()).
            registerListener(new AddressSettingsListener() {
                    public void event( AddressSettingsInternal settings )
                    {
                        reconfigure();
                    }
                });
    }


    static MailSenderImpl mailSender() {
        synchronized (LOCK) {
            if (null == MAIL_SENDER) {
                MAIL_SENDER = new MailSenderImpl();
            }
        }
        return MAIL_SENDER;
    }

    private boolean runTransaction(TransactionWork tw)
    {
        if (null == transactionRunner) {
            return LocalUvmContextFactory.context().runTransaction(tw);
        } else {
            return transactionRunner.runTransaction(tw);
        }
    }

    public void setMailSettings(final MailSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(org.hibernate.Session s)
                {
                    s.merge(settings);
                    return true;
                }

                public Object getResult() { return null; }
            };
        runTransaction(tw);

        mailSettings = settings;
        reconfigure();
    }

    public MailSettings getMailSettings()
    {
        return mailSettings;
    }

    // Writes out the new exim config files when the mali settings change.
    private void writeConfiguration()
    {
        File exim_dir = new File(EXIM_CONF_DIR);
        if (exim_dir.isDirectory()) {

            String hostName = LocalUvmContextFactory.context().networkManager().
                getAddressSettingsInternal().getHostName().toString();
            if (hostName == null) {
                logger.warn("null hostname, using untangle-server");
                hostName = "untangle-server";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(EXIM_CONF_START);

            sb.append("dc_other_hostnames='");
            sb.append(hostName);
            sb.append("'\n");

            sb.append("dc_readhost='");
            sb.append(hostName);
            sb.append("'\n");
            
            if ( mailSettings.isUseMxRecords() ) {
                /**
                 * Send mail directly using MX Records
                 */
                sb.append("dc_eximconfig_configtype='");
                sb.append("internet");
                sb.append("'\n");

                sb.append("dc_smarthost=''\n");
            } else {
                /**
                 * Send mail to smarthost
                 */
                sb.append("dc_eximconfig_configtype='");
                sb.append("satellite");
                sb.append("'\n");

                sb.append("dc_smarthost='");
                sb.append(mailSettings.getSmtpHost());
                sb.append("'\n");

                /**
                 * write auth file
                 */
                String user = mailSettings.getAuthUser();
                if ("".equals(user)) user = null;
                String pass = mailSettings.getAuthPass();
                if ("".equals(pass)) pass = null;
                
                if ((user == null && pass != null) || (user != null && pass == null)) {
                    logger.warn("SMTP AUTH user/pass -- only one set, ignoring");
                    user = null;
                    pass = null;
                }
                else if (user != null) {
                    StringBuilder sbpasswd = new StringBuilder();
                    sbpasswd.append(mailSettings.getSmtpHost());
                    sbpasswd.append(":");
                    sbpasswd.append(user);
                    sbpasswd.append(":");
                    sbpasswd.append(pass);
                    sbpasswd.append("\n");
                    ConfigFileUtil.writeFile( sbpasswd, EXIM_AUTH_FILE );
                }
                else if (user == null && pass == null) {
                    StringBuilder blank = new StringBuilder();
                    blank.append("");
                    ConfigFileUtil.writeFile( blank, EXIM_AUTH_FILE );
                }
                    

                /**
                 * Substitute the port in the exim template
                 */
                try {
                    String line;
                    Process proc;
                    BufferedReader input;
                    // remove all "  port = xx"
                    String cmd1[] = {"/bin/sed","-e","/..port.=.[0-9].*$/d","-i",EXIM_TEMPLATE_FILE};
                    // insert new "  port = xx"
                    String cmd2[] = {"/bin/sed","-e","s|  driver = smtp|  driver = smtp\\n  port = " + mailSettings.getSmtpPort() + "|g","-i",EXIM_TEMPLATE_FILE};

                    proc = LocalUvmContextFactory.context().exec(cmd1);
                    input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    while ((line = input.readLine()) != null) {logger.warn(line);}
                    proc.destroy();
                    
                    proc = LocalUvmContextFactory.context().exec(cmd2);
                    input  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    while ((line = input.readLine()) != null) {logger.warn(line);}
                    proc.destroy();
                }
                catch (java.io.IOException e) {
                    logger.error( "Unable to save Mail Daemon configuration", e );
                }
            }
            
            ConfigFileUtil.writeFile( sb, EXIM_CONF_FILE );

            try {
                ScriptRunner.getInstance().exec( EXIM_CMD_UPDATE_CONF );
            }
            catch (com.untangle.uvm.node.NodeException e) {
                logger.error( "Unable to save Mail Daemon configuration", e );
            }

        }
    }

    void restartMailDaemon() {
        File exim_dir = new File(EXIM_CONF_DIR);
        if (exim_dir.isDirectory()) {

            /* Run the script */
            try {
                ScriptRunner.getInstance().exec( "/etc/init.d/exim4","restart" );
            } catch ( Exception e ) {
                logger.error( "Unable to reload Mail Daemon configuration", e );
            }
        }
    }

    private void reconfigure() {
        refreshSessions();
        writeConfiguration();
        restartMailDaemon();
    }

    // Called when settings updated.
    private void refreshSessions() {
        Properties commonProps = new Properties();
        commonProps.put(MAIL_FROM_PROP, mailSettings.getFromAddress());
        commonProps.put(MAIL_ENVELOPE_FROM_PROP, mailSettings.getFromAddress());
        commonProps.put(MAIL_TRANSPORT_PROTO_PROP, "smtp");

        File exim_dir = new File(EXIM_CONF_DIR);
        if (exim_dir.isDirectory()) {
            commonProps.put(MAIL_HOST_PROP, "localhost");
        } else if ( !mailSettings.isUseMxRecords() ) {
            commonProps.put(MAIL_HOST_PROP, mailSettings.getSmtpHost());
        }

        Properties mvProps = (Properties) commonProps.clone();
        // This one always uses our SMTP host.
        mvProps.put(MAIL_HOST_PROP, UNTANGLE_SMTP_RELAY);
        // What we really want here is something that will uniquely identify the customer,
        // including the serial # stamped on the CD. XXXX
        utSession = Session.getInstance(mvProps);

        Properties alertProps = (Properties) commonProps.clone();
        alertSession = Session.getInstance(alertProps);

        Properties reportProps = (Properties) commonProps.clone();
        reportSession = Session.getInstance(reportProps);
    }

    private static final String[] RECIPIENTS_PROTO = new String[0];

    public void sendAlert(String subject, String bodyText) {
        sendAlertWithAttachment(subject, bodyText, null);
    }

    private void sendAlertWithAttachment(String subject, String bodyText, List attachment) {
        // Compute the list of recipients from the user list.
        AdminSettings adminSettings = LocalUvmContextFactory.context()
            .adminManager().getAdminSettings();
        Set users = adminSettings.getUsers();
        List alertableUsers = new ArrayList();
        for (Iterator iter = users.iterator(); iter.hasNext();) {
            User user = (User) iter.next();
            String userEmail = user.getEmail();
            if (userEmail == null) {
                if (logger.isDebugEnabled())
                    logger.debug("Skipping user " + user.getLogin()
                                 + " with no email address");
            } else {
                if (!user.getSendAlerts())
                    logger.debug("Skipping user " + user.getLogin()
                                 + " with sendAlerts off");
                else
                    alertableUsers.add(userEmail);
            }
        }

        String[] recipients = (String[]) alertableUsers.toArray(RECIPIENTS_PROTO);
        if (recipients.length == 0) {
            logger.warn("Not sending alert email, no recipients");
        } else if (attachment == null) {
            sendSimple(alertSession, recipients, subject, bodyText, null);
        } else {
            MimeBodyPart part = makeAttachmentFromList(attachment);
            sendSimple(alertSession, recipients, subject, bodyText, part);
        }
    }

    private MimeBodyPart makeAttachmentFromList(List list) {
        if (list == null) return null;

        int bodySize = 0;
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            ByteBuffer buf = (ByteBuffer) iter.next();
            bodySize += buf.remaining();
        }
        byte[] text = new byte[bodySize];
        int pos = 0;
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            ByteBuffer buf = (ByteBuffer) iter.next();
            int size = buf.remaining();
            buf.get(text, pos, size);
            pos += size;
        }

        try {
            MimeBodyPart attachment = new MimeBodyPart(new InternetHeaders(), text);
            return attachment;
        } catch (MessagingException x) {
            logger.error("Unable to make attachment", x);
            return null;
        }
    }

    // Not currently used
    /*
      public void sendReport(String subject, String bodyText) {
      String reportEmailAddr = mailSettings.getReportEmail();
      if (reportEmailAddr == null) {
      logger.info("Not sending report email, no address");
      } else {
      String[] recipients = new String[1];
      recipients[0] = reportEmailAddr;
      sendSimple(reportSession, recipients, subject, bodyText, null);
      }
      }
    */

    public void sendReports(String subject, String bodyHTML, List<String> extraLocations, List<File> extras) {
        String reportEmailAddr = mailSettings.getReportEmail();
        if (reportEmailAddr == null) {
            logger.info("Not sending report email, no address");
        } else {
            String[] recipients = new String[1];
            recipients[0] = reportEmailAddr;

            sendMessageWithAttachments(recipients, subject, bodyHTML, extraLocations, extras);
        }
    }

    public void sendMessageWithAttachments(String[] recipients, String subject, String bodyHTML, List<String> extraLocations, List<File> extras) {
        if (extraLocations == null && extras == null) {
            // Do this simplest thing.  Shouldn't be used. XX
            sendSimple(reportSession, recipients, subject, bodyHTML, null);
            return;
        } else if ((extraLocations == null && extras != null) ||
                   (extraLocations != null && extras == null) ||
                   (extraLocations.size() != extras.size())) {
            throw new IllegalArgumentException("sendReports mismatch of locations and extras");
        }

        List<MimeBodyPart> parts = new ArrayList<MimeBodyPart>();

        try {
            for (int i = 0; i < extras.size(); i++) {
                String location = extraLocations.get(i);
                File extra = extras.get(i);
                DataSource ds = new FileDataSource(extra);
                ((FileDataSource)ds).setFileTypeMap( mimetypesFileTypeMap );
                DataHandler dh = new DataHandler(ds);
                MimeBodyPart part = new MimeBodyPart();
                part.setDataHandler(dh);
                part.setHeader("Content-Location", location);
                part.setFileName(extra.getName());
                parts.add(part);
            }
        } catch (MessagingException x) {
            logger.error("Unable to parse extras", x);
            return;
        }

        sendRelated(reportSession, recipients, subject, bodyHTML, parts);
    }

    public void sendErrorLogs(String subject, String bodyText, List<MimeBodyPart> parts)
    {
        String[] recipients = new String[1];
        recipients[0] = ERROR_LOG_RECIPIENT;

        // New behavior 8/15/05: First try our own mail server, if that doesn't work
        // (they have a firewall rule preventing it, for instance), try their email
        // server.  If that doesn't work, go ahead and drop it.  jdi
        Session[] trySessions = new Session[] { utSession, alertSession };
        boolean success = false;
        for (Session session : trySessions) {
            if (parts == null) {
                // Do this simplest thing.  Shouldn't be used. XX
                success = sendSimple(session, recipients, subject, bodyText, null);
            } else {
                success = sendMixed(session, recipients, subject, bodyText, parts);
            }
            if (success)
                break;
        }
        if (!success) {
            logger.error("Unable to send exception email, dropping");
        }
    }

    public void sendMessage(String[] recipients, String subject, String bodyText)
    {
        sendMessageWithAttachment(recipients, subject, bodyText, null);
    }

    private void sendMessageWithAttachment(String[] recipients, String subject, String bodyText, List attachment)
    {
        if (attachment == null) {
            sendSimple(alertSession, recipients, subject, bodyText, null);
        } else {
            MimeBodyPart part = makeAttachmentFromList(attachment);
            sendSimple(alertSession, recipients, subject, bodyText, part);
        }
    }

    /*
     * See doc on interface
     */
    public boolean sendMessage(InputStream msgStream) {
        //TODO bscott Need better error handling
        //TODO bscott by using JavaMail, we don't seem to be able to have
        //     a null ("<>") MAIL FROM.  This is a violation of some spec
        //     or another, which declares that the envelope from should
        //     be blank for notifications (so other servers don't send
        //     dead letters causing a loop).

        MimeMessage msg = streamToMIMEMessage(msgStream);

        if(msg == null) {
            return false;
        }

        //Send the message
        try {
            dosend(reportSession, msg);
            logIt(msg);
            return true;
        }
        catch(Exception ex) {
            logger.warn("Unable to send Message", ex);
            return false;
        }
    }

    /*
     * See doc on interface
     */
    public boolean sendMessage(InputStream msgStream, String...rcptStrs) {

        //First, convert the addresses
        Address[] addresses = parseAddresses(rcptStrs);
        if(addresses == null || addresses.length == 0) {
            logger.warn("No recipients for email");
            return false;
        }

        //TODO bscott Need better error handling
        //TODO bscott by using JavaMail, we don't seem to be able to have
        //     a null ("<>") MAIL FROM.  This is a violation of some spec
        //     or another, which declares that the envelope from should
        //     be blank for notifications (so other servers don't send
        //     dead letters causing a loop).

        MimeMessage msg = streamToMIMEMessage(msgStream);

        if(msg == null) {
            return false;
        }

        //Send the message
        try {
            dosend(reportSession, msg, addresses);
            logIt(msg);
            return true;
        }
        catch(Exception ex) {
            logger.warn("Unable to send Message", ex);
            return false;
        }
    }


    // Here's where we actually do the sending
    public boolean sendTestMessage(String recipient)
    {
        return sendSimple(alertSession, new String[] { recipient },
                          TEST_MESSAGE_SUBJECT, TEST_MESSAGE_BODY, null);
    }


    /**
     * Returns null if message could not be created, and logs
     * any errors
     */
    private MimeMessage streamToMIMEMessage(InputStream in) {
        try {
            MimeMessage msg = new MimeMessage(alertSession, in);
            msg.setHeader("X-Mailer", Mailer);
            return msg;
        }
        catch(Exception ex) {
            logger.error("Unable to convert input stream to MIMEMessage", ex);
            return null;
        }
    }

    private Address[] parseAddresses(String[] addrStrings) {
        List<Address> ret = new ArrayList<Address>();
        for(String s : addrStrings) {
            try {
                for(Address addr : InternetAddress.parse(s, false)) {
                    InternetAddress inetAddr = (InternetAddress) addr;
                    if(inetAddr.getAddress() != null &&
                       !"".equals(inetAddr.getAddress()) &&
                       !"<>".equals(inetAddr.getAddress())) {
                        ret.add(inetAddr);
                    }
                }
            }
            catch(Exception ex) {
                logger.warn("Unable to parse \"" + s + "\" into email address");
            }
        }
        return (Address[]) ret.toArray(new Address[ret.size()]);
    }


    private Message prepMessage(Session session, String[] to, String subject)
    {
        Message msg = new MimeMessage(session);

        // come up with all recipients
        Address[][] addrs = new Address[to.length][];
        for (int i = 0; i < to.length; i++) {
            try {
                addrs[i] = InternetAddress.parse(to[i], false);
            } catch (AddressException x) {
                logger.error("Failed to parse receipient address " + to[i] + ", ignoring");
                addrs[i] = null;
            }
        }
        int addrCount = 0;
        for (int i = 0; i < addrs.length; i++)
            if (addrs[i] != null)
                addrCount += addrs[i].length;
        if (addrCount == 0) {
            logger.warn("No recipients for email, ignoring");
            return null;
        }

        Address[] recipients = new Address[addrCount];
        for (int i = 0, c = 0; i < addrs.length; i++) {
            if (addrs[i] != null) {
                for (int j = 0; j < addrs[i].length; j++)
                    recipients[c++] = addrs[i][j];
            }
        }

        try {
            msg.setRecipients(Message.RecipientType.TO, recipients);
            // msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc, false));
            // msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc, false));
            msg.setFrom();
            msg.setSubject(subject);
            msg.setHeader("X-Mailer", Mailer);
            msg.setSentDate(new Date());
            return msg;
        } catch (MessagingException x) {
            logger.error("Unable to send message", x);
            return null;
        }
    }

    private void logIt(Message msg)
        throws MessagingException
    {
        if (logger.isInfoEnabled()) {
            StringBuffer sb = new StringBuffer("Successfully sent message '");
            sb.append(msg.getSubject());
            sb.append("' to ");
            Address[] peeps = msg.getAllRecipients();
            if (peeps == null || peeps.length == 0) {
                sb.append(" nobody!");
            } else {
                sb.append(peeps.length);
                sb.append(" recipients (");
                for (int i = 0; i < peeps.length; i++) {
                    if (i > 0)
                        sb.append(", ");
                    sb.append(peeps[i]);
                }
                sb.append(")");
            }
            logger.info(sb.toString());
        }
    }

    boolean sendSimple(Session session, String[] to, String subject,
                       String bodyText, MimeBodyPart attachment)
    {
        if (SessionDebug)
            session.setDebug(true);

        // construct the message
        Message msg = prepMessage(session, to, subject);
        if (msg == null)
            // Nevermind after all.
            return true;

        try {
            if (attachment == null) {
                msg.setText(bodyText);
            } else {
                MimeBodyPart main = new MimeBodyPart();
                main.setText(bodyText);
                main.setDisposition(Part.INLINE);
                Multipart mp = new MimeMultipart();
                mp.addBodyPart(main);
                mp.addBodyPart(attachment);
                msg.setContent(mp);
            }

            // send it
            dosend(session, msg);
            logIt(msg);
            return true;
        } catch (MessagingException x) {
            logger.warn("Unable to send message", x);
            return false;
        }
    }

    boolean sendRelated(Session session, String[] to, String subject,
                        String bodyHTML, List<MimeBodyPart> extras)
    {
        if (SessionDebug)
            session.setDebug(true);

        // construct the message
        Message msg = prepMessage(session, to, subject);
        if (msg == null)
            // Nevermind after all.
            return true;

        try {
            Multipart mp = new MimeMultipart("related");
            MimeBodyPart main = new MimeBodyPart();
            main.setContent(bodyHTML, "text/html");
            // main.setDisposition(Part.INLINE);
            mp.addBodyPart(main);
            for (MimeBodyPart part : extras)
                mp.addBodyPart(part);
            msg.setContent(mp);

            // send it
            dosend(session, msg);
            logIt(msg);
            return true;
        } catch (MessagingException x) {
            logger.warn("Unable to send message", x);
            return false;
        }
    }

    boolean sendMixed(Session session, String[] to, String subject,
                      String bodyText, List<MimeBodyPart> extras)
    {
        if (SessionDebug)
            session.setDebug(true);

        // construct the message
        Message msg = prepMessage(session, to, subject);
        if (msg == null)
            // Nevermind after all.
            return true;

        try {
            Multipart mp = new MimeMultipart("mixed");
            MimeBodyPart main = new MimeBodyPart();
            main.setText(bodyText);
            // main.setDisposition(Part.INLINE);
            mp.addBodyPart(main);
            for (MimeBodyPart part : extras)
                mp.addBodyPart(part);
            msg.setContent(mp);

            // send it
            dosend(session, msg);
            logIt(msg);
            return true;
        } catch (MessagingException x) {
            logger.warn("Unable to send message", x);
            return false;
        }
    }

    // --

    // Here's where we actually do the sending
    private void dosend(Session session, Message msg)
        throws MessagingException
    {
        dosend(session, msg, msg.getAllRecipients());
    }

    // Here's where we actually do the sending
    private void dosend(Session session, Message msg, Address[] recipients)
        throws MessagingException
    {
        SMTPTransport transport = null;
        try {
            transport = (SMTPTransport) session.getTransport();
            // We get the host from the session since it can differ (mv errors).
            String host = session.getProperty(MAIL_HOST_PROP);

            File exim_dir = new File(EXIM_CONF_DIR);
            if (exim_dir.isDirectory()) {
                transport.connect(host, null, null);
            } else {
                int port = mailSettings.getSmtpPort();
                String user = mailSettings.getAuthUser();
                String pass = mailSettings.getAuthPass();
                if ((user == null && pass != null) || (user != null && pass == null)) {
                    logger.warn("SMTP AUTH user/pass -- only one set, ignoring");
                    user = null;
                    pass = null;
                }
                transport.setStartTLS(mailSettings.isUseTls());
                transport.setLocalHost(mailSettings.getLocalHostName());
                transport.connect(host, port, user, pass);
            }
            transport.sendMessage(msg, recipients);
        } catch (MessagingException x) {
            throw x;
        } catch (Exception x) {
            // Uh oh...
            logger.error("Unexpected exception in dosend", x);
            throw new MessagingException("Unexpected exception in dosend", x);
        } finally {
            try { if (transport != null) transport.close(); } catch (MessagingException x) { }
        }
    }

    // --

    private static void usage() {
        System.err.println("usage: mail-reports [-s subject] bodyhtmlfile { extrafile }");
        System.exit(1);
    }

    public static void main(String[] args)
    {
        String subject = "Untangle Reports";
        File uvmClassDir = null;
        File bodyFile = null;
        List<File> extraFiles = new ArrayList<File>();
        List<String> extraLocations = new ArrayList<String>();

        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-s")) {
                    subject = args[++i];
                } else if (uvmClassDir == null) {
                    uvmClassDir = new File(args[i]);
                } else if (bodyFile == null) {
                    bodyFile = new File(args[i]);
                } else {
                    // For now just use the name plus two parent directories as the content-location
                    String extraLocation;
                    File extraFile = new File(args[i]);
                    File parentFile = extraFile.getParentFile();
                    if (parentFile == null) {
                        extraLocation = extraFile.getName();
                    } else {
                        File grandParentFile = parentFile.getParentFile();
                        // XXX super ugly fixme XXX
                        if ((parentFile.getName().equals("images") && !extraFile.getName().endsWith(".png")) ||
                            grandParentFile == null) {
                            extraLocation = parentFile.getName() + File.separator + extraFile.getName();
                        } else {
                            extraLocation = grandParentFile.getName() + File.separator + parentFile.getName() + File.separator + extraFile.getName();
                        }
                    }

                    extraLocations.add(extraLocation);
                    extraFiles.add(extraFile);
                }
            }
            if (uvmClassDir == null || bodyFile == null)
                usage();

            List<File> cds = new ArrayList<File>();
            cds.add(uvmClassDir);
            org.hibernate.SessionFactory sessionFactory = Util.makeStandaloneSessionFactory(cds, null);
            MailSenderImpl us = new MailSenderImpl(sessionFactory);

            // Read in the body file.
            FileReader frmain = new FileReader(bodyFile);
            StringBuilder sbmain = new StringBuilder();
            char[] buf = new char[1024];
            int rs;
            while ((rs = frmain.read(buf)) > 0)
                sbmain.append(buf, 0, rs);
            frmain.close();
            String bodyHTML = sbmain.toString();

            us.sendReports(subject, bodyHTML, extraLocations, extraFiles);
        } catch (IOException x) {
            System.err.println("Unable to send message" + x);
            x.printStackTrace();
            System.exit(2);
        }

    }
}
