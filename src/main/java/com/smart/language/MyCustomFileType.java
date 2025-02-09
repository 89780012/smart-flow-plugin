package com.smart.language;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MyCustomFileType extends LanguageFileType {

    public static final MyCustomFileType INSTANCE = new MyCustomFileType();

    private MyCustomFileType() {
        super(BizFileAction.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "BizFileAction";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "BizFileAction file";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension() {
        return "biz";
    }

    @Override
    public Icon getIcon() {
        return BizFileIcons.FILE;
    }
}
