package com.smart.ui;

import com.smart.enums.StepType;

import javax.swing.*;
import java.awt.*;

public class StepTypeCellEditor extends DefaultCellEditor {

    private final JComboBox<StepType> stepTypeCombo;

    public StepTypeCellEditor(JComboBox<StepType> comboBox) {
        super(comboBox);
        this.stepTypeCombo = comboBox;
    }

    @Override
    public Object getCellEditorValue() {
        StepType selectedStepType = (StepType) stepTypeCombo.getSelectedItem();
        return selectedStepType != null ? selectedStepType.getDisplayName() : "";
    }


    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value instanceof StepType) {
            stepTypeCombo.setSelectedItem(value);
        }
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }
}