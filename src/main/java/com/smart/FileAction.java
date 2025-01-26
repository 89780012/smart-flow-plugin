package com.smart;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class FileAction extends FileTypeFactory {
    @Override
    public void createFileTypes(FileTypeConsumer consumer) {
        consumer.consume(new MyCustomFileType(), "biz"); // 替换为你的文件扩展名
    }
}

class MyCustomFileType implements FileType {

    @Override
    public String getName() {
        return "business name";
    }

    @Override
    public String getDescription() {
        return "business description";
    }

    @Override
    public String getDefaultExtension() {
        return "biz"; // 替换为你的文件扩展名
    }

    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/biz.svg",getClass());
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}

class BizFileEditorProvider implements FileEditorProvider {

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file.getExtension() != null && file.getExtension().equals("biz");
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new BizFileEditor(project, file);
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return "BizFileEditor";
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
    }
}

