package com.smart.utils;

import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;

import javax.swing.*;

public class AlertUtils {

    //在组件上给出提示
    public static void alertOnAbove(JComponent component,String text){
        // 显示成功提示
        SwingUtilities.invokeLater(() -> {
            JBPopupFactory.getInstance()
                    .createBalloonBuilder(new JLabel(text))
                    .setFadeoutTime(3000)
                    .createBalloon()
                    .show(RelativePoint.getNorthWestOf(component),
                            Balloon.Position.above);
        });
    }
}
