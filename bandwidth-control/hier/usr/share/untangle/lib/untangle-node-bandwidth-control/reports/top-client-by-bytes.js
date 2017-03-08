{
    "uniqueId": "bandwidth-control-pJgnRp3Lx8",
    "category": "Bandwidth Control",
    "description": "The sum of the data transferred grouped by client address.",
    "displayOrder": 301,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "round(coalesce(sum(s2c_bytes + c2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "session_minutes",
    "title": "Top Clients (by total bytes)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
