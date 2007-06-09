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

package com.untangle.node.httpblocker;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.node.http.RequestLine;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

public class HttpBlockerPassedFilter implements ListEventFilter<HttpBlockerEvent>
{
    private static final String RL_QUERY = "FROM RequestLine rl ORDER BY rl.httpRequestEvent.timeStamp DESC";
    private static final String EVT_QUERY = "FROM HttpBlockerEvent evt WHERE evt.requestLine = :requestLine";

    private static final RepositoryDesc REPO_DESC
        = new RepositoryDesc("Passed HTTP Traffic");

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public boolean accept(HttpBlockerEvent e)
    {
        return null == e.getAction() || Action.PASS == e.getAction();
    }

    public void warm(Session s, List<HttpBlockerEvent> l, int limit,
                     Map<String, Object> params)
    {
        Query q = s.createQuery(RL_QUERY);
        for (String param : q.getNamedParameters()) {
            Object o = params.get(param);
            if (null != o) {
                q.setParameter(param, o);
            }
        }

        q.setMaxResults(limit);

        int c = 0;
        for (Iterator i = q.iterate(); i.hasNext() && c < limit; ) {
            RequestLine rl = (RequestLine)i.next();
            Query evtQ = s.createQuery(EVT_QUERY);
            evtQ.setEntity("requestLine", rl);
            HttpBlockerEvent evt = (HttpBlockerEvent)evtQ.uniqueResult();
            if (null == evt) {
                evt = new HttpBlockerEvent(rl, null, null, null, true);
                Hibernate.initialize(rl);
                l.add(evt);
                c++;
            } else if (Action.PASS == evt.getAction()) {
                Hibernate.initialize(evt);
                Hibernate.initialize(evt.getRequestLine());
                l.add(evt);
                c++;
            }
        }
    }
}
