/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareHttpHandler.java 8668 2007-01-29 19:17:09Z amread $
 */

package com.untangle.tran.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import org.apache.log4j.Logger;
import sun.misc.BASE64Decoder;

public class EncryptedUrlDatabase extends UrlDatabase
{
    private static final byte[] DB_SALT = "oU3q.72p".getBytes();
    private static final byte[] VERSION_KEY = "goog-black-enchash".getBytes();

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\[goog-black-enchash ([0-9.]+)\\]");
    private static final Pattern TUPLE_PATTERN = Pattern.compile("\\+([0-9A-F]+)\t([A-Za-z0-9+/=]+)");

    private final Logger logger = Logger.getLogger(getClass());

    EncryptedUrlDatabase() throws DatabaseException, IOException { }

    // UrlDatabase methods ----------------------------------------------------

    protected void updateDatabase(Database db) throws IOException
    {
        DatabaseEntry versionKey = new DatabaseEntry(VERSION_KEY);

        try {
            // XXX do an update if exists
            if (OperationStatus.SUCCESS == db.get(null, versionKey, new DatabaseEntry(), LockMode.DEFAULT)) {
                return;
            }
        } catch (DatabaseException exn) {
            logger.warn("could not get database version", exn);
        }


        URL url = new URL("http://sb.google.com/safebrowsing/update?version=goog-black-enchash:1:1");
        InputStream is = url.openStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = br.readLine();

        String version;

        Matcher matcher = VERSION_PATTERN.matcher(line);
        if (matcher.find()) {
            version = matcher.group(1);
            try {
                db.put(null, versionKey, new DatabaseEntry(version.getBytes()));
            } catch (DatabaseException exn) {
                logger.warn("could not set database version", exn);
            }
        } else {
            logger.warn("No version number: " + line);
        }

        while (null != (line = br.readLine())) {
            matcher = TUPLE_PATTERN.matcher(line);
            if (matcher.find()) {
                byte[] host = new BigInteger(matcher.group(1), 16).toByteArray();
                byte[] b64Data = matcher.group(2).getBytes();
                byte[] regexp = base64Decode(matcher.group(2));

                try {
                    db.put(null, new DatabaseEntry(host),
                           new DatabaseEntry(regexp));
                } catch (DatabaseException exn) {
                    logger.warn("could not add database entry", exn);
                }
            }
        }
    }

    protected byte[] getKey(byte[] host)
    {
        byte[] in = new byte[DB_SALT.length + host.length];
        System.arraycopy(DB_SALT, 0, in, 0, DB_SALT.length);

        System.arraycopy(host, 0, in, DB_SALT.length, host.length);

        // XXX Switch to Fast MD5 http://www.twmacinta.com/myjava/fast_md5.php
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exn) {
            logger.warn("Could not get MD5 algorithm", exn);
            return null;
        }

        return md.digest(in);
    }

    protected List<String> getValues(byte[] host, byte[] data)
    {
        byte[] buf = new byte[8 + DB_SALT.length + host.length];
        System.arraycopy(DB_SALT, 0, buf, 0, DB_SALT.length);
        System.arraycopy(data, 0, buf, DB_SALT.length, 8);
        System.arraycopy(host, 0, buf, 8 + DB_SALT.length, host.length);

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exn) {
            logger.warn("Could not get MD5 algorithm", exn);
            return Collections.emptyList();
        }
        buf = md.digest(buf);

        Cipher arcfour;
        try {
            arcfour = Cipher.getInstance("ARCFOUR");
            Key key = new SecretKeySpec(buf, "ARCFOUR");
            arcfour.init(Cipher.DECRYPT_MODE, key);
        } catch (GeneralSecurityException exn) {
            logger.warn("could not get ARCFOUR algorithm", exn);
            return Collections.emptyList();
        }

        try {
            buf = arcfour.doFinal(data, 8, data.length - 8);
        } catch (GeneralSecurityException exn) {
            logger.warn("could not decrypt regexp", exn);
            return Collections.emptyList();
        }

        List<String> l = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            char c = (char)buf[i];
            if ('\t' == c) {
                l.add(sb.toString());
                sb.delete(0, sb.length());
            } else {
                sb.append(c);
            }
        }
        l.add(sb.toString());

        return l;
    }

    // private methods --------------------------------------------------------

    private byte[] base64Decode(String s) {
        try {
            return new BASE64Decoder().decodeBuffer(s);
        } catch (IOException exn) {
            logger.warn("could not decode", exn);
            return new byte[0];
        }
    }
}
