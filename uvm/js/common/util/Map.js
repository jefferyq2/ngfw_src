Ext.define('Ung.util.Map', {
    singleton: true,
    alternateClassName: 'Map',

    policies: {
        1: 'Default'
    },

    init: function () {
        this.buildInterfacesMap();
        this.buildPoliciesMap();
    },

    buildInterfacesMap: function () {
        var interfaces, map = {};
        if (rpc.reportsManager) {
            interfaces = Rpc.directData('rpc.reportsManager.getInterfacesInfo').list;
            Ext.Array.each(interfaces, function (intf) {
                map[intf.interfaceId] = intf.name;
            });
        }
        this.interfaces = map;
    },

    buildPoliciesMap: function () {
        var policies,
            map = { 1: 'Default' };

        if (rpc.reportsManager) {
            policies = Rpc.directData('rpc.reportsManager.getPoliciesInfo');
            if (policies) {
                Ext.Array.each(policies, function (policy) {
                    // map[intf.interfaceId] = intf.name;
                });
            }
        }
        this.policies = map;
    },

    columns: {
        'request_id': { text: 'Request Id'.t(), colWidth: 120 },
        'time_stamp': { text: 'Timestamp'.t(), colWidth: 160 },
        'session_id': { text: 'Session Id'.t(), colWidth: 120 },
        'client_intf': { text: 'Client Interface'.t(), colWidth: 100 },
        'server_intf': { text: 'Server Interface'.t(), colWidth: 100 },
        'c_client_addr': { text: 'Client'.t(), colWidth: 120 },
        's_client_addr': { text: 'New Client'.t(), colWidth: 120 },
        'c_server_addr': { text: 'Original Server'.t(), colWidth: 120 },
        's_server_addr': { text: 'Server'.t(), colWidth: 120 },
        'c_client_port': { text: 'Client Port'.t(), colWidth: 120 },
        's_client_port': { text: 'New Client Port'.t(), colWidth: 120 },
        'c_server_port': { text: 'Original Server Port'.t(), colWidth: 120 },
        's_server_port': { text: 'Server Port'.t(), colWidth: 120 },
        'client_country': { text: 'Client Country'.t(), colWidth: 120 },
        'client_latitude': { text: 'Client Latitude'.t(), colWidth: 120 },
        'client_longitude': { text: 'Client Longitude'.t(), colWidth: 120 },
        'server_country': { text: 'Server Country'.t(), colWidth: 120 },
        'server_latitude': { text: 'Server Latitude'.t(), colWidth: 120 },
        'server_longitude': { text: 'Server Longitude'.t(), colWidth: 120 },
        'policy_id': { text: 'Policy Id'.t(), colWidth: 120 },
        'username': { text: 'Username'.t(), colWidth: 120 },
        'hostname': { text: 'Hostname'.t(), colWidth: 120 },
        'method': { text: 'Method'.t(), colWidth: 120 },
        'uri': { text: 'URI'.t(), colWidth: 120 },
        'host': { text: 'Host'.t(), colWidth: 250 },
        'domain': { text: 'Domain'.t(), colWidth: 120 },
        'referer': { text: 'Referer'.t(), colWidth: 120 },
        'c2s_content_length': { text: 'Upload Content Length'.t(), colWidth: 120 },
        's2c_content_length': { text: 'Download Content Length'.t(), colWidth: 120 },
        's2c_content_type': { text: 'Content Type'.t(), colWidth: 120 },
        's2c_content_filename': { text: 'Content Filename'.t(), colWidth: 120 },
        'ad_blocker_cookie_ident': { text: 'Blocked Cookie'.t() + ' (Ad Blocker)', colWidth: 120 },
        'ad_blocker_action': { text: 'Action'.t() + ' (Ad Blocker)', colWidth: 120 },
        'web_filter_reason': { text: 'Reason For Action'.t() + ' (Web Filter)', colWidth: 120 },
        'web_filter_category_id': { text: 'Web Category'.t() + ' (Web Filter)', colWidth: 250 },
        'web_filter_rule_id': { text: 'Web Rule'.t() + ' (Web Filter)', colWidth: 120 },
        'web_filter_blocked': { text: 'Blocked'.t() + ' (Web Filter)', colWidth: 120 },
        'web_filter_flagged': { text: 'Flagged'.t() + ' (Web Filter)', colWidth: 120 },
        'virus_blocker_lite_clean': { text: 'Clean'.t() + ' (Virus Blocker Lite)', colWidth: 120 },
        'virus_blocker_lite_name': { text: 'Virus Name'.t() + ' (Virus Blocker Lite)', colWidth: 120 },
        'virus_blocker_clean': { text: 'Clean'.t() + ' (Virus Blocker)', colWidth: 120 },
        'virus_blocker_name': { text: 'Virus Name'.t() + ' (Virus Blocker)', colWidth: 120 },
        'threat_prevention_blocked': { text: 'Blocked'.t() + ' (Threat Prevention)', colWidth: 120 },
        'threat_prevention_flagged': { text: 'Flagged'.t() + ' (Threat Prevention)', colWidth: 120 },
        'threat_prevention_rule_id': { text: 'Rule'.t() + ' (Threat Prevention)', colWidth: 120 },
        'threat_prevention_reputation': { text: 'Reputation'.t() + ' (Threat Prevention)', colWidth: 120 },
        'threat_prevention_categories': { text: 'Categories'.t() + ' (Threat Prevention)', colWidth: 120 }
    },

    webReasons: {
        D: 'in Categories Block list'.t(),
        U: 'in Site Block list'.t(),
        T: 'in Search Term list'.t(),
        E: 'in File Block list'.t(),
        M: 'in MIME Types Block list'.t(),
        H: 'hostname is an IP address'.t(),
        I: 'in Site Pass list'.t(),
        R: 'referer in Site Pass list'.t(),
        C: 'in Clients Pass list'.t(),
        B: 'in Temporary Unblocked list'.t(),
        F: 'in Rules list'.t(),
        K: 'Kid-friendly redirect'.t(),
        default: 'no rule applied'.t()
    },

    webCategories: {
        0: 'Uncategorized',
        1: 'Real Estate',
        2: 'Computer and Internet Security',
        3: 'Financial Services',
        4: 'Business and Economy',
        5: 'Computer and Internet Info',
        6: 'Auctions',
        7: 'Shopping',
        8: 'Cult and Occult',
        9: 'Travel',
        10: 'Abused Drugs',
        11: 'Adult and Pornography',
        12: 'Home and Garden',
        13: 'Military',
        14: 'Social Networking',
        15: 'Dead Sites',
        16: 'Individual Stock Advice and Tools',
        17: 'Training and Tools',
        18: 'Dating',
        19: 'Sex Education',
        20: 'Religion',
        21: 'Entertainment and Arts',
        22: 'Personal sites and Blogs',
        23: 'Legal',
        24: 'Local Information',
        25: 'Streaming Media',
        26: 'Job Search',
        27: 'Gambling',
        28: 'Translation',
        29: 'Reference and Research',
        30: 'Shareware and Freeware',
        31: 'Peer to Peer',
        32: 'Marijuana',
        33: 'Hacking',
        34: 'Games',
        35: 'Philosophy and Political Advocacy',
        36: 'Weapons',
        37: 'Pay to Surf',
        38: 'Hunting and Fishing',
        39: 'Society',
        40: 'Educational Institutions',
        41: 'Online Greeting Cards',
        42: 'Sports',
        43: 'Swimsuits and Intimate Apparel',
        44: 'Questionable',
        45: 'Kids',
        46: 'Hate and Racism',
        47: 'Personal Storage',
        48: 'Violence',
        49: 'Keyloggers and Monitoring',
        50: 'Search Engines',
        51: 'Internet Portals',
        52: 'Web Advertisements',
        53: 'Cheating',
        54: 'Gross',
        55: 'Web-based Email',
        56: 'Malware Sites',
        57: 'Phishing and Other Frauds',
        58: 'Proxy Avoidance and Anonymizers',
        59: 'Spyware and Adware',
        60: 'Music',
        61: 'Government',
        62: 'Nudity',
        63: 'News and Media',
        64: 'Illegal',
        65: 'Content Delivery Networks',
        66: 'Internet Communications',
        67: 'Bot Nets',
        68: 'Abortion',
        69: 'Health and Medicine',
        71: 'SPAM URLs',
        74: 'Dynamically Generated Content',
        75: 'Parked Domains',
        76: 'Alcohol and Tobacco',
        78: 'Image and Video Search',
        79: 'Fashion and Beauty',
        80: 'Recreation and Hobbies',
        81: 'Motor Vehicles',
        82: 'Web Hosting'
    },

    countries: {
        XU: 'Unknown'.t(),
        XL: 'Local'.t(),
        AF: 'Afghanistan'.t(),
        AX: 'Aland Islands'.t(),
        AL: 'Albania'.t(),
        DZ: 'Algeria'.t(),
        AS: 'American Samoa'.t(),
        AD: 'Andorra'.t(),
        AO: 'Angola'.t(),
        AI: 'Anguilla'.t(),
        AQ: 'Antarctica'.t(),
        AG: 'Antigua and Barbuda'.t(),
        AR: 'Argentina'.t(),
        AM: 'Armenia'.t(),
        AW: 'Aruba'.t(),
        AU: 'Australia'.t(),
        AT: 'Austria'.t(),
        AZ: 'Azerbaijan'.t(),
        BS: 'Bahamas'.t(),
        BH: 'Bahrain'.t(),
        BD: 'Bangladesh'.t(),
        BB: 'Barbados'.t(),
        BY: 'Belarus'.t(),
        BE: 'Belgium'.t(),
        BZ: 'Belize'.t(),
        BJ: 'Benin'.t(),
        BM: 'Bermuda'.t(),
        BT: 'Bhutan'.t(),
        BO: 'Bolivia, Plurinational State of'.t(),
        BQ: 'Bonaire, Sint Eustatius and Saba'.t(),
        BA: 'Bosnia and Herzegovina'.t(),
        BW: 'Botswana'.t(),
        BV: 'Bouvet Island'.t(),
        BR: 'Brazil'.t(),
        IO: 'British Indian Ocean Territory'.t(),
        BN: 'Brunei Darussalam'.t(),
        BG: 'Bulgaria'.t(),
        BF: 'Burkina Faso'.t(),
        BI: 'Burundi'.t(),
        KH: 'Cambodia'.t(),
        CM: 'Cameroon'.t(),
        CA: 'Canada'.t(),
        CV: 'Cape Verde'.t(),
        KY: 'Cayman Islands'.t(),
        CF: 'Central African Republic'.t(),
        TD: 'Chad'.t(),
        CL: 'Chile'.t(),
        CN: 'China'.t(),
        CX: 'Christmas Island'.t(),
        CC: 'Cocos (Keeling) Islands'.t(),
        CO: 'Colombia'.t(),
        KM: 'Comoros'.t(),
        CG: 'Congo'.t(),
        CD: 'Congo, the Democratic Republic of the'.t(),
        CK: 'Cook Islands'.t(),
        CR: 'Costa Rica'.t(),
        CI: "Cote d'Ivoire".t(),
        HR: 'Croatia'.t(),
        CU: 'Cuba'.t(),
        CW: 'Curacao'.t(),
        CY: 'Cyprus'.t(),
        CZ: 'Czech Republic'.t(),
        DK: 'Denmark'.t(),
        DJ: 'Djibouti'.t(),
        DM: 'Dominica'.t(),
        DO: 'Dominican Republic'.t(),
        EC: 'Ecuador'.t(),
        EG: 'Egypt'.t(),
        SV: 'El Salvador'.t(),
        GQ: 'Equatorial Guinea'.t(),
        ER: 'Eritrea'.t(),
        EE: 'Estonia'.t(),
        ET: 'Ethiopia'.t(),
        FK: 'Falkland Islands (Malvinas)'.t(),
        FO: 'Faroe Islands'.t(),
        FJ: 'Fiji'.t(),
        FI: 'Finland'.t(),
        FR: 'France'.t(),
        GF: 'French Guiana'.t(),
        PF: 'French Polynesia'.t(),
        TF: 'French Southern Territories'.t(),
        GA: 'Gabon'.t(),
        GM: 'Gambia'.t(),
        GE: 'Georgia'.t(),
        DE: 'Germany'.t(),
        GH: 'Ghana'.t(),
        GI: 'Gibraltar'.t(),
        GR: 'Greece'.t(),
        GL: 'Greenland'.t(),
        GD: 'Grenada'.t(),
        GP: 'Guadeloupe'.t(),
        GU: 'Guam'.t(),
        GT: 'Guatemala'.t(),
        GG: 'Guernsey'.t(),
        GN: 'Guinea'.t(),
        GW: 'Guinea-Bissau'.t(),
        GY: 'Guyana'.t(),
        HT: 'Haiti'.t(),
        HM: 'Heard Island and McDonald Islands'.t(),
        VA: 'Holy See (Vatican City State)'.t(),
        HN: 'Honduras'.t(),
        HK: 'Hong Kong'.t(),
        HU: 'Hungary'.t(),
        IS: 'Iceland'.t(),
        IN: 'India'.t(),
        ID: 'Indonesia'.t(),
        IR: 'Iran, Islamic Republic of'.t(),
        IQ: 'Iraq'.t(),
        IE: 'Ireland'.t(),
        IM: 'Isle of Man'.t(),
        IL: 'Israel'.t(),
        IT: 'Italy'.t(),
        JM: 'Jamaica'.t(),
        JP: 'Japan'.t(),
        JE: 'Jersey'.t(),
        JO: 'Jordan'.t(),
        KZ: 'Kazakhstan'.t(),
        KE: 'Kenya'.t(),
        KI: 'Kiribati'.t(),
        KP: "Korea, Democratic People's Republic of".t(),
        KR: 'Korea, Republic of'.t(),
        KW: 'Kuwait'.t(),
        KG: 'Kyrgyzstan'.t(),
        LA: "Lao People's Democratic Republic".t(),
        LV: 'Latvia'.t(),
        LB: 'Lebanon'.t(),
        LS: 'Lesotho'.t(),
        LR: 'Liberia'.t(),
        LY: 'Libya'.t(),
        LI: 'Liechtenstein'.t(),
        LT: 'Lithuania'.t(),
        LU: 'Luxembourg'.t(),
        MO: 'Macao'.t(),
        MK: 'Macedonia, the Former Yugoslav Republic of'.t(),
        MG: 'Madagascar'.t(),
        MW: 'Malawi'.t(),
        MY: 'Malaysia'.t(),
        MV: 'Maldives'.t(),
        ML: 'Mali'.t(),
        MT: 'Malta'.t(),
        MH: 'Marshall Islands'.t(),
        MQ: 'Martinique'.t(),
        MR: 'Mauritania'.t(),
        MU: 'Mauritius'.t(),
        YT: 'Mayotte'.t(),
        MX: 'Mexico'.t(),
        FM: 'Micronesia, Federated States of'.t(),
        MD: 'Moldova, Republic of'.t(),
        MC: 'Monaco'.t(),
        MN: 'Mongolia'.t(),
        ME: 'Montenegro'.t(),
        MS: 'Montserrat'.t(),
        MA: 'Morocco'.t(),
        MZ: 'Mozambique'.t(),
        MM: 'Myanmar'.t(),
        NA: 'Namibia'.t(),
        NR: 'Nauru'.t(),
        NP: 'Nepal'.t(),
        NL: 'Netherlands'.t(),
        NC: 'New Caledonia'.t(),
        NZ: 'New Zealand'.t(),
        NI: 'Nicaragua'.t(),
        NE: 'Niger'.t(),
        NG: 'Nigeria'.t(),
        NU: 'Niue'.t(),
        NF: 'Norfolk Island'.t(),
        MP: 'Northern Mariana Islands'.t(),
        NO: 'Norway'.t(),
        OM: 'Oman'.t(),
        PK: 'Pakistan'.t(),
        PW: 'Palau'.t(),
        PS: 'Palestine, State of'.t(),
        PA: 'Panama'.t(),
        PG: 'Papua New Guinea'.t(),
        PY: 'Paraguay'.t(),
        PE: 'Peru'.t(),
        PH: 'Philippines'.t(),
        PN: 'Pitcairn'.t(),
        PL: 'Poland'.t(),
        PT: 'Portugal'.t(),
        PR: 'Puerto Rico'.t(),
        QA: 'Qatar'.t(),
        RE: 'Reunion'.t(),
        RO: 'Romania'.t(),
        RU: 'Russian Federation'.t(),
        RW: 'Rwanda'.t(),
        BL: 'Saint Barthelemy'.t(),
        SH: 'Saint Helena, Ascension and Tristan da Cunha'.t(),
        KN: 'Saint Kitts and Nevis'.t(),
        LC: 'Saint Lucia'.t(),
        MF: 'Saint Martin (French part)'.t(),
        PM: 'Saint Pierre and Miquelon'.t(),
        VC: 'Saint Vincent and the Grenadines'.t(),
        WS: 'Samoa'.t(),
        SM: 'San Marino'.t(),
        ST: 'Sao Tome and Principe'.t(),
        SA: 'Saudi Arabia'.t(),
        SN: 'Senegal'.t(),
        RS: 'Serbia'.t(),
        SC: 'Seychelles'.t(),
        SL: 'Sierra Leone'.t(),
        SG: 'Singapore'.t(),
        SX: 'Sint Maarten (Dutch part)'.t(),
        SK: 'Slovakia'.t(),
        SI: 'Slovenia'.t(),
        SB: 'Solomon Islands'.t(),
        SO: 'Somalia'.t(),
        ZA: 'South Africa'.t(),
        GS: 'South Georgia and the South Sandwich Islands'.t(),
        SS: 'South Sudan'.t(),
        ES: 'Spain'.t(),
        LK: 'Sri Lanka'.t(),
        SD: 'Sudan'.t(),
        SR: 'Suriname'.t(),
        SJ: 'Svalbard and Jan Mayen'.t(),
        SZ: 'Swaziland'.t(),
        SE: 'Sweden'.t(),
        CH: 'Switzerland'.t(),
        SY: 'Syrian Arab Republic'.t(),
        TW: 'Taiwan, Province of China'.t(),
        TJ: 'Tajikistan'.t(),
        TZ: 'Tanzania, United Republic of'.t(),
        TH: 'Thailand'.t(),
        TL: 'Timor-Leste'.t(),
        TG: 'Togo'.t(),
        TK: 'Tokelau'.t(),
        TO: 'Tonga'.t(),
        TT: 'Trinidad and Tobago'.t(),
        TN: 'Tunisia'.t(),
        TR: 'Turkey'.t(),
        TM: 'Turkmenistan'.t(),
        TC: 'Turks and Caicos Islands'.t(),
        TV: 'Tuvalu'.t(),
        UG: 'Uganda'.t(),
        UA: 'Ukraine'.t(),
        AE: 'United Arab Emirates'.t(),
        GB: 'United Kingdom'.t(),
        US: 'United States'.t(),
        UM: 'United States Minor Outlying Islands'.t(),
        UY: 'Uruguay'.t(),
        UZ: 'Uzbekistan'.t(),
        VU: 'Vanuatu'.t(),
        VE: 'Venezuela, Bolivarian Republic of'.t(),
        VN: 'Viet Nam'.t(),
        VG: 'Virgin Islands, British'.t(),
        VI: 'Virgin Islands, U.S.'.t(),
        WF: 'Wallis and Futuna'.t(),
        EH: 'Western Sahara'.t(),
        YE: 'Yemen'.t(),
        ZM: 'Zambia'.t(),
        ZW: 'Zimbabwe'.t(),
    },
});
