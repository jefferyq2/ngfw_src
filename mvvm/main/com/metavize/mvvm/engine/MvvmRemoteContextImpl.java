/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.io.IOException;

import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.ToolboxManager;
import com.metavize.mvvm.client.MvvmRemoteContext;
import com.metavize.mvvm.logging.LoggingManager;
import com.metavize.mvvm.security.AdminManager;
import com.metavize.mvvm.tran.TransformManager;

class MvvmRemoteContextImpl implements MvvmRemoteContext
{
    private final MvvmLocalContext context;

    // constructors -----------------------------------------------------------

    MvvmRemoteContextImpl(MvvmLocalContext context)
    {
        this.context = context;
    }

    // MvvmRemoteContext methods ----------------------------------------------

    public ToolboxManager toolboxManager()
    {
        return context.toolboxManager();
    }

    public TransformManager transformManager()
    {
        return context.transformManager();
    }

    public LoggingManager loggingManager()
    {
        return context.loggingManager();
    }

    public AdminManager adminManager()
    {
        return context.adminManager();
    }

    public ArgonManager argonManager()
    {
        return context.argonManager();
    }

    public NetworkingManager networkingManager()
    {
        return context.networkingManager();
    }

    public void localBackup() throws IOException
    {
        context.localBackup();
    }

    public void usbBackup() throws IOException
    {
        context.usbBackup();
    }

    public void shutdown()
    {
        context.shutdown();
    }

    public void doFullGC()
    {
        context.doFullGC();
    }

}
