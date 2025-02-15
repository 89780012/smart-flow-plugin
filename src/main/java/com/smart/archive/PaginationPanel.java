package com.smart.archive;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PaginationPanel extends JPanel {
    private int currentPage = 1;
    private int totalPages;
    private int totalRecords;
    private static final int PAGE_SIZE = 10;
    private static final int MAX_PAGE_BUTTONS = 5;

    private JButton firstBtn;
    private JButton prevBtn; 
    private JButton nextBtn;
    private JButton lastBtn;
    private List<JButton> pageButtons;
    private JTextField pageInput;
    private JButton jumpBtn;
    private JLabel totalLabel;
    
    private Runnable onPageChange;

    public PaginationPanel(Runnable onPageChange) {
        super(new FlowLayout(FlowLayout.CENTER, 5, 0));
        this.onPageChange = onPageChange;
        this.setBorder(JBUI.Borders.empty(5));
        initComponents();
    }

    private void initComponents() {
        pageButtons = new ArrayList<>();
        
        firstBtn = new JButton("首页");
        prevBtn = new JButton("上一页");
        nextBtn = new JButton("下一页");
        lastBtn = new JButton("末页");
        
        pageInput = new JTextField(3);
        jumpBtn = new JButton("跳转");
        totalLabel = new JLabel();

        firstBtn.addActionListener(e -> {
            if(currentPage != 1) {
                currentPage = 1;
                updateUI();
                onPageChange.run();
            }
        });

        prevBtn.addActionListener(e -> {
            if(currentPage > 1) {
                currentPage--;
                updateUI();
                onPageChange.run();
            }
        });

        nextBtn.addActionListener(e -> {
            if(currentPage < totalPages) {
                currentPage++;
                updateUI();
                onPageChange.run();
            }
        });

        lastBtn.addActionListener(e -> {
            if(currentPage != totalPages) {
                currentPage = totalPages;
                updateUI();
                onPageChange.run();
            }
        });

        jumpBtn.addActionListener(e -> {
            try {
                int page = Integer.parseInt(pageInput.getText());
                if(page >= 1 && page <= totalPages && page != currentPage) {
                    currentPage = page;
                    updateUI();
                    onPageChange.run();
                }
            } catch(NumberFormatException ex) {
                // 忽略非法输入
            }
            pageInput.setText("");
        });
    }

    public void updateData(int totalRecords) {
        this.totalRecords = totalRecords;
        this.totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
        
        if(currentPage > totalPages) {
            currentPage = totalPages;
        }

        if(currentPage == 0){
            currentPage = 1;
        }
        
        updateUI();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        
        // 清除所有组件
        removeAll();
        
        if(totalRecords == 0) {
            add(new JLabel("暂无数据"));
            return;
        }

        // 添加导航按钮
        firstBtn.setEnabled(currentPage != 1);
        prevBtn.setEnabled(currentPage != 1);
        nextBtn.setEnabled(currentPage != totalPages);
        lastBtn.setEnabled(currentPage != totalPages);

        add(firstBtn);
        add(prevBtn);

        // 计算要显示的页码范围
        int start = Math.max(1, currentPage - MAX_PAGE_BUTTONS / 2);
        int end = Math.min(totalPages, start + MAX_PAGE_BUTTONS - 1);
        
        if(end - start + 1 < MAX_PAGE_BUTTONS) {
            start = Math.max(1, end - MAX_PAGE_BUTTONS + 1);
        }

        // 显示页码按钮
        if(start > 1) {
            add(new JLabel("..."));
        }
        
        for(int i = start; i <= end; i++) {
            JButton pageBtn = new JButton(String.valueOf(i));
            if(i == currentPage) {
                pageBtn.setEnabled(false);
            }
            pageBtn.addActionListener(e -> {
                currentPage = Integer.parseInt(pageBtn.getText());
                updateUI();
                onPageChange.run();
            });
            add(pageBtn);
        }

        if(end < totalPages) {
            add(new JLabel("..."));
        }

        add(nextBtn);
        add(lastBtn);

        // 添加跳转和统计信息
        if(totalPages > 5) {
            add(new JLabel("跳至"));
            add(pageInput);
            add(jumpBtn);
        }

        totalLabel.setText(String.format("共 %d 条记录", totalRecords));
        add(totalLabel);

        revalidate();
        repaint();
    }

    public int getCurrentPage() {
        return Math.max(1, currentPage);
    }
} 