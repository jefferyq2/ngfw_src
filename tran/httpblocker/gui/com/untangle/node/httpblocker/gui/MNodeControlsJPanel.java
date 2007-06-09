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

package com.untangle.node.httpblocker.gui;

import javax.swing.JTabbedPane;

import com.untangle.gui.node.*;

public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel{

    private static final String NAME_PASS = "Pass Lists";
    private static final String NAME_PASS_CLIENTS = "Clients";
    private static final String NAME_PASS_URLS = "URLs";
    private static final String NAME_BLOCK = "Block Lists";
    private static final String NAME_BLOCK_FILE_EXTENSIONS = "File Extensions";
    private static final String NAME_BLOCK_MIME_TYPES = "MIME Types";
    private static final String NAME_BLOCK_URLS = "URLs";
    private static final String NAME_BLOCK_CATEGORIES = "Categories";
    private static final String NAME_LOG = "Event Log";


    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel){
        super(mNodeJPanel);
    }

    public void generateGui(){
        // BLOCK LISTS ///////////
        JTabbedPane blockJTabbedPane = addTabbedPane(NAME_BLOCK, null);

        // BLOCKED CATEGORIES /////////
        BlockedCategoriesConfigJPanel blockedCategoriesConfigJPanel = new BlockedCategoriesConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_CATEGORIES, null, blockedCategoriesConfigJPanel);
        addSavable(NAME_BLOCK + " " + NAME_BLOCK_CATEGORIES, blockedCategoriesConfigJPanel);
        addRefreshable(NAME_BLOCK + " " + NAME_BLOCK_CATEGORIES, blockedCategoriesConfigJPanel);
        blockedCategoriesConfigJPanel.setSettingsChangedListener(this);

        // BLOCKED URLS ///////////////
        BlockedURLsConfigJPanel blockedURLsConfigJPanel = new BlockedURLsConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_URLS, null, blockedURLsConfigJPanel);
        addSavable(NAME_BLOCK + " " + NAME_BLOCK_URLS, blockedURLsConfigJPanel);
        addRefreshable(NAME_BLOCK + " " + NAME_BLOCK_URLS, blockedURLsConfigJPanel);
        blockedURLsConfigJPanel.setSettingsChangedListener(this);

        // BLOCKED MIME TYPES ///////////
        BlockedMIMETypesConfigJPanel blockedMIMETypesConfigJPanel = new BlockedMIMETypesConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_MIME_TYPES, null, blockedMIMETypesConfigJPanel);
        addSavable(NAME_BLOCK + " " + NAME_BLOCK_MIME_TYPES, blockedMIMETypesConfigJPanel);
        addRefreshable(NAME_BLOCK + " " + NAME_BLOCK_MIME_TYPES, blockedMIMETypesConfigJPanel);
        blockedMIMETypesConfigJPanel.setSettingsChangedListener(this);

        // BLOCKED FILE EXTENSIONS ///////
        BlockedExtensionsConfigJPanel blockedExtensionsConfigJPanel = new BlockedExtensionsConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_FILE_EXTENSIONS, null, blockedExtensionsConfigJPanel);
        addSavable(NAME_BLOCK + " " + NAME_BLOCK_FILE_EXTENSIONS, blockedExtensionsConfigJPanel);
        addRefreshable(NAME_BLOCK + " " + NAME_BLOCK_FILE_EXTENSIONS, blockedExtensionsConfigJPanel);
        blockedExtensionsConfigJPanel.setSettingsChangedListener(this);

        // PASS LISTS /////
        JTabbedPane passJTabbedPane = addTabbedPane(NAME_PASS, null);

        // PASSED URLS /////////
        PassedURLsConfigJPanel passedURLsConfigJPanel = new PassedURLsConfigJPanel();
        passJTabbedPane.addTab(NAME_PASS_URLS, null, passedURLsConfigJPanel);
        addSavable(NAME_PASS + " " + NAME_PASS_URLS, passedURLsConfigJPanel);
        addRefreshable(NAME_PASS + " " + NAME_PASS_URLS, passedURLsConfigJPanel);
        passedURLsConfigJPanel.setSettingsChangedListener(this);

        // PASSED CLIENTS ///////
        PassedClientsConfigJPanel passedClientsConfigJPanel = new PassedClientsConfigJPanel();
        passJTabbedPane.addTab(NAME_PASS_CLIENTS, null, passedClientsConfigJPanel);
        addSavable(NAME_PASS + " " + NAME_PASS_CLIENTS, passedClientsConfigJPanel);
        addRefreshable(NAME_PASS + " " + NAME_PASS_CLIENTS, passedClientsConfigJPanel);
        passedClientsConfigJPanel.setSettingsChangedListener(this);

        // EVENT LOG ///////
        LogJPanel logJPanel = new LogJPanel(mNodeJPanel.getNode(), this);
        addTab(NAME_LOG, null, logJPanel);
        addShutdownable(NAME_LOG, logJPanel);
    }

}
