package com.smart.archive;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ArchiveTableRenderer extends DefaultTableCellRenderer {

    private final ArchiveDialog dialog;

    public ArchiveTableRenderer(ArchiveDialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                 boolean isSelected, boolean hasFocus,
                                                 int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        // 设置为蓝色带下划线的文字
        label.setText("<html><u><font color='blue'>还原</font></u></html>");
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        return label;
    }
} 