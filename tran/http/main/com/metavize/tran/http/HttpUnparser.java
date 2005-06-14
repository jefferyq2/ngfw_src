/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.http;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractUnparser;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Header;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenStreamer;
import com.metavize.tran.token.UnparseResult;
import com.metavize.tran.token.Unparser;
import org.apache.log4j.Logger;

class HttpUnparser extends AbstractUnparser
{
    private static final ByteBuffer[] BYTE_BUFFER_PROTO = new ByteBuffer[0];

    // XXX move to util class/interface
    private static final byte[] LAST_CHUNK = "0\r\n\r\n".getBytes();
    private static final byte[] CRLF = "\r\n".getBytes();
    private static final byte[] COLON_SPACE = ": ".getBytes();

    private static final int BUFFER_SIZE = 4096;

    private static final int CLOSE_ENCODING = 0;
    private static final int CONTENT_LENGTH_ENCODING = 1;
    private static final int CHUNKED_ENCODING = 2;

    private final Logger logger = Logger.getLogger(HttpUnparser.class);

    // used to keep request with header, IIS requires this
    private final Queue outputQueue = new LinkedList();
    private final HttpCasing httpCasing;

    private int size = 0;
    private int transferEncoding;
    private String sessStr;

    HttpUnparser(TCPSession session, boolean clientSide,
                    HttpCasing httpCasing)
    {
        super(session, clientSide);
        this.httpCasing = httpCasing;
    }

    public TokenStreamer endSession() { return null; }

    public UnparseResult unparse(Token token)
    {
        logger.debug(sessStr + " got unparse event node: " + token);

        if (token instanceof StatusLine) {
            logger.debug(sessStr + " got status line!");
            transferEncoding = CLOSE_ENCODING;
            return statusLine((StatusLine)token);
        } else if (token instanceof RequestLine) {
            logger.debug(sessStr + " got request line!");
            return requestLine((RequestLine)token);
        } else if (token instanceof Header) {
            logger.debug(sessStr + " got header!");
            return header((Header)token);
        } else if (token instanceof Chunk) {
            logger.debug(sessStr + " got chunk!");
            return chunk((Chunk)token);
        } else if (token instanceof EndMarker) {
            logger.debug(sessStr + " got endmarker");
            return endMarker();
        } else {
            throw new IllegalArgumentException("unexpected: " + token);
        }
    }

    private UnparseResult statusLine(StatusLine s)
    {
        logger.debug(sessStr + " status-line");

        queueOutput(s.getBytes());

        return new UnparseResult(BYTE_BUFFER_PROTO);
    }

    private UnparseResult requestLine(RequestLine rl)
    {
        logger.debug(sessStr + " request-line");
        ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

        HttpMethod method = rl.getMethod();
        logger.debug(sessStr + " Unparser got method: " + method);

        httpCasing.queueRequest(rl);

        queueOutput(rl.getBytes());

        return new UnparseResult(BYTE_BUFFER_PROTO);
    }

    private UnparseResult header(Header h)
    {
        logger.debug(sessStr + " header");

        String encoding = h.getValue("transfer-encoding");
        if (null != encoding && encoding.equalsIgnoreCase("chunked")) {
            transferEncoding = CHUNKED_ENCODING;
        } else if (null != h.getValue("content-length")) {
            transferEncoding = CONTENT_LENGTH_ENCODING;
        }

        queueOutput(h.getBytes());
        if (isClientSide()) {
            return dequeueOutput();
        } else {
            return new UnparseResult(BYTE_BUFFER_PROTO);
        }
    }

    private UnparseResult chunk(Chunk c)
    {
        logger.debug(sessStr + " chunk");

        ByteBuffer cBuf = c.getBytes();

        if (CHUNKED_ENCODING == transferEncoding && 0 == cBuf.remaining()) {
            return new UnparseResult(BYTE_BUFFER_PROTO);
        }

        ByteBuffer buf;

        switch (transferEncoding) {
        case CLOSE_ENCODING:
        case CONTENT_LENGTH_ENCODING:
            buf = cBuf;
            break;
        case CHUNKED_ENCODING:
            buf = ByteBuffer.allocate(cBuf.remaining() + 32);
            String hexLen = Integer.toHexString(cBuf.remaining());
            buf.put(hexLen.getBytes());
            buf.put(CRLF);
            buf.put(cBuf);
            buf.put(CRLF);
            buf.flip();
            break;
        default:
            throw new IllegalStateException
                ("transferEncoding: " + transferEncoding);
        }

        if (outputQueue.isEmpty()) {
            return new UnparseResult(new ByteBuffer[] { buf });
        } else {
            queueOutput(buf);
            return dequeueOutput();
        }
    }

    private UnparseResult endMarker()
    {
        logger.debug(sessStr + " GOT END MARER!!");

        ByteBuffer buf = null;

        if (CHUNKED_ENCODING == transferEncoding) {
            buf = ByteBuffer.wrap(LAST_CHUNK);
        }

        if (outputQueue.isEmpty()) {
            return new UnparseResult(null == buf ? BYTE_BUFFER_PROTO
                                     : new ByteBuffer[] { buf });
        } else {
            if (null != buf) {
                queueOutput(buf);
            }

            return dequeueOutput();
        }
    }

    private void queueOutput(ByteBuffer buf)
    {
        size += buf.remaining();

        outputQueue.add(buf);
    }

    private UnparseResult dequeueOutput()
    {
        ByteBuffer buf = ByteBuffer.allocate(size);

        for (Iterator i = outputQueue.iterator(); i.hasNext(); ) {
            ByteBuffer b = (ByteBuffer)i.next();
            buf.put(b);
        }

        buf.flip();

        size = 0;
        outputQueue.clear();

        return new UnparseResult(new ByteBuffer[] { buf });
    }
}
