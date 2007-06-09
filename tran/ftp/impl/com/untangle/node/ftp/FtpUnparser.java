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

package com.untangle.node.ftp;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.tapi.Fitting;
import com.untangle.uvm.tapi.TCPSession;
import com.untangle.uvm.tapi.event.TCPStreamer;
import com.untangle.node.token.AbstractUnparser;
import com.untangle.node.token.ParseException;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseException;
import com.untangle.node.token.UnparseResult;

class FtpUnparser extends AbstractUnparser
{
    private final byte[] CRLF = new byte[] { 13, 10 };

    public FtpUnparser(TCPSession session, boolean clientSide)
    {
        super(session, clientSide);
    }

    public UnparseResult unparse(Token token) throws UnparseException
    {
        InetSocketAddress socketAddress = null;
        if (token instanceof FtpReply) { // XXX tacky
            FtpReply reply = (FtpReply)token;
            if (FtpReply.PASV == reply.getReplyCode()) {
                try {
                    socketAddress = reply.getSocketAddress();
                } catch (ParseException exn) {
                    throw new UnparseException(exn);
                }
            } 

            /* Extended pasv replies don't contain the server address, have to get that
             * from the session.  NAT/Router is the only place that has that information
             * must register the connection there. */
            else if (FtpReply.EPSV == reply.getReplyCode()) {
                try {
                    socketAddress = reply.getSocketAddress();
                    if (null == socketAddress) 
                        throw new UnparseException("unable to get socket address");
                } catch (ParseException exn) {
                    throw new UnparseException(exn);
                }
                
                /* Nat didn't already rewrite the reply, use the server address */
                InetAddress address = socketAddress.getAddress();
                if ((null == address)||
                    address.getHostAddress().equals("0.0.0.0")) {
                    TCPSession session = getSession();
                    
                    socketAddress = new InetSocketAddress( session.serverAddr(), socketAddress.getPort());
                } /* otherwise use the data from nat */
            }
        } else if (token instanceof FtpCommand) { // XXX tacky
            FtpCommand cmd = (FtpCommand)token;
            if (FtpFunction.PORT == cmd.getFunction()) {
                try {
                    socketAddress = cmd.getSocketAddress();
                } catch (ParseException exn) {
                    throw new UnparseException(exn);
                }
            } else if (FtpFunction.EPRT == cmd.getFunction()) {
                try {
                    socketAddress = cmd.getSocketAddress();                    
                } catch (ParseException exn) {
                    throw new UnparseException(exn);
                }
            }
        }

        if (null != socketAddress) {
            UvmContextFactory.context().pipelineFoundry()
                .registerConnection(socketAddress, Fitting.FTP_DATA_STREAM);
        }

        return new UnparseResult(new ByteBuffer[] { token.getBytes() });
    }

    public TCPStreamer endSession() { return null; }
}
