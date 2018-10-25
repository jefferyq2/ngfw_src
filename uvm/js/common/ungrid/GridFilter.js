Ext.define('Ung.cmp.GridFilter', {
    extend: 'Ext.form.FieldSet',
    alias: 'widget.ungridfilter',

    controller: 'ungridfilter',

    layout: 'hbox',
    border: 0,

    items: [{
        xtype: 'displayfield',
        labelCls: 'fa fa-search',
        labelWidth: 'auto',
        padding: '6 -10 -6 0'
    },{
        xtype: 'displayfield',
        cls: 'x-btn-inner-default-toolbar-small',
        name: 'filterLabel',
        value: 'Filter'.t(),
        labelWidth: 'auto',
        padding: '2 0 0 5',
        bind: {
            fieldStyle: '{filterStyle}'
        },
    },{
        xtype: 'textfield',
        name: 'filterSearch',
        _neverDirty: true,
        padding: '2 0 0 0',
        emptyText: 'Search ...'.t(),
        labelWidth: 'auto',
        enableKeyEvents: true,
        triggers: {
            clear: {
                cls: 'x-form-clear-trigger',
                hidden: true,
                handler: function (field) {
                    field.setValue('');
                }
            }
        },
        listeners: {
            change: 'changeFilterSearch',
            buffer: 100
        }
    },{
        xtype: 'tbtext',
        bind: {
            style: '{filterStyle}',
            html: '{filterSummary}'
        },
        padding: '5 0 0 6'
    }]
});