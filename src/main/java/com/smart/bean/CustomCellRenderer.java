package com.smart.bean;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class CustomCellRenderer extends DefaultTableCellRenderer {
    private static final Color DISABLED_BACKGROUND = new Color(245, 245, 245); // 浅灰色背景

    private List<Integer> disEditableColumns;

    public CustomCellRenderer(List<Integer> disEditableColumns) {
        this.disEditableColumns = disEditableColumns;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // 其他列使用普通渲染
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // 检查列是否可编辑
        boolean isEditable = !disEditableColumns.contains(column);

        if (!isEditable) {
            // 不可编辑列使用浅灰色背景
            setBackground(DISABLED_BACKGROUND);
            setForeground(Color.BLACK);
            // 为不可编辑列设置细线边框
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    BorderFactory.createEmptyBorder(1, 4, 1, 4)
            ));
        } else {
            // 可编辑列使用白色背景
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
            // 为可编辑列设置默认边框
            setBorder(isSelected ?
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(table.getSelectionBackground(), 1),
                            BorderFactory.createEmptyBorder(1, 4, 1, 4)
                    ) :
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                            BorderFactory.createEmptyBorder(1, 4, 1, 4)
                    )
            );
        }

        return this;
    }
}