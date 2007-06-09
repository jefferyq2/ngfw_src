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

package com.untangle.node.openvpn.gui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.node.openvpn.*;

public class ServerRoutingWizard extends MWizardJDialog {

    private static final String MESSAGE_DIALOG_TITLE = "Setup Wizard Warning";
    private static final String MESSAGE_CLIENT_NOT_CONFIGURED = "You have not finished configuring OpenVPN.  Please run the Setup Wizard again.";

    private MNodeControlsJPanel mNodeControlsJPanel;

    public static ServerRoutingWizard factory(Window topLevelWindow, VpnNode vpnNode,
                                              MNodeControlsJPanel mNodeControlsJPanel) {
        if( topLevelWindow instanceof Frame )
            return new ServerRoutingWizard((Frame)topLevelWindow, vpnNode, mNodeControlsJPanel);
        else if( topLevelWindow instanceof Dialog )
            return new ServerRoutingWizard((Dialog)topLevelWindow, vpnNode, mNodeControlsJPanel);
        else
            return null;
    }

    public ServerRoutingWizard(Frame topLevelFrame, VpnNode vpnNode, MNodeControlsJPanel mNodeControlsJPanel) {
        super(topLevelFrame, true);
        init(mNodeControlsJPanel, vpnNode);
    }

    public ServerRoutingWizard(Dialog topLevelDialog, VpnNode vpnNode, MNodeControlsJPanel mNodeControlsJPanel) {
        super(topLevelDialog, true);
        init(mNodeControlsJPanel, vpnNode);
    }

    private void init(MNodeControlsJPanel mNodeControlsJPanel, VpnNode vpnNode){
        this.mNodeControlsJPanel = mNodeControlsJPanel;
        setTitle("Untangle OpenVPN Server Routing Setup Wizard");
        addWizardPageJPanel(new ServerRoutingWizardWelcomeJPanel(vpnNode),         "1. Welcome", false, true);
        addWizardPageJPanel(new ServerRoutingWizardCertificateJPanel(vpnNode),     "2. Generate Certificate", false, true);
        addWizardPageJPanel(new ServerRoutingWizardGroupsJPanel(vpnNode),          "3. Add Address Pools", false, true);
        addWizardPageJPanel(new ServerRoutingWizardExportsJPanel(vpnNode),         "4. Add Exports", false, true);
        addWizardPageJPanel(new ServerRoutingWizardClientsJPanel(vpnNode),         "5. Add VPN Clients", false, true);
        addWizardPageJPanel(new ServerRoutingWizardSitesJPanel(vpnNode),           "6. Add VPN Sites", false, true);
        addWizardPageJPanel(new ServerRoutingWizardCongratulationsJPanel(vpnNode), "7. Congratulations", true, true);
    }

    protected Dimension getTitleJPanelPreferredSize(){ return new Dimension(250,360); }
    protected Dimension getContentJPanelPreferredSize(){ return new Dimension(485,360); }

    protected void wizardFinishedAbnormal(int currentPage){
        if( currentPage <= 5 ){
            MOneButtonJDialog.factory(this, "", MESSAGE_CLIENT_NOT_CONFIGURED, MESSAGE_DIALOG_TITLE, "");
            super.wizardFinishedAbnormal(currentPage);
        }
        else
            this.wizardFinishedNormal();
    }

    protected void wizardFinishedNormal(){
        super.wizardFinishedNormal();
        mNodeControlsJPanel.getInfiniteProgressJComponent().startLater("Reconfiguring...");
        mNodeControlsJPanel.refreshGui();
        mNodeControlsJPanel.getInfiniteProgressJComponent().stopLater(0l);
    }
}


