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
import java.awt.Window;
import java.awt.event.*;
import javax.swing.CellEditor;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.node.openvpn.*;

public class KeyButtonRunnable implements ButtonRunnable {
    private boolean isEnabled;
    private VpnClientBase vpnClient;
    private static VpnNode vpnNode;
    private Window topLevelWindow;
    public KeyButtonRunnable(String isEnabled){
        if( "true".equals(isEnabled) ) {
            this.isEnabled = true;
        }
        else if( "false".equals(isEnabled) ){
            this.isEnabled = false;
        }
    }
    public String getButtonText(){ return "Distribute Client"; }
    public boolean isEnabled(){ return isEnabled; }
    public void setEnabled(boolean isEnabled){ this.isEnabled = isEnabled; }
    public boolean valueChanged(){ return false; }
    public void setVpnClient(VpnClientBase vpnClient){ this.vpnClient = vpnClient; }
    public static void setVpnNode(VpnNode vpnNodeX){ vpnNode = vpnNodeX; }
    public void setCellEditor(CellEditor cellEditor){}
    public void setTopLevelWindow(Window topLevelWindow){ this.topLevelWindow = topLevelWindow; }
    public void actionPerformed(ActionEvent evt){ run(); }
    public void run(){
        if(!Util.getIsDemo())
            new DistributeKeyThread(vpnClient, vpnNode);
    }

    class DistributeKeyThread extends Thread {
        private KeyJDialog keyJDialog;
        private VpnClientBase vpnClient;
        private VpnNode vpnNode;
        public DistributeKeyThread(VpnClientBase vpnClient, VpnNode vpnNode){
            setDaemon(true);
            setName("MV-CLIENT: DistributeKeyThread");
            this.vpnClient = vpnClient;
            this.vpnNode = vpnNode;
            keyJDialog = KeyJDialog.factory(topLevelWindow, vpnClient);
            keyJDialog.setVisible(true);
            if( keyJDialog.isProceeding() )
                start();
        }
        public void run(){
            if( keyJDialog.isUsbSelected() ){
                vpnClient.setDistributeUsb(true);
            }
            else if( keyJDialog.isEmailSelected() ){
                vpnClient.setDistributeUsb(false);
                vpnClient.setDistributionEmail( keyJDialog.getEmailAddress() );
            }
            try{
                vpnNode.distributeClientConfig( vpnClient );
                String successString;
                if( vpnClient.getDistributeUsb() )
                    successString = "<html>OpenVPN successfully saved your digital key to your USB key.</html>";
                else
                    successString = "<html>OpenVPN successfully sent your digital key via email.</html>";

                MOneButtonJDialog.factory(topLevelWindow, "OpenVPN", successString, "OpenVPN Confirmation", "Confirmation" );
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error saving/sending key:", e);
                String warningString;
                if( vpnClient.getDistributeUsb() )
                    warningString = "<html>OpenVPN was not able to save your digital key to your USB key.  Please try again.</html>";
                else
                    warningString = "<html>OpenVPN was not able to send your digital key via email.  Please try again.</html>";
                MOneButtonJDialog.factory(topLevelWindow, "OpenVPN", warningString, "OpenVPN Warning", "Warning" );
            }
        }
    }
}
