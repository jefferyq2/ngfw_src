/**
 * $Id$
 */
package com.untangle.app.threat_prevention;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for ip reputation.
 */
@SuppressWarnings("serial")
public class ThreatPreventionEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private long    ruleId;
    private boolean blocked;
    private boolean flagged;
    private int     clientReputation;
    private int     clientThreatmask;
    private int     serverReputation;
    private int     serverThreatmask;

    public ThreatPreventionEvent() { }

    public ThreatPreventionEvent( SessionEvent sessionEvent, boolean blocked,  boolean flagged, int ruleId , int clientReputation, int clientThreatmask, int serverReputation, int serverThreatmask)
    {
        this.sessionEvent = sessionEvent;
        this.blocked = blocked;
        this.flagged = flagged;
        this.ruleId  = ruleId;
        this.clientReputation  = clientReputation;
        this.clientThreatmask  = clientThreatmask;
        this.serverReputation  = serverReputation;
        this.serverThreatmask  = serverThreatmask;
    }

    public boolean getBlocked() { return blocked; }
    public void setBlocked( boolean blocked ) { this.blocked = blocked; }

    public boolean getFlagged() { return flagged; }
    public void setFlagged( boolean flagged ) { this.flagged = flagged; }
    
    public long getRuleId() { return ruleId; }
    public void setRuleId( long ruleId ) { this.ruleId = ruleId; }

    public int getClientReputation() { return clientReputation; }
    public void setClientReputation( int reputation ) { this.clientReputation = reputation; }

    public int getClientThreatmask() { return clientThreatmask; }
    public void setClientThreatmask( int threatmask ) { this.clientThreatmask = threatmask; }

    public int getServerReputation() { return serverReputation; }
    public void setServerReputation( int reputation ) { this.serverReputation = reputation; }

    public int getServerThreatmask() { return serverThreatmask; }
    public void setServerThreatmask( int threatmask ) { this.serverThreatmask = threatmask; }

    public Long getSessionId() { return sessionEvent.getSessionId(); }
    public void setSessionId( Long sessionId ) { this.sessionEvent.setSessionId(sessionId); }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + schemaPrefix() + "sessions" + sessionEvent.getPartitionTablePostfix() + " " +
            "SET threat_prevention_blocked = ?, " +
            "    threat_prevention_flagged = ?, " + 
            "    threat_prevention_rule_index = ?, " + 
            "    threat_prevention_client_reputation = ?, " + 
            "    threat_prevention_client_threatmask = ?, " + 
            "    threat_prevention_server_reputation = ?, " + 
            "    threat_prevention_server_threatmask = ? " + 
            "WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        
        
        int i=0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setBoolean(++i, getFlagged());
        pstmt.setLong(++i, getRuleId());
        pstmt.setInt(++i, getClientReputation());
        pstmt.setInt(++i, getClientThreatmask());
        pstmt.setInt(++i, getServerReputation());
        pstmt.setInt(++i, getServerThreatmask());
        pstmt.setLong(++i, getSessionId());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        if ( getBlocked() )
            action = I18nUtil.marktr("blocked");
        else
            action = I18nUtil.marktr("password");
            
        String summary = "Threat Prevention " + action + " " + sessionEvent.toSummaryString();
        return summary;
    }
}
