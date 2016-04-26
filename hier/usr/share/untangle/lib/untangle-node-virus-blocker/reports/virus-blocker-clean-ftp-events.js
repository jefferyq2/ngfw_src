{
    "category": "Virus Blocker",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "virus_blocker_clean",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","uri","virus_blocker_clean","virus_blocker_name","s_server_addr"],
    "description": "Scanned FTP sessions marked clean.",
    "displayOrder": 1032,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "table": "ftp_events",
    "title": "Clean Ftp Events",
    "uniqueId": "virus-blocker-YJ85M0V1KK"
}
