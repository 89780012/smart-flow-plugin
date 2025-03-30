package com.smart.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.smart.cache.PluginCache;
import com.smart.utils.SpringBeanUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncTaskManager {
    private static AsyncTaskManager instance;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> springBeanLoadTask;
    private boolean isSpringBeanLoading = false;

    private AsyncTaskManager() {
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public static synchronized AsyncTaskManager getInstance() {
        if (instance == null) {
            instance = new AsyncTaskManager();
        }
        return instance;
    }

    public void startSpringBeanLoadTask(Project project, long initialDelay, long period) {
        if (project.isDisposed()) {
            return;
        }
        if (isSpringBeanLoading) {
            return;
        }

        isSpringBeanLoading = true;
        springBeanLoadTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                ReadAction.run(() -> {
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            Map<String, PsiClass> beanNames = ReadAction.compute(() ->
                                SpringBeanUtils.getAllSpringBeanNames(project)
                            );
                            if (!beanNames.isEmpty()) {
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    PluginCache.springBeanClasses = beanNames;
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, initialDelay, period, TimeUnit.SECONDS);

        System.out.println("Spring Bean定时加载任务已启动");
    }

    public void startSpringBeanLoadTask(Project project) {
        startSpringBeanLoadTask(project, 0, 10); // 默认0秒延迟，每10秒
    }

    public void stopSpringBeanLoadTask() {
        if (springBeanLoadTask != null && !springBeanLoadTask.isCancelled()) {
            springBeanLoadTask.cancel(true);
            isSpringBeanLoading = false;
            System.out.println("Spring Bean加载任务已停止");
        }
    }

    public void shutdown() {
        stopSpringBeanLoadTask();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void refreshSpringBeans(Project project) {
        if (project.isDisposed()) {
            return;
        }

        isSpringBeanLoading = false;
        
        stopSpringBeanLoadTask();

        ReadAction.run(() -> {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    Map<String, PsiClass> beanNames = ReadAction.compute(() ->
                        SpringBeanUtils.getAllSpringBeanNames(project)
                    );
                    if (!beanNames.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            PluginCache.springBeanClasses = beanNames;
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        startSpringBeanLoadTask(project, 10, 10); // 10秒后开始新的定时任务
    }
}