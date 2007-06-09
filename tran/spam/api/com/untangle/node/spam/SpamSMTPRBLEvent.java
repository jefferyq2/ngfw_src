/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.spam;

import java.net.InetAddress;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.PipelineEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.PipelineEndpoints;
import org.hibernate.annotations.Type;

/**
 * Log for Spam SMTP RBL events.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="tr_spam_smtp_rbl_evt", schema="events")
    public class SpamSMTPRBLEvent extends PipelineEvent
    {
        private HostName hostname;
        private IPaddr ipAddr;
        private boolean skipped;

        // constructors -----------------------------------------------------------

        public SpamSMTPRBLEvent() {}

        public SpamSMTPRBLEvent(PipelineEndpoints plEndp, HostName hostname, IPaddr ipAddr, boolean skipped) {
            super(plEndp);
            this.hostname = hostname;
            this.ipAddr = ipAddr;
            this.skipped = skipped;
        }

        public SpamSMTPRBLEvent(PipelineEndpoints plEndp, String hostnameS, InetAddress ipAddrIN, boolean skipped) {
            super(plEndp);
            try {
                this.hostname = HostName.parse(hostnameS);
            } catch (ParseException e) {
                this.hostname = HostName.getEmptyHostName();
            }
            this.ipAddr = new IPaddr(ipAddrIN);
            this.skipped = skipped;
        }

        // accessors --------------------------------------------------------------

        /**
         * Hostname of RBL service.
         *
         * @return hostname of RBL service.
         */
        @Column(nullable=false)
        @Type(type="com.untangle.uvm.type.HostNameUserType")
        public HostName getHostname() {
            return hostname;
        }

        public void setHostname(HostName hostname) {
            this.hostname = hostname;
            return;
        }

        /**
         * IP address of mail server listed on RBL service.
         *
         * @return IP address of mail server listed on RBL service.
         */
        @Column(nullable=false)
        @Type(type="com.untangle.uvm.type.IPaddrUserType")
        public IPaddr getIPAddr() {
            return ipAddr;
        }

        public void setIPAddr(IPaddr ipAddr) {
            this.ipAddr = ipAddr;
            return;
        }

        /**
         * Confirmed RBL hit but skipping rejection indicator.
         *
         * @return confirmed RBL hit but skipping rejection indicator.
         */
        @Column(nullable=false)
        public boolean getSkipped() {
            return skipped;
        }

        public void setSkipped(boolean skipped) {
            this.skipped = skipped;
            return;
        }

        // Syslog methods ---------------------------------------------------------

        @Transient
        public void appendSyslog(SyslogBuilder sb)
        {
            // No longer log pipeline endpoints, they are not necessary anyway.
            // PipelineEndpoints pe = getPipelineEndpoints();
            /* unable to log this event */
            // pe.appendSyslog(sb);

            sb.startSection("info");
            sb.addField("hostname", getHostname().toString());
            sb.addField("ipaddr", getIPAddr().toString());
            sb.addField("skipped", getSkipped());
        }

        @Transient
        public String getSyslogId()
        {
            return "SMTP_RBL";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            // INFORMATIONAL = statistics or normal operation
            // WARNING = traffic altered
            return false == getSkipped() ? SyslogPriority.INFORMATIONAL : SyslogPriority.WARNING; // traffic altered
        }
    }
