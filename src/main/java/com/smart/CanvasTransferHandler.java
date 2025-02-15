package com.smart;

import com.smart.bean.ComponentItem;
import com.smart.ui.VisualLayoutPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.UUID;

public class CanvasTransferHandler extends TransferHandler {
    private final VisualLayoutPanel visualLayoutPanel;

    public CanvasTransferHandler(VisualLayoutPanel editor) {
        this.visualLayoutPanel = editor;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(new DataFlavor(ComponentItem.class, "ComponentItem"));
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            System.out.println("无法导入组件: 不支持的数据类型");
            return false;
        }

        Transferable transferable = support.getTransferable();
        try {
            ComponentItem item = (ComponentItem) transferable.getTransferData(new DataFlavor(ComponentItem.class, "ComponentItem"));
            Point dropPoint = support.getDropLocation().getDropPoint();
            
            String id = UUID.randomUUID().toString();
            System.out.println("正在添加新拖拽组件: ID = " + id + ", 类型 = " + item.getName() + ", 位置 = " + dropPoint);
            visualLayoutPanel.addComponent(item, dropPoint, id, true);  // 传入 true 表示这是新拖拽的组件

            System.out.println("新拖拽组件添加成功: ID = " + id);
            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            System.err.println("添加组件时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
