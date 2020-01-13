Ext.define('Ung.apps.threatprevention.view.ThreatLookup', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-threat-prevention-threatlookup',
    itemId: 'lookup',
    title: 'Threat Lookup'.t(),
    scrollable: true,
    bodyPadding: 10,
    defaultButton: 'searchButton',
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: {
            fontSize: '12px',
            fontWeight: 600
        },
        html: 'Lookup the threat information for websites or IP Addresses.'.t()
    }],

    items: [{
        xtype: 'fieldset',
        title: 'IP Address and URL Threats'.t(),
        layout: 'vbox',
        items: [{
            xtype: 'displayfield',
            value: 'Threat Prevention must be enabled to perform lookups'.t(),
            bind: {
                hidden: '{state.on == true}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'Lookup Threat'.t(),
            fieldIndex: 'threatLookupInput',
            bind: {
                hidden: '{state.on == false}',
                value: '{threatLookupInput}'
            },
        }, {
            xtype: 'button',
            reference: 'searchButton',
            text: 'Search'.t(),
            iconCls: 'fa fa-search',
            handler: 'handleThreatLookup',
            bind: {
                hidden: '{state.on == false}',
                disabled: '{threatLookupInput.length === 0}'
            }
        }, {
            xtype: 'fieldset',
            title: 'Threat Results'.t(),
            layout: 'vbox',
            bind: {
                hidden: '{!threatLookupAddress}'
            },
            items: [{
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Address/URL'.t(),
                fieldIndex: 'threatLookupAddress',
                bind: {
                    value: '{threatLookupAddress}',
                    hidden: '{threatLookupAddress.length === 0}'
                }
            }, {
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Category'.t(),
                fieldIndex: 'threatLookupCategory',
                bind: {
                    value: '{threatLookupCategory}',
                    hidden: '{threatLookupCategory.length === 0}'
                }
            }, {
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Reputation Score'.t(),
                fieldIndex: 'threatLookupReputationScore',
                bind: {
                    value: '{threatLookupReputationScore}',
                    hidden: '{threatLookupReputationScore.length === 0}'
                }
            }, {
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Reputation Level'.t(),
                fieldIndex: 'threatLookupReputationLevel',
                bind: {
                    value: '{threatLookupReputationLevel}',
                    hidden: '{threatLookupReputationLevel.length === 0}'
                }
            }, {
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Reputation Level Details'.t(),
                fieldIndex: 'threatLookupReputationLevelDetails',
                bind: {
                    value: '{threatLookupReputationLevelDetails}',
                    hidden: '{threatLookupReputationLevelDetails.length === 0}'
                }
            }]
        }]
    }]
});
