/*
 * $Id$
 */
package com.untangle.node.reports.items;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class DetailSection extends Section implements Serializable
{
    private final List<ColumnDesc> columns = new ArrayList<ColumnDesc>();

    private String sql;

    public DetailSection(String name, String title)
    {
        super(name, title);
    }

    public List<ColumnDesc> getColumns()
    {
        return columns;
    }

    public void addColumn(ColumnDesc col)
    {
        columns.add(col);
    }

    public String getSql()
    {
        return sql;
    }

    public void setSql(String sql)
    {
        this.sql = sql;
    }
}