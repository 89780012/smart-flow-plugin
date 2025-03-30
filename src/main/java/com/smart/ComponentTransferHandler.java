package com.smart;

import com.smart.bean.ComponentItem;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class ComponentTransferHandler extends TransferHandler {
    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node != null && node.getUserObject() instanceof ComponentItem) {
            ComponentItem item = (ComponentItem) node.getUserObject();
            return new StringSelection(item.getName());
        }
        return null;
    }
}
