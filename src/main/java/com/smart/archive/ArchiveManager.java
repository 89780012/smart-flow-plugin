package com.smart.archive;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.smart.BizFileEditor;
import com.smart.cache.PluginCache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.smart.settings.SmartPluginSettings;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;

import cn.hutool.http.HttpUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.core.io.FileUtil;

public class ArchiveManager {
    private static final Logger LOG = Logger.getInstance(ArchiveManager.class);
    private static final String REMOTE_API_URL = SmartPluginSettings.API_DOMAIN+ "/api/upload";
    private static final String REMOTE_LIST_API_URL = SmartPluginSettings.API_DOMAIN+ "/api/archives";
    
    private final Project project;
    private final VirtualFile file;
    private BizFileEditor bizFileEditor;
    
    private static final Gson gson = new Gson();
    private final String manifestFileName = "manifest.json";
    
    public ArchiveManager(Project project, VirtualFile file, BizFileEditor bizFileEditor) {
        this.project = project;
        this.file = file;
        this.bizFileEditor = bizFileEditor;
    }

    
    public void saveArchive(String description) {
        try {
            SimpleDateFormat dateTimeFmt = new SimpleDateFormat("yyyyMMddHHmmss");
            String dateTime = dateTimeFmt.format(new Date());
            
            String pluginDir = PathManager.getPluginsPath() + "/smart-flow-plugin/archives";
            String fileId = getFileId();
            
            String fullDirPath = pluginDir + "/" + fileId;
            String archivePath = fullDirPath + "/" + dateTime + ".xml";
            
            // 创建目录
            Files.createDirectories(Paths.get(fullDirPath));
            
            // 保存到本地
            Path localPath = Paths.get(archivePath);
            Files.copy(file.getInputStream(), localPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 果启用了远程存储且已授权,则异步上传到远程
            if (PluginCache.isValidLicense && PluginCache.enableRemoteStorage) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> 
                    uploadToRemote(new File(archivePath), fileId, dateTime, description)
                );
            }
            
            // 更新manifest
            ArchiveEntry entry = new ArchiveEntry(
                fileId,
                archivePath,
                description,
                dateTime,
                "1"
            );
            updateManifest(pluginDir + "/" + fileId, entry);
            LOG.info("Successfully saved archive to: " + archivePath);
        } catch (IOException e) {
            LOG.error("Failed to save archive", e);
            e.printStackTrace();
        }
    }
    
    public String getFileId() {
        try {
            // 创建DOM解析器
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            // 解析XML文件
            Document doc = builder.parse(file.getInputStream());
            
            // 获取id节点的值
            NodeList idNodes = doc.getElementsByTagName("id");
            if (idNodes.getLength() > 0) {
                String id = idNodes.item(0).getTextContent();
                return id != null && !id.trim().isEmpty() ? id : "default_id";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "default_id";
    }
    
    /**
     * 获取所有存档记录
     * @return 存档记录列表,包含id、time、description等信息
     */
    public List<Map<String, Object>> getArchives(String startTime) {
        Map<String, Map<String, Object>> archiveMap = new LinkedHashMap<>();
        try {
            String fileId = getFileId();
            
            // 获取本地存档
            String archiveRoot = PathManager.getPluginsPath() + "/smart-flow-plugin/archives/" + fileId;
            Path manifestPath = Paths.get(archiveRoot, manifestFileName);
            
            if (Files.exists(manifestPath)) {
                String content = new String(Files.readAllBytes(manifestPath));
                List<ArchiveEntry> localEntries = gson.fromJson(content, 
                    new TypeToken<List<ArchiveEntry>>(){}.getType());
                    
                // 添加本地存档到Map
                for (ArchiveEntry entry : localEntries) {
                    // 如果指定了startTime,过滤本地记录
                    if (startTime != null && entry.getTime().compareTo(startTime) < 0) {
                        continue;
                    }
                    
                    Map<String, Object> archive = new HashMap<>();
                    archive.put("id", entry.getFilePath());
                    archive.put("time", entry.getTime());
                    archive.put("type", "1"); // 本地类型
                    archive.put("description", entry.getDescription());
                    archiveMap.put(entry.getTime(), archive);
                }
            }

            // 如果启用了远程存储,获取远程存档(传入startTime)
            if (PluginCache.isValidLicense) {
                List<ArchiveEntry> remoteEntries = getRemoteArchives(fileId, startTime);
                for (ArchiveEntry entry : remoteEntries) {
                    Map<String, Object> archive = new HashMap<>();
                    archive.put("id", archiveRoot + "/" + entry.getFilePath());
                    archive.put("time", entry.getTime());
                    archive.put("type", "2"); // 远程类型
                    archive.put("description", entry.getDescription());
                    archive.put("stored_url", entry.getStoredUrl());
                    
                    // 如果本地没有该记录,标记为仅远程
                    if (!archiveMap.containsKey(entry.getTime())) {
                        archive.put("remote_only", true);
                    }
                    
                    archiveMap.put(entry.getTime(), archive);
                }
            }
            
        } catch (Exception e) {
            LOG.error("Failed to get archives", e);
        }
        
        // 转换为List并按时间倒序排序
        List<Map<String, Object>> archives = new ArrayList<>(archiveMap.values());
        archives.sort((a1, a2) -> ((String)a2.get("time")).compareTo((String)a1.get("time")));
        
        return archives;
    }

    /**
     * 还原到指定存档
     */
    public boolean restore(String archiveId) {
        try {
            // 获取存档文件
            Path archivePath = Paths.get(archiveId);
            if (!Files.exists(archivePath)) {
                LOG.error("Archive file not found: " + archiveId);
                return false;
            }

            // 读取存档内容
            byte[] content = Files.readAllBytes(archivePath);

            // 在写操作线程中执行文件更新和画布刷新
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    // 写入当前文件
                    try (OutputStream out = new FileOutputStream(file.getPath())) {
                        out.write(content);
                    }

                    // 刷新文件
                    file.refresh(false, false);
                    
                    // 立即在 EDT 执行画布刷新
                    SwingUtilities.invokeLater(() -> {
                        if (bizFileEditor != null) {
                            // 刷新画布
                            bizFileEditor.refresh();
                        }
                    });
                    
                } catch (IOException e) {
                    LOG.error("Failed to write file", e);
                }
            });

            LOG.info("Successfully restored from archive: " + archiveId);
            return true;

        } catch (IOException e) {
            LOG.error("Failed to restore from archive", e);
        }
        return false;
    }

    public void updateManifest(String dirPath, ArchiveEntry newEntry) throws IOException {
        Path manifestPath = Paths.get(dirPath, manifestFileName);

        List<ArchiveEntry> entries = new ArrayList<>();
        
        // 读取现有manifest
        if (Files.exists(manifestPath)) {
            String content = new String(Files.readAllBytes(manifestPath));
            entries = gson.fromJson(content, new TypeToken<List<ArchiveEntry>>(){}.getType());
        }

        //需要比对是否有重复的存档
        for (ArchiveEntry entry : entries) {
            if (entry.getTime().equals(newEntry.getTime())) {
                return;
            }
        }
        // 添加新条目
        entries.add(newEntry);
        
        // 写入manifest
        String json = gson.toJson(entries);
        Files.write(manifestPath, json.getBytes());
    }

    public void uploadToRemote(File file, String fileId, String time, String description) {
        LOG.info("准备上传文件: " + file.getName());
        LOG.info("目标URL: " + REMOTE_API_URL);
        LOG.info("文件ID: " + fileId);
        
        try {
            // 构建表单参数
            Map<String, Object> formMap = new HashMap<>();
            formMap.put("file_id", fileId);
            formMap.put("time", time);
            formMap.put("description", description);
            formMap.put("file", file);

            LOG.info("开始发送请求...");
            
            // 执行上传请求
            HttpResponse response = HttpUtil.createPost(REMOTE_API_URL)
                .timeout(60000) // 设置超时时间为60秒
                .form(formMap)  // 设置表单数据
                .execute();
            
            int responseCode = response.getStatus();
            String responseBody = response.body();
            
            LOG.info("服务器响应代码: " + responseCode);
            LOG.info("服务器响应内容: " + responseBody);

            if (!response.isOk()) {
                throw new IOException("服务器返回非成功状态码: " + responseCode + 
                                    "\n响应内容: " + responseBody);
            }

            // 在EDT线程中显示成功通知
            ApplicationManager.getApplication().invokeLater(() -> {
                Notifications.Bus.notify(new Notification(
                    "Smart Flow Notifications",
                    "远程存储成功",
                    "文件已成功上传到远程服务器",
                    NotificationType.INFORMATION
                ));
            });
            
            LOG.info("文件上传成功完成");
            
        } catch (Exception e) {
            LOG.error("文件上传失败", e);
            String errorMessage = e.getMessage();
            if (e instanceof java.net.ConnectException) {
                errorMessage = "无法连接到服务器，请检查网络连接和服务器状态";
            } else if (e instanceof java.net.SocketTimeoutException) {
                errorMessage = "连接服务器超时，请稍后重试";
            }
            
            final String finalErrorMessage = errorMessage;
            // 在EDT线程中显示错误通知
            ApplicationManager.getApplication().invokeLater(() -> {
                Notifications.Bus.notify(new Notification(
                    "Smart Flow Notifications",
                    "远程存储失败",
                    "文件已保存在本地,但上传到远程服务器失败: " + finalErrorMessage,
                    NotificationType.WARNING
                ));
            });
        } finally {
            LOG.info("文件上传操作完成");
        }
    }

    private List<ArchiveEntry> getRemoteArchives(String fileId, String startTime) {
        List<ArchiveEntry> remoteEntries = new ArrayList<>();
        try {
            // 构建带有startTime参数的URL
            String urlStr = REMOTE_LIST_API_URL + "/" + fileId;
            if (startTime != null) {
                urlStr += "?startTime=" + startTime;
            }
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    // 解析响应JSON
                    return gson.fromJson(response.toString(), 
                        new TypeToken<List<ArchiveEntry>>(){}.getType());
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to get remote archives", e);
        }
        return remoteEntries;
    }

    public void downloadRemoteArchive(String storedUrl, String targetPath) {
        try {
            LOG.info("开始下载文件: " + storedUrl);
            
            // 使用hutool下载文件
            long size = HttpUtil.downloadFile(storedUrl, FileUtil.file(targetPath), 60000);
            
            LOG.info("文件下载完成，大小: " + size + " bytes");

            // 通知下载成功
            ApplicationManager.getApplication().invokeLater(() -> {
                Notifications.Bus.notify(new Notification(
                    "Smart Flow Notifications",
                    "下载成功",
                    "远程存档已成功下载到本地",
                    NotificationType.INFORMATION
                ));
            });

        } catch (Exception e) {
            LOG.error("Failed to download remote archive", e);
            ApplicationManager.getApplication().invokeLater(() -> {
                Notifications.Bus.notify(new Notification(
                    "Smart Flow Notifications",
                    "下载失败",
                    "下载远程存档失败: " + e.getMessage(),
                    NotificationType.ERROR
                ));
            });
        }
    }
}