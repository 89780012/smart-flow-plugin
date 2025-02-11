package com.smart.language;
import com.intellij.lang.Language;

/**
 * 定义语言
 */
public class BizFileAction extends Language {

    public static final BizFileAction INSTANCE = new BizFileAction();

    protected BizFileAction() {
        super("BizFileAction");
    }
}




