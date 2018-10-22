Ext.define('Ung.apps.intrusionprevention.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-intrusion-prevention-rules',
    itemId: 'rules',
    title: 'Rules'.t(),
    scrollable: true,

    controller: 'unintrusionrulegrid',

    viewConfig: {
        listeners: {
            drop: 'updateRuleStatus'
        }
    },

    emptyText: 'No Rules Defined'.t(),
    name: 'rules',

    restrictedRecords: {
        keyMatch: 'id',
        valueMatch: /^reserved\_/,
        editableFields:[
            "enabled"
        ]
    },

    region: 'center',

    bind: '{rules}',

    bbar: [{
        xtype: 'tbtext',
        name: 'ruleStatus',
    }],

    tbar: ['@add', '->', '@import', '@export'],
    recordActions: ['edit', 'copy', 'delete', 'reorder'],
    copyId: 'id',
    copyAppendField: 'description',
    recordModel: 'Ung.model.intrusionprevention.rule',

    emptyRow: {
        javaClass: 'com.untangle.app.intrusion_prevention.IntrusionPreventionRule',
        enabled: true,
        id: -1,
        description: '',
        conditions: {
            javaClass: "java.util.LinkedList",
            'list': []
        },
        action: 'default'
    },

    columns: [{
        xtype:'checkcolumn',
        header: "Enabled".t(),
        dataIndex: 'enabled',
        width: Renderer.booleanWidth
    },{
        header: "Description".t(),
        dataIndex: 'description',
        width: Renderer.messageWidth,
        flex:2,
        editor: {
            xtype: 'textfield',
            emptyText: '[no description]'.t(),
            allowBlank: false,
            blankText: 'The description cannot be blank.'.t(),
            // listeners: {
            //     change: 'editorTitleChange'
            // }
        }
    },{
        header: "Conditions".t(),
        dataIndex: 'conditions',
        width: Renderer.conditionsWidth,
        flex: 3,
        renderer: Ung.cmp.ConditionsEditor.renderer
    },{
        header: "Action".t(),
        dataIndex: 'action',
        width: Renderer.messageWidth,
        renderer: Ung.apps.intrusionprevention.MainController.ruleActionsRenderer,
        editor: {
            xtype: 'combo',
            editable: false,
            queryMode: 'local',
            bind: {
                store: '{ruleActionsStore}'
            },
            displayField: 'display',
            valueField: 'value'
        },
    }],

    editorXtype: 'ung.cmp.unintrusionrulesrecordeditor',
    editorFields: [{
        xtype:'checkbox',
        bind: '{record.enabled}',
        fieldLabel: 'Enabled'.t(),
    },{
        xtype:'textfield',
        bind: '{record.description}',
        fieldLabel: 'Description'.t(),
        emptyText: "[enter description]".t(),
        allowBlank: false
    },
    Ung.cmp.ConditionsEditor.build({
        // xtype: 'conditionseditor',
        xtype: 'ipsrulesconditionseditor',
        bind: '{record.conditions}',
        flex: 1,
        model: 'Ung.apps.intrusionprevention.model.Condition',
        fields:{
            type: 'type',
            comparator: 'comparator',
            value: 'value',
        },
        javaClassValue: 'com.untangle.app.intrusion_prevention.IntrusionPreventionRuleCondition',

        conditions: [{
            name: "SID",
            displayName: "Signature Identifier".t(),
            type: "textfield",
            comparator: 'numeric'
        },{
            name:"GID",
            displayName: "Group Identifier".t(),
            type: 'textfield',
            comparator: 'numeric'
        },{
            name:"CATEGORY",
            displayName: "Category".t(),
            type: 'checkboxgroup',
            store: Ung.apps.intrusionprevention.Main.categories,
            storeValue: 'name',
            storeLabel: 'name',
            storeTip: Ung.apps.intrusionprevention.Main.categoryRenderer,
            comparator: 'boolean'
        },{
            name:"CLASSTYPE",
            displayName: "Classtype".t(),
            type: 'checkboxgroup',
            store: Ung.apps.intrusionprevention.Main.classtypes,
            storeValue: 'name',
            storeLabel: 'name',
            storeTip: Ung.apps.intrusionprevention.Main.classtypeRenderer,
            comparator: 'boolean'
        },{
            name:"MSG",
            displayName: "Message".t(),
            type: 'textfield',
            // vtype: 'email'
            comparator: 'text'
        },{
            name:"PROTOCOL",
            displayName: "Protocol".t(),
            type: 'checkboxgroup',
            values: [],
            comparator: 'boolean'
        },{
            name:"SRC_ADDR",
            displayName: "Source Address".t(),
            type: 'textfield',
            vtype:"ipMatcher",
            comparator: 'text'
        },{
            name:"SRC_PORT",
            displayName: "Source Port".t(),
            type: 'textfield',
            comparator: 'numeric'
        },{
            name:"DST_ADDR",
            displayName: "Destination Address".t(),
            type: 'textfield',
            vtype:"ipMatcher",
            comparator: 'text'
        },{
            name:"DST_PORT",
            displayName: "Destination Port".t(),
            type: 'textfield',
            vtype:"ipMatcher",
            comparator: 'numeric'
        },{
            name:"SIGNATURE",
            displayName: "Any part of signature".t(),
            type: 'textfield',
            comparator: 'text'
        },{
            name:"CUSTOM",
            displayName: "Custom signature".t(),
            type: 'boolean',
            comparator: 'boolean',
            values: [[
                "true","True".t()
            ],[
                "false","False".t()
            ]]
        // },
        //     // ??? checkboxgroup
        // {
        //     name:"SOURCE",
        //     displayName: "Source".t(),
        //     type: 'textfield'
        },{
            name:"SYSTEM_MEMORY",
            displayName: "System Memory".t(),
            // type: 'numberfield',
            type: 'sizefield',
            comparator: 'numeric'
        }]
    }),
    {
        xtype: 'combo',
        fieldLabel: 'Actions'.t(),
        labelAlign: 'right',
        width: 400,
        bind: {
            store: '{ruleActionsStore}',
            value: '{record.action}'
            },
        queryMode: 'local',
        editable: false,
        // !!! move outside into conditions set as fields:
        typeField: 'type',
        displayField: 'display',
        valueField: 'value',
    }]
});
