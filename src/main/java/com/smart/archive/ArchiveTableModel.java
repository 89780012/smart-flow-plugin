package com.smart.archive;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ArchiveTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {"时间", "描述", "类型", "下载状态", "上传状态", "操作"};
    private List<Map<String, Object>> data = new ArrayList<>();

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Map<String, Object> row = data.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return row.get("time");
            case 1:
                return row.get("description");
            case 2:
                return row.get("type");
            case 3:
                return row.get("downloadStatus");
            case 4:
                return row.get("uploadStatus");
            case 5:
                return row.get("id");
            default:
                return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; // 只有操作列可编辑
    }

    public Map<String, Object> getRowData(int row) {
        return data.get(row);
    }
} 