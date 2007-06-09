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

package com.untangle.node.mail.gui;

import java.awt.Component;
import java.util.Arrays;
import java.util.List;

import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.util.Util;
import com.untangle.node.mail.papi.MailNode;
import com.untangle.node.mail.papi.MailNodeSettings;
import com.untangle.node.mail.papi.quarantine.Inbox;
import com.untangle.node.mail.papi.quarantine.InboxIndex;
import com.untangle.node.mail.papi.quarantine.QuarantineMaintenenceView;
import com.untangle.node.mail.papi.quarantine.QuarantineSettings;
import com.untangle.node.mail.papi.safelist.SafelistAdminView;

public class MailNodeCompoundSettings implements CompoundSettings {

    public static final String GLOBAL_BUSINESS_PAPERS = "GLOBAL";

    // MAIL NODE SETTINGS //
    private MailNodeSettings mailNodeSettings;
    public MailNodeSettings getMailNodeSettings(){ return mailNodeSettings; }
    private MailNode mailNode;

    // GENERAL SETTINGS //
    private Component generalSettingsComponent;
    public Component getGeneralSettingsComponent(){ return generalSettingsComponent; }
    private long minStorageGigs;
    public long getMinStorageGigs(){ return minStorageGigs; }
    private long maxStorageGigs;
    public long getMaxStorageGigs(){ return maxStorageGigs; }

    // QUARANTINE SETTINGS //
    private QuarantineSettings quarantineSettings;
    public QuarantineSettings getQuarantineSettings(){ return quarantineSettings; }

    // QUARANTINE MAINTENANCE VIEW //
    private QuarantineMaintenenceView quarantineMaintenanceView;
    public QuarantineMaintenenceView getQuarantineMaintenanceView(){ return quarantineMaintenanceView; }

    // GLOBAL SAFELIST //
    private List<String> globalSafelist;
    public List<String> getGlobalSafelist(){ return globalSafelist; }
    public void setGlobalSafelist(List<String> inGlobalSafelist){
        globalSafelist = inGlobalSafelist;
    }


    // SAFELIST ADMIN VIEW //
    private SafelistAdminView safelistAdminView;
    public SafelistAdminView getSafelistAdminView(){ return safelistAdminView; }
    private String[] safelistContents;
    private List<String> safelists;
    private int[] safelistCounts;
    private InboxIndex inboxIndex;
    private List<Inbox> inboxList;
    public String[] getSafelistContents(){ return safelistContents; }
    public List<String> getSafelists(){ return safelists; }
    public int[] getSafelistCounts(){ return safelistCounts; }
    public List<Inbox> getInboxList() {
        return inboxList;
    }
    public InboxIndex getInboxIndex(){
        return inboxIndex;
    }
    public void loadSafelists() throws Exception {
        safelists = safelistAdminView.listSafelists();
    }
    public void loadSafelistCounts() throws Exception {
        safelistCounts = new int[safelists.size()];
        int i=0;
        for(String account : safelists){
            safelistCounts[i] = safelistAdminView.getSafelistCnt(account);
            i++;
        }
    }
    public void loadSafelistContents(String account) throws Exception { safelistContents = safelistAdminView.getSafelistContents(account); }
    public void loadInboxIndex(String account) throws Exception { inboxIndex = quarantineMaintenanceView.getInboxIndex(account); }
    public void loadInboxList() throws Exception { inboxList = quarantineMaintenanceView.listInboxes(); }

    // QUARANTINE SAFELIST CONTROLS //////
    private Component safelistComponent;
    public Component getSafelistComponent(){ return safelistComponent; }

    // QUARANTINE RELEASE & PURGE //
    private Component quarantineReleaseAndPurgeComponent;
    public Component getQuarantineReleaseAndPurgeComponent(){ return quarantineReleaseAndPurgeComponent; }

    // QUARANTINABLE ADDRESSES //
    private Component quarantinableAddressesComponent;
    public Component getQuarantinableAddressesComponent(){ return quarantinableAddressesComponent; }

    // QUARANTINABLE FORWARDS //
    private Component quarantinableForwardsComponent;
    public Component getQuarantinableForwardsComponent(){ return quarantinableForwardsComponent; }

    // QUARANTINE GENERAL SETTINGS //
    private Component quarantineGeneralSettingsComponent;
    public Component getQuarantineGeneralSettingsComponent(){ return quarantineGeneralSettingsComponent; }



    public void save() throws Exception {
        mailNodeSettings.setQuarantineSettings(quarantineSettings);
        mailNode.setMailNodeSettings(mailNodeSettings);

        // GLOBAL STUFF //
        safelistAdminView.replaceSafelist(GLOBAL_BUSINESS_PAPERS, globalSafelist.toArray(new String[0]));
    }

    public void refresh() throws Exception {
        if(mailNode == null)
            mailNode = (MailNode) Util.getNode("mail-casing");
        mailNodeSettings = mailNode.getMailNodeSettings();
        quarantineSettings = mailNodeSettings.getQuarantineSettings();

        // GENERAL SETTINGS //
        minStorageGigs = mailNode.getMinAllocatedStoreSize(true);
        maxStorageGigs = mailNode.getMaxAllocatedStoreSize(true);

        if(quarantineMaintenanceView == null)
            quarantineMaintenanceView = mailNode.getQuarantineMaintenenceView();
        if(safelistAdminView == null)
            safelistAdminView = mailNode.getSafelistAdminView();

        // GLOBAL STUFF //
        globalSafelist = Arrays.asList( safelistAdminView.getSafelistContents(GLOBAL_BUSINESS_PAPERS) );

        if(generalSettingsComponent == null)
            generalSettingsComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.MCasingJPanel", "mail-casing");
        if(safelistComponent == null)
            safelistComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.WhitelistAllUsersJPanel", "mail-casing");
        if(quarantineReleaseAndPurgeComponent == null)
            quarantineReleaseAndPurgeComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.QuarantineAllUsersJPanel", "mail-casing");
        if(quarantinableAddressesComponent == null)
            quarantinableAddressesComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.QuarantinableAddressesJPanel", "mail-casing");
        if(quarantinableForwardsComponent == null)
            quarantinableForwardsComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.QuarantinableForwardsJPanel", "mail-casing");
        if(quarantineGeneralSettingsComponent == null)
            quarantineGeneralSettingsComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.QuarantineGeneralSettingsJPanel", "mail-casing");
    }

    public void validate() throws Exception {

    }

}
