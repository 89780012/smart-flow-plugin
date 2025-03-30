package com.smart.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.smart.CanvasTransferHandler;
import com.smart.bean.ComponentInfo;
import com.smart.bean.ComponentItem;
import com.smart.bean.ComponentProp;
import com.smart.bean.Connection;
import com.smart.cache.PluginCache;
import com.smart.dialog.*;
import com.smart.enums.ReturnType;
import com.smart.enums.ThreadType;
import com.smart.listener.MouseOverBtnAdapter;
import com.smart.tasks.AsyncTaskManager;
import com.smart.utils.AlertUtils;
import com.smart.utils.IconUtils;
import com.smart.utils.ToggleButtonUtils;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.RoundRectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.List;

/**
 *  创建中间画布
 */
public class VisualLayoutPanel {
    private final Project project;
    private JPanel mainPanel; // 替换原来的 layeredPane
    private List<Connection> connections = new ArrayList<>();
    private boolean isDraggingControlPoint = false;
    private Rectangle selectionRect; // 框选区域
    private Point selectionStart; // 框选
    private Point dragStart;
    private Point dragEnd;
    private JLayeredPane startComponent;
    private boolean isConnecting = false;
    private JTextField editingTextField;
    private static final int CONNECTION_CLICK_TOLERANCE = 10;
    private Connection editingConnection;
    private static final Cursor CONNECT_CURSOR = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private List<JLayeredPane> selectedComponents = new ArrayList<>();
    private Connection draggedConnection;
    private final List<ComponentInfo> components = new ArrayList<>();
    //框选标识为
    private boolean isSelecting = false;
    // 在类的开头添加一个用于存储复制的组件的列表
    private List<ComponentInfo> copiedComponents = new ArrayList<>();

    private boolean isPanning = true; // 是否正在平移画布
    private Point lastPanPoint; // 上次平移的位置
    private JPanel viewPort;
    private JPanel containerPanel;
    private JPanel canvasPanel;

    private List<ComponentSelectionListener> selectionListeners = new ArrayList<>();

    private static final Color EXPRESSION_LINE_COLOR = new Color(255, 152, 0); // 使用橙色


    //工具栏
    private JPanel toolbarPanel;
    private JToggleButton selectedButton; // 当前选中的按钮
    private static final String PAN_TOOL = "pan";
    private static final String SELECT_TOOL = "select";
    private String currentTool = SELECT_TOOL; // 默认为选择工具

    private VirtualFile currentFile;

    // 在类的开头添加新的成员变量
    private Point highlightedEndpoint = null; // 当前高亮的端点
    private boolean isDraggingEndpoint = false; // 是否正在拖拽端点
    private Connection draggingConnection = null; // 正在拖拽的连接线
    private boolean isDraggingStartPoint = false; // 是否在拖拽起点(true为起点,false为终点)
    private Point originalPoint = null; // 存储原始端点位置
    private boolean wasInPanMode = false; // 存储拖拽前的平移状态

    private MiniMapDialog miniMapDialog;
    private JToggleButton miniMapButton;

    private JToggleButton cunDangButton;  //创建存档按钮

    public interface ComponentSelectionListener {
        void onComponentSelected(String componentId);
    }


    protected void fireComponentSelected(String componentId) {
        for (ComponentSelectionListener listener : selectionListeners) {
            listener.onComponentSelected(componentId);
        }
    }
    
    public VisualLayoutPanel(VirtualFile file,Project project) {
        this.project = project;
        this.createCanvasLayout();
        this.currentFile = file;
    }

    //创建画布
    public JPanel createCanvasLayout() {
        // 创建一个JPanel作为根容器，使用BorderLayout
        mainPanel = new JPanel(new BorderLayout());
        // 创建一个视口面板
        viewPort = new JPanel(new BorderLayout());
        viewPort.setPreferredSize(new Dimension(800, 600));

        // 创建一个容器面板
        containerPanel = new JPanel(null);
        containerPanel.setPreferredSize(new Dimension(1600, 1200));

        // 中间画布
        canvasPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // 保持原有的绘制代码不变
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int w = getWidth();
                int h = getHeight();

                // 设置背景
                g2d.setColor(new Color(245, 245, 250));
                g2d.fillRect(0, 0, w, h);

                // 绘制点阵
                g2d.setColor(new Color(200, 200, 220));
                int dotSize = 2;
                int spacing = 20;
                for (int x = spacing; x < w; x += spacing) {
                    for (int y = spacing; y < h; y += spacing) {
                        g2d.fillOval(x - dotSize/2, y - dotSize/2, dotSize, dotSize);
                    }
                }
                paintConnections(g2d);
                //矩阵区域
                if (selectionRect != null) {
                    g2d.setColor(new Color(0, 120, 215, 50));
                    g2d.fill(selectionRect);
                    g2d.setColor(new Color(0, 120, 215));
                    g2d.draw(selectionRect);
                }
            }
        };
        canvasPanel.setLayout(null);
        canvasPanel.setTransferHandler(new CanvasTransferHandler(this));
        canvasPanel.setBounds(0, 0, 1600, 1200);

        // 将canvasPanel添加到containerPanel
        containerPanel.add(canvasPanel);

        // 创建滚动面板并添加containerPanel
        JScrollPane scrollPane = new JScrollPane(containerPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.setViewportBorder(null);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        viewPort.add(scrollPane, BorderLayout.CENTER);

        // 添加键盘监听器
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (e.getID() == KeyEvent.KEY_PRESSED && !isPanning) {
                    isPanning = true;
                    viewPort.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                    isPanning = false;
                    viewPort.setCursor(Cursor.getDefaultCursor());
                    lastPanPoint = null;
                }
            }
            return false;
        });

        // 保持原有的鼠标监听器代码不变
        MouseAdapter comprehensiveMouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point adjustedPoint = e.getPoint();
                Point endpoint = getEndpointAtPoint(e.getPoint());
                
                boolean isCLickEndPoint = false;
                if (endpoint != null && SwingUtilities.isLeftMouseButton(e)) {
                    // 如果点击了端点,暂时禁用平移功能
                    boolean wasInPanMode1 = isPanning;
                    isPanning = false;
                    isCLickEndPoint = true;
                    // 开始拖拽端点
                    for (Connection conn : connections) {
                        if (endpoint.equals(conn.startPoint)) {
                            draggingConnection = conn;
                            isDraggingStartPoint = true;
                            break;
                        } else if (endpoint.equals(conn.endPoint)) {
                            draggingConnection = conn;
                            isDraggingStartPoint = false;
                            break;
                        }
                    }
                    
                    if (draggingConnection != null) {
                        isDraggingEndpoint = true;
                        originalPoint = endpoint;
                        dragStart = e.getPoint();
                        // 存储之前的平移状态
                        wasInPanMode = wasInPanMode1;
                    }
                    return;
                }
                //点击了连接端点
                if(!isCLickEndPoint){
                    //平移工具或者空格键均可以触发平移效果
                    if ((PAN_TOOL.equals(currentTool) || isPanning) && SwingUtilities.isLeftMouseButton(e)) {
                        // 平移过程中，如果没有点击组件上，则将全部组件的选中状态清除
                        JLayeredPane componentAtPoint = getComponentAtPoint(adjustedPoint);
                        if (componentAtPoint == null) {  //没有选上组件
                            clearSelection();
                        }

                        // 平移工具的处理逻辑
                        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, containerPanel);
                        if (scrollPane != null) {
                            lastPanPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), scrollPane.getViewport());
                        }
                        return;
                    }
                }
                //编辑文字
                if (editingTextField != null && !editingTextField.getBounds().contains(e.getPoint())) {
                    finishEditing();
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (endpoint != null) {
                        // 开始拖拽端点
                        for (Connection conn : connections) {
                            if (endpoint.equals(conn.startPoint)) {
                                draggingConnection = conn;
                                isDraggingStartPoint = true;
                                break;
                            } else if (endpoint.equals(conn.endPoint)) {
                                draggingConnection = conn;
                                isDraggingStartPoint = false;
                                break;
                            }
                        }
                        if (draggingConnection != null) {
                            isDraggingEndpoint = true;
                            originalPoint = endpoint;
                            dragStart = e.getPoint();
                        }
                    } else {
                        //没有点击连接线
                        for (Connection c : connections) {
                            c.isSelected = false;
                        }
                        //获取指定点上的组件
                        JLayeredPane componentAtPoint = getComponentAtPoint(adjustedPoint);
                        if (componentAtPoint == null) {  //没有选上组件
                            // 点击空白处时清除所有选中状态
                            clearSelection();
                            // 非平移模式下才启动框选或清除选择
                            if (!isPanning && SELECT_TOOL.equals(currentTool)) {
                                // 如果是开始框选，则设置框选起点
                                isSelecting = true;
                                selectionStart = e.getPoint();
                                selectionRect = new Rectangle(selectionStart);
                            }
                        } else {
                            handleNodeClick(componentAtPoint, e);
                        }
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    //连接线弹框控制
                    Connection clickedConnection = getConnectionAtPoint(e.getPoint());
                    if (clickedConnection != null) {
                        showDeleteConnectionMenu(clickedConnection, e.getPoint());
                    }
                }
                canvasPanel.repaint();
                canvasPanel.requestFocusInWindow();
            }

            //鼠标释放，框选模式
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDraggingEndpoint && draggingConnection != null) {
                    JLayeredPane targetComponent = getComponentAtPoint(e.getPoint());
                    if (targetComponent != null) {
                        // 获取目标组件的连接点
                        Point newEndpoint;
                        if (isDraggingStartPoint) {
                            // 如果拖动的是起点,需要从目标组件到终点组件的连接点
                            Component endComponent = getComponentByInfo(draggingConnection.end);
                            if (endComponent != null) {
                                newEndpoint = getConnectionPoint(targetComponent, (JLayeredPane)endComponent);
                                // 同时更新终点的连接点位置
                                draggingConnection.endPoint = getConnectionPoint((JLayeredPane)endComponent, targetComponent);
                            } else {
                                newEndpoint = getConnectionPoint(targetComponent, new JLayeredPane() {{ 
                                    setBounds(e.getPoint().x, e.getPoint().y, 1, 1); 
                                }});
                            }
                        } else {
                            // 如果拖动��是终点,需要从目标组件到起点组件的连接点
                            Component startComponent = getComponentByInfo(draggingConnection.start);
                            if (startComponent != null) {
                                newEndpoint = getConnectionPoint(targetComponent, (JLayeredPane)startComponent);
                                // 同时更新起点的连接点位置
                                draggingConnection.startPoint = getConnectionPoint((JLayeredPane)startComponent, targetComponent);
                            } else {
                                newEndpoint = getConnectionPoint(targetComponent, new JLayeredPane() {{
                                    setBounds(e.getPoint().x, e.getPoint().y, 1, 1);
                                }});
                            }
                        }

                        // 更新连接线端点和组件信息
                        if (isDraggingStartPoint) {
                            draggingConnection.startPoint = newEndpoint;
                            draggingConnection.start = PluginCache.componentInfoMap.get(
                                PluginCache.sourseCodeUtils.getComponentId(String.valueOf(targetComponent.getClientProperty("id"))));
                        } else {
                            draggingConnection.endPoint = newEndpoint;
                            draggingConnection.end = PluginCache.componentInfoMap.get(
                                PluginCache.sourseCodeUtils.getComponentId(String.valueOf(targetComponent.getClientProperty("id"))));
                        }

                        // 更新源代码
                        PluginCache.sourseCodeUtils.updateSourceCode();
                    } else {
                        // 如果没有拖到组件上,恢复原始位置
                        if (isDraggingStartPoint) {
                            draggingConnection.startPoint = originalPoint;
                        } else {
                            draggingConnection.endPoint = originalPoint;
                        }
                    }

                    // 重置状态
                    isDraggingEndpoint = false;
                    draggingConnection = null;
                    originalPoint = null;

                    // 恢复之前的平移状态
                    if (wasInPanMode) {
                        isPanning = true;
                    }

                    canvasPanel.repaint();
                } else if (isConnecting) {
                    JLayeredPane endComponent = getComponentAtPoint(e.getPoint());
                    if (endComponent != null && endComponent != startComponent) {
                        createNewConnection(startComponent, endComponent);
                    }
                    isConnecting = false;
                    startComponent = null;
                } else if (isSelecting) { //框选结束
                    System.out.println("结束框选操作");
                    selectComponentsInRect(e.isControlDown());
                    selectionRect = null;
                    selectionStart = null;
                    isSelecting = false;
                } else if (dragStart != null) {
                    JLayeredPane componentAtPoint = getComponentAtPoint(dragStart);
                    if (componentAtPoint != null) {
                        String id = (String) componentAtPoint.getClientProperty("id");
                        updateComponentPosition(id, componentAtPoint.getLocation());
                        PluginCache.sourseCodeUtils.updateSourceCode();
                    }
                }
                draggedConnection = null;
                dragStart = null;
                dragEnd = null;
                canvasPanel.repaint();
                canvasPanel.requestFocusInWindow(); // 确保canvasPanel获得焦点
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDraggingEndpoint && draggingConnection != null) {
                    // 更新被拖拽的端点位置
                    if (isDraggingStartPoint) {
                        draggingConnection.startPoint = e.getPoint();
                    } else {
                        draggingConnection.endPoint = e.getPoint();
                    }
                    canvasPanel.repaint();
                    return;
                }
                if (isPanning && lastPanPoint != null && SwingUtilities.isLeftMouseButton(e)) {
                    JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, containerPanel);
                    if (scrollPane != null) {
                        // 将当前事件坐标转换为滚动面板的视图坐标
                        Point currentPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), scrollPane.getViewport());

                        // 计算偏移量
                        int dx = lastPanPoint.x - currentPoint.x;
                        int dy = lastPanPoint.y - currentPoint.y;

                        // 获取前滚动位置
                        Point viewPosition = scrollPane.getViewport().getViewPosition();

                        // 计算新的滚动位置
                        int newX = Math.max(0, Math.min(viewPosition.x + dx,
                            containerPanel.getWidth() - scrollPane.getViewport().getWidth()));
                        int newY = Math.max(0, Math.min(viewPosition.y + dy,
                            containerPanel.getHeight() - scrollPane.getViewport().getHeight()));

                        // 直接设置视图位置，而不是使用滚动条
                        scrollPane.getViewport().setViewPosition(new Point(newX, newY));

                        // 更新上次拖动点
                        lastPanPoint = currentPoint;
                    }
                    return;
                }
                if (isConnecting) {
                    dragEnd = e.getPoint();
                } else if (isSelecting) {
                    int x = Math.min(selectionStart.x, e.getX());
                    int y = Math.min(selectionStart.y, e.getY());
                    int width = Math.abs(selectionStart.x - e.getX());
                    int height = Math.abs(selectionStart.y - e.getY());
                    selectionRect.setBounds(x, y, width, height);
                    System.out.println("框选区域更新：" + selectionRect);
                } else if (draggedConnection != null) {
                    Point dragEnd = e.getPoint();
                    int dx = dragEnd.x - dragStart.x;
                    int dy = dragEnd.y - dragStart.y;
                    draggedConnection.controlPoint.x += dx;
                    draggedConnection.controlPoint.y += dy;
                    dragStart = dragEnd;
                    canvasPanel.repaint();
                } else if (dragStart != null) {
                    JLayeredPane componentAtPoint = getComponentAtPoint(dragStart);
                    if (componentAtPoint != null) {
                        Point currentPoint = e.getPoint();
                        int dx = currentPoint.x - dragStart.x;
                        int dy = currentPoint.y - dragStart.y;
                        Point newLocation = new Point(componentAtPoint.getX() + dx, componentAtPoint.getY() + dy);
                        componentAtPoint.setLocation(newLocation);
                        String id = (String) componentAtPoint.getClientProperty("id");
                        updateComponentPosition(id, newLocation);
                    }
                }
                canvasPanel.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Connection conn = getConnectionAtPoint(e.getPoint());
                Point endpoint = getEndpointAtPoint(e.getPoint());

                if (endpoint != null) {
                    // 鼠标在端点附近
                    highlightedEndpoint = endpoint;
                    canvasPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else if (conn != null && isNearControlPoint(e.getPoint(), conn)) {
                    // 原有的控制点逻辑
                    canvasPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    highlightedEndpoint = null;
                } else {
                    JLayeredPane componentAtPoint = getComponentAtPoint(e.getPoint());
                    if (componentAtPoint != null && isOverBorder(componentAtPoint, SwingUtilities.convertPoint(canvasPanel, e.getPoint(), componentAtPoint))) {
                        canvasPanel.setCursor(CONNECT_CURSOR);
                    } else {
                        canvasPanel.setCursor(Cursor.getDefaultCursor());
                    }
                    highlightedEndpoint = null;
                }
                canvasPanel.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                //点击事件全部调整
                if (e.getClickCount() == 2) {
                    //双击连接线
                    Connection clickedConnection = getConnectionAtPoint(e.getPoint());
                    if (clickedConnection != null) {
                        startEditingConnectionLabel(clickedConnection, e.getPoint());
                    } else {
                        //双击节点
                        JLayeredPane componentAtPoint = getComponentAtPoint(e.getPoint());
                        if (componentAtPoint != null && !isOverBorder(componentAtPoint, SwingUtilities.convertPoint(canvasPanel, e.getPoint(), componentAtPoint))) {
                            startEditingComponentName(componentAtPoint, SwingUtilities.convertPoint(canvasPanel, e.getPoint(), componentAtPoint));
                        }
                    }
                }
            }

        };

        // 为containerPanel添加鼠标监听器
        containerPanel.addMouseListener(comprehensiveMouseListener);
        containerPanel.addMouseMotionListener(comprehensiveMouseListener);

        // 添加键事件监听器到 canvasPanel
        canvasPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
                    copySelectedComponents();
                } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {
                    pasteCopiedComponents();
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelectedNodes();
                }
            }
        });
        canvasPanel.setFocusable(true);
        canvasPanel.requestFocusInWindow();

        // 创建工具栏和包装面板
        toolbarPanel = createToolbar();
        JPanel toolbarWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 确保工具栏背景是透明的
                setOpaque(false);
            }
        };
        toolbarWrapper.setOpaque(false);
        toolbarWrapper.setBorder(null); // 移除边距，消除缝隙
        toolbarWrapper.add(toolbarPanel, BorderLayout.CENTER);

        // 创建一个左侧面板来容纳工具栏
        JPanel leftPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 确保左侧面板背景与画布一致
                g.setColor(new Color(245, 245, 250));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        leftPanel.setOpaque(true);
        leftPanel.setBorder(null); // 移除边距，消除缝隙

        // 使工具栏垂直居中
        leftPanel.add(toolbarWrapper, BorderLayout.CENTER);

        // 将视口和工具栏添加到主面板
        mainPanel.add(viewPort, BorderLayout.CENTER);
        mainPanel.add(leftPanel, BorderLayout.WEST);

        return mainPanel;
    }



    /**
     * 绘制连接
     * @param g2d 图形上下文
     */
    private void paintConnections(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Connection conn : connections) {
            drawConnection(g2d, conn.startPoint, conn.endPoint, conn.controlPoint, false);

            if (!conn.label.isEmpty()) {
                drawConnectionLabel(g2d, conn.label, conn);
            }

            // 如果连接线被选中,绘制控制点和虚线
            if (conn.isSelected) {
                drawControlPoint(g2d, conn.controlPoint);
                drawDashedLine(g2d, conn.controlPoint, calculateMidPoint(conn));
            }
        }

        // 绘制临时连接线
        if (isConnecting && startComponent != null && dragEnd != null) {
            Point startPoint = getConnectionPoint(startComponent, new JLayeredPane() {{ setBounds(dragEnd.x, dragEnd.y, 1, 1); }});
            Point controlPoint = new Point((startPoint.x + dragEnd.x) / 2, (startPoint.y + dragEnd.y) / 2);
            drawConnection(g2d, startPoint, dragEnd, controlPoint, true);
        }

        // 只在鼠标附近显示端点
        if (highlightedEndpoint != null) {
            // 获取当前鼠标位置附近的连接线
            Connection nearbyConn = null;
            for (Connection conn : connections) {
                if (conn.startPoint.equals(highlightedEndpoint) ||
                    conn.endPoint.equals(highlightedEndpoint)) {
                    nearbyConn = conn;
                    break;
                }
            }

            if (nearbyConn != null) {
                // 绘制端点光圈效果
                g2d.setColor(new Color(0, 120, 215, 150));
                g2d.setStroke(new BasicStroke(2));
                int size = 12;
                g2d.drawOval(highlightedEndpoint.x - size/2,
                             highlightedEndpoint.y - size/2,
                             size, size);

                // 只绘制当前连接线的两个端点
                g2d.setColor(new Color(0, 120, 215));
                drawEndpoint(g2d, nearbyConn.startPoint);
                drawEndpoint(g2d, nearbyConn.endPoint);
            }
        }
    }

    //框选节点
    private void selectComponentsInRect(boolean isMultiSelect) {
        //System.out.println("开始框选节点，是否多选：" + isMultiSelect);
        if (!isMultiSelect) {
            clearSelection();
        }
        for (Component comp : canvasPanel.getComponents()) {
            if (comp instanceof JLayeredPane) {
                JLayeredPane layeredPane = (JLayeredPane) comp;
                if (selectionRect.intersects(layeredPane.getBounds())) {
                    if (!selectedComponents.contains(layeredPane)) {
                        selectedComponents.add(layeredPane);
                        highlightSelectedComponent(layeredPane);
                        //System.out.println("节点被选中：" + layeredPane.getClientProperty("id"));
                    }
                } else if (!isMultiSelect) {
                    selectedComponents.remove(layeredPane);
                    unhighlightComponent(layeredPane);
                    //System.out.println("节点取消选中：" + layeredPane.getClientProperty("id"));
                }
            }
        }
        //System.out.println("框选完成，当前选中��点数：" + selectedComponents.size());
    }

    // 节点点击
    private void handleNodeClick(JLayeredPane componentAtPoint, MouseEvent e) {
        //System.out.println("点击节点：" + componentAtPoint.getClientProperty("id"));

        if (isOverBorder(componentAtPoint, SwingUtilities.convertPoint(canvasPanel, e.getPoint(), componentAtPoint))) {
            //System.out.println("开始连接操作");
            isConnecting = true;
            startComponent = componentAtPoint;
            dragStart = e.getPoint();
            dragEnd = dragStart;
        } else {
            if (!e.isControlDown()) {
                clearSelection();
            }
            if (!selectedComponents.contains(componentAtPoint)) {
                selectedComponents.add(componentAtPoint);
                highlightSelectedComponent(componentAtPoint);
                //System.out.println("节点被选中：" + componentAtPoint.getClientProperty("id"));
            } else if (e.isControlDown()) {
                selectedComponents.remove(componentAtPoint);
                unhighlightComponent(componentAtPoint);
                //System.out.println("节点取消选中：" + componentAtPoint.getClientProperty("id"));
            }
            //System.out.println("当前选中节点数：" + selectedComponents.size());

            dragStart = e.getPoint();
            componentAtPoint.getParent().setComponentZOrder(componentAtPoint, 0);
        }
        canvasPanel.requestFocusInWindow();
    }

    private void pasteCopiedComponents() {
        if (copiedComponents.isEmpty()) {
            //System.out.println("没有可粘贴的组件");
            return;
        }

        for (ComponentInfo originalInfo : copiedComponents) {
            // 生成新的唯一ID
            String newId = UUID.randomUUID().toString();

            // 设置新位置，稍微偏移以便区分
            Point newLocation = new Point(originalInfo.getX() + 20, originalInfo.getY() + 20);

            // 获取组件类型和图标路径
            String name = originalInfo.getName();
            String iconPath = getIconPathForComponentType(name);
            ComponentItem newItem = PluginCache.componentItemMap.get(originalInfo.getType());
            newItem.setName(name);
            newItem.setIconPath(iconPath);
            // 添加新的组件到画布
            addComponent(newItem, newLocation, newId, false);

            // 如果原组件有连接线，可以选择是否复制连接线
            // 这里不复制连接线，确保粘贴后的组件独立
        }
        PluginCache.sourseCodeUtils.updateSourceCode();
        canvasPanel.revalidate();
        canvasPanel.repaint();
        //System.out.println("已粘贴 " + copiedComponents.size() + " 个组件");
    }

    // 添加新方法来绘制
    private void drawControlPoint(Graphics2D g2d, Point point) {
        int size = 8;
        g2d.setColor(Color.BLUE);
        g2d.fillOval(point.x - size/2, point.y - size/2, size, size);
        g2d.setColor(Color.WHITE);
        g2d.drawOval(point.x - size/2, point.y - size/2, size, size);
    }

    // 添加新方法来绘制虚线
    private void drawDashedLine(Graphics2D g2d, Point start, Point end) {
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g2d.setStroke(dashed);
        g2d.setColor(Color.GRAY);
        g2d.drawLine(start.x, start.y, end.x, end.y);
    }

    private String getIconPathForComponentType(String name) {
        return PluginCache.componentIconMap.getOrDefault(name, "/icons/biz.svg");
    }

    // 添加复制选中组件的方法
    private void copySelectedComponents() {
        copiedComponents.clear();
        for (JLayeredPane component : selectedComponents) {
            String id = (String) component.getClientProperty("id");
            ComponentInfo info = PluginCache.componentInfoMap.get(PluginCache.sourseCodeUtils.getComponentId(id));
            if (info != null) {
                ComponentInfo copiedInfo = new ComponentInfo(id, info.getName(), info.getX(), info.getY(),info.getType());
                copiedComponents.add(copiedInfo);
            }
        }
        System.out.println("已复制 " + copiedComponents.size() + " 个组件");
    }

    /**
     * 创建组件面板
     * @param item 组件项
     * @param id 组件ID
     * @return 创建的组件面板
     */
    public JLayeredPane createComponentPanel(ComponentItem item, String id) {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(90, 70));
        layeredPane.setSize(90, 70);
        layeredPane.setOpaque(false);

        // 修改内容面板为绝对布局
        JPanel contentPanel = new JPanel(null) { // 改为 null 布局
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

                g2d.setColor(new Color(210, 210, 210, 180));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setBounds(0, 0, 90, 70);

        final Icon finalIcon = IconUtils.getIcon(item.getIconPath());;

        Icon largeIcon = null;
        if(item.getIconPath().endsWith(".png")){
            // 处理实际项目资源
            largeIcon = new ImageIcon(item.getIconPath());
        }else{
            largeIcon = new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.scale(2, 2);
                    finalIcon.paintIcon(c, g2d, x / 2, y / 2);
                    g2d.dispose();
                }
                @Override
                public int getIconWidth() {
                    return (int)(finalIcon.getIconWidth() * 2);
                }
                @Override
                public int getIconHeight() {
                    return (int)(finalIcon.getIconHeight() * 2);
                }
            };

        }
        // 创建图标标签并设置固定位置
        JLabel iconLabel = new JLabel(largeIcon);
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        int iconWidth = largeIcon.getIconWidth();
        int iconX = (90 - iconWidth) / 2; // 90是面板宽度
        iconLabel.setBounds(iconX, 2, iconWidth, 45); // 使用计算出的X坐标和实际图标宽度

        // 创建文字标签并设置固定位置
        JLabel nameLabel = new JLabel(item.getName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        nameLabel.setForeground(new Color(60, 60, 60));
        nameLabel.setBounds(2, 47, 86, 20); // 固定文字标签位置和大小

        // 直接添加到内容面板，不使用其他容器
        contentPanel.add(iconLabel);
        contentPanel.add(nameLabel);

        layeredPane.add(contentPanel, JLayeredPane.DEFAULT_LAYER);

        // 存储组件引用
        layeredPane.putClientProperty("id", id);
        layeredPane.putClientProperty("nameLabel", nameLabel);
        layeredPane.putClientProperty("iconLabel", iconLabel);
        layeredPane.putClientProperty("contentPanel", contentPanel);

        addLayeredPaneListeners(layeredPane, id);

        layeredPane.setFocusable(true);
        layeredPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteNode(layeredPane);
                }
            }
        });

        return layeredPane;
    }

    /**
     * 为JLayeredPane添加鼠标监听器
     * @param layeredPane 要添加监听器的JLayeredPane
     * @param id 件的唯一标识符
     */
    private void addLayeredPaneListeners(JLayeredPane layeredPane, String id) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            private Point dragStart;

            @Override
            public void mousePressed(MouseEvent e) {
                if (editingTextField != null && !editingTextField.getBounds().contains(e.getPoint())) {
                    finishEditing();
                }
                //右键
                if (SwingUtilities.isRightMouseButton(e)) {
                    showNodeContextMenu(layeredPane, e.getPoint());
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    //左键
                    if (isOverBorder(layeredPane, e.getPoint())) {
                        System.out.println("开始连接操作");
                        isConnecting = true;
                        startComponent = layeredPane;
                        dragStart = SwingUtilities.convertPoint(layeredPane, e.getPoint(), canvasPanel);
                        dragEnd = dragStart;
                    } else {
                        //System.out.println("开始拖动操作");
                        dragStart = e.getPoint();
                        layeredPane.getParent().setComponentZOrder(layeredPane, 0);

                        // 修改选中逻辑
                        if (!selectedComponents.contains(layeredPane)) {
                            // 如果点击的不是已选中的节点，且没有按住Ctrl键，则清除选择
                            if (!e.isControlDown()) {
                                clearSelection();
                            }
                            selectedComponents.add(layeredPane);
                            highlightSelectedComponent(layeredPane);
                        }
                        // 如果按住Ctrl键点击已选中的节点，则取消选中
                        else if (e.isControlDown()) {
                            selectedComponents.remove(layeredPane);
                            unhighlightComponent(layeredPane);
                        }
                        // 如果直接点击已选中的节点，保持选中状态不变
                    }
                    layeredPane.requestFocusInWindow(); // 请求焦点，使键盘事件生效
                }
                canvasPanel.requestFocusInWindow(); // 确保canvasPanel获得点
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isConnecting) {
                    System.out.println("结束连接操作");
                    JLayeredPane endComponent = getComponentAtPoint(dragEnd);
                    if (endComponent != null && endComponent != startComponent) {
                        createNewConnection(startComponent, endComponent);
                    }
                    isConnecting = false;
                    startComponent = null;
                    dragEnd = null;
                } else if (dragStart != null) {
                    // 如果有选中的组件，更新所有选中组件的位置
                    if (selectedComponents.contains(layeredPane)) {
                        for (JLayeredPane selectedPane : selectedComponents) {
                            String selectedId = (String) selectedPane.getClientProperty("id");
                            updateComponentPosition(selectedId, selectedPane.getLocation());
                        }
                    } else {
                        // 只更新当前组件的位置
                        String id = (String) layeredPane.getClientProperty("id");
                        updateComponentPosition(id, layeredPane.getLocation());
                    }
                    PluginCache.sourseCodeUtils.updateSourceCode();
                }
                dragStart = null;
                dragEnd = null;
                isDraggingControlPoint =false;
                draggedConnection = null;
                canvasPanel.repaint();
                canvasPanel.requestFocusInWindow();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isConnecting) {
                    dragEnd = SwingUtilities.convertPoint(layeredPane, e.getPoint(), canvasPanel);
                    canvasPanel.repaint();
                } else if (dragStart != null) {
                    Point currentPoint = e.getPoint();
                    Point location = layeredPane.getLocation();
                    int dx = currentPoint.x - dragStart.x;
                    int dy = currentPoint.y - dragStart.y;

                    // 如果当前组件在选中列表中，移动所有选中的组件
                    if (selectedComponents.contains(layeredPane)) {
                        for (JLayeredPane selectedPane : selectedComponents) {
                            Point selectedLocation = selectedPane.getLocation();
                            Point newLocation = new Point(selectedLocation.x + dx, selectedLocation.y + dy);
                            selectedPane.setLocation(newLocation);

                            // 更新组件位置信息
                            String selectedId = (String) selectedPane.getClientProperty("id");
                            updateComponentPosition(selectedId, newLocation);
                        }
                    } else {
                        // 如果当前组件不在选中列表中，只移动当前组件
                        Point newLocation = new Point(location.x + dx, location.y + dy);
                        layeredPane.setLocation(newLocation);
                        String id = (String) layeredPane.getClientProperty("id");
                        updateComponentPosition(id, newLocation);
                    }

                    canvasPanel.repaint();
                }
                if (draggedConnection != null) {
                    Point dragEnd = e.getPoint();
                    int dx = dragEnd.x - dragStart.x;
                    int dy = dragEnd.y - dragStart.y;
                    draggedConnection.controlPoint.x += dx;
                    draggedConnection.controlPoint.y += dy;
                    dragStart = dragEnd;
                    canvasPanel.repaint();
                } else if (isSelecting && !isDraggingControlPoint) { // 添加检查
                    int x = Math.min(selectionStart.x, e.getX());
                    int y = Math.min(selectionStart.y, e.getY());
                    int width = Math.abs(selectionStart.x - e.getX());
                    int height = Math.abs(selectionStart.y - e.getY());
                    selectionRect.setBounds(x, y, width, height);
                    canvasPanel.repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                String id = (String) layeredPane.getClientProperty("id");
                //点击 打开数据属性面板
                fireComponentSelected(id);

                if (e.getClickCount() == 2 && !isOverBorder(layeredPane, e.getPoint())) {
                    startEditingComponentName(layeredPane, e.getPoint());
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // 在节���上控制
                if (isOverBorder(layeredPane, e.getPoint())) {
                    //System.out.println("鼠标移动到边界，更改光标样式");
                    layeredPane.setCursor(CONNECT_CURSOR);
                } else {
                    layeredPane.setCursor(Cursor.getDefaultCursor());
                }

                //在连接线上控制
                Connection conn = getConnectionAtPoint(e.getPoint());
                if (conn != null && isNearControlPoint(e.getPoint(), conn)) {
                    canvasPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    canvasPanel.setCursor(Cursor.getDefaultCursor());
                }
                canvasPanel.repaint();
            }
        };


        // 添加新的监听器
        layeredPane.addMouseListener(mouseAdapter);
        layeredPane.addMouseMotionListener(mouseAdapter);

        // 为子组件添加相同的监听器
        for (Component comp : layeredPane.getComponents()) {
            comp.addMouseListener(mouseAdapter);
            comp.addMouseMotionListener(mouseAdapter);
        }
        //System.out.println("已为组件 " + id + " 添加鼠监听器");
    }

    // 修改 startEditingComponentName 方法
    private void startEditingComponentName(JLayeredPane layeredPane, Point clickPoint) {
        if (editingTextField != null) {
            finishEditing();
        }

        JLabel nameLabel = (JLabel) layeredPane.getClientProperty("nameLabel");
        if (nameLabel != null) {
            String currentName = nameLabel.getText();
            editingTextField = new JTextField(currentName);

            // 直接使用nameLabel的bounds，因为现在使用的是绝对布局
            Rectangle bounds = nameLabel.getBounds();
            editingTextField.setBounds(bounds);

            editingTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 120, 215)),
                BorderFactory.createEmptyBorder(1, 3, 1, 3)
            ));
            editingTextField.setFont(nameLabel.getFont());
            editingTextField.setHorizontalAlignment(JTextField.CENTER);

            editingTextField.addActionListener(e -> finishEditing());
            editingTextField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    finishEditing();
                }
            });

            nameLabel.setVisible(false);
            layeredPane.add(editingTextField, JLayeredPane.POPUP_LAYER);
            editingTextField.requestFocus();
            editingTextField.selectAll();
        }
    }

    // 修改 finishEditing 方法
    private void finishEditing() {
        if (editingTextField != null) {
            if (editingConnection != null) {
                // 处理连接线标签编辑
                String newLabel = editingTextField.getText();
                editingConnection.label = newLabel;
                PluginCache.sourseCodeUtils.updateSourceCode();
                canvasPanel.remove(editingTextField);
                editingTextField = null;
                editingConnection = null;
                canvasPanel.revalidate();
                canvasPanel.repaint();
            } else {
                // 处理组件名称编辑
                String newName = editingTextField.getText();
                JLayeredPane parentPane = (JLayeredPane) editingTextField.getParent();
                JLabel nameLabel = (JLabel) parentPane.getClientProperty("nameLabel");
                String id = (String) parentPane.getClientProperty("id");

                if (nameLabel != null && id != null) {
                    // 更新标签文本
                    nameLabel.setText(newName);
                    nameLabel.setVisible(true);

                    // 更新组件信息
                    updateComponentName(id, newName);

                    // 移除编辑框
                    parentPane.remove(editingTextField);
                    editingTextField = null;

                    // 更新源代码
                    PluginCache.sourseCodeUtils.updateSourceCode();

                    // 只重绘父面板
                    parentPane.repaint();
                }
            }
        }
    }

    private void createNewConnection(JLayeredPane startComponent, JLayeredPane endComponent) {
        ComponentInfo startInfo = PluginCache.componentInfoMap.get(PluginCache.sourseCodeUtils.getComponentId(String.valueOf(startComponent.getClientProperty("id"))));
        ComponentInfo endInfo = PluginCache.componentInfoMap.get(PluginCache.sourseCodeUtils.getComponentId(String.valueOf(endComponent.getClientProperty("id"))));

        // 判断组件类型和连接规则
        if (!isValidConnection(startInfo, endInfo)) {
            showInvalidConnectionMessage();
            return;
        }

        Point startPoint = getConnectionPoint(startComponent, endComponent);
        Point endPoint = getConnectionPoint(endComponent, startComponent);

        Connection connection = new Connection(startInfo, endInfo, startPoint, endPoint);
        connections.add(connection);
        PluginCache.sourseCodeUtils.updateSourceCode();
        canvasPanel.repaint();
    }

    // 添加新方法用于判断连接是否有效
    private boolean isValidConnection(ComponentInfo startInfo, ComponentInfo endInfo) {
        if (startInfo == null || endInfo == null) {
            return false;
        }

        // 判断开始组件
        if ("start".equals(endInfo.getType())) {
            // 不允许连接到开始组件
            return false;
        }

        // 判断结束组件
        if ("end".equals(startInfo.getType())) {
            // 不允许从结束组件发起连接
            return false;
        }

        return true;
    }

    // 添加新方法用于显示无效连接提示
    private void showInvalidConnectionMessage() {
        SwingUtilities.invokeLater(() -> {
            // 创建消息对话框
            JOptionPane pane = new JOptionPane(
                "无效的连接：\n- 开始组件不能作为连接目标\n- 结束组件不能作为连接源",
                JOptionPane.WARNING_MESSAGE
            );

            // 创建对话框并设置标题
            JDialog dialog = pane.createDialog(canvasPanel, "无效连接");

            // 获取父窗口的位置和大小
            Window parent = SwingUtilities.getWindowAncestor(canvasPanel);
            if (parent != null) {
                // 计算对话框居中位置
                int x = parent.getX() + (parent.getWidth() - dialog.getWidth()) / 2;
                int y = parent.getY() + (parent.getHeight() - dialog.getHeight()) / 2;
                dialog.setLocation(x, y);
            }

            // 显示对话框
            dialog.setVisible(true);
        });
    }

    private Point getConnectionPoint(JLayeredPane from, JLayeredPane to) {
        Rectangle fromBounds = from.getBounds();
        Point fromCenter = new Point(fromBounds.x + fromBounds.width / 2, fromBounds.y + fromBounds.height / 2);
        Point toCenter = new Point(to.getBounds().x + to.getBounds().width / 2, to.getBounds().y + to.getBounds().height / 2);

        double angle = Math.atan2(toCenter.y - fromCenter.y, toCenter.x - fromCenter.x);

        int x, y;
        if (Math.abs(Math.cos(angle)) > Math.abs(Math.sin(angle))) {
            // 连接点在左右边
            x = (Math.cos(angle) > 0) ? fromBounds.x + fromBounds.width : fromBounds.x;
            y = fromCenter.y + (int) (Math.tan(angle) * (x - fromCenter.x));
        } else {
            // 连接点在上下边
            y = (Math.sin(angle) > 0) ? fromBounds.y + fromBounds.height : fromBounds.y;
            x = fromCenter.x + (int) ((y - fromCenter.y) / Math.tan(angle));
        }

        return new Point(x, y);
    }


    /**
     * 获取指定点上的组件
     * @param point 指定点
     * @return 组
     */
    private JLayeredPane getComponentAtPoint(Point point) {
        for (Component comp : canvasPanel.getComponents()) {
            if (comp instanceof JLayeredPane && comp.getBounds().contains(point)) {
                return (JLayeredPane) comp;
            }
        }
        return null;
    }

    /**
     * 更新组件名
     * @param id 组件ID
     * @param newName 新名称
     */
    private void updateComponentName(String id, String newName) {
        for (ComponentInfo info : components) {
            if (info.getId().equals(id)) {
                info.setName(newName);
                break;
            }
        }
    }

    /**
     * 更新组件位置
     * @param id 组件ID
     * @param newLocation 新位置
     */
    private void updateComponentPosition(String id, Point newLocation) {
        for (ComponentInfo info : components) {
            if (info.getId().equals(id)) {
                info.setX(newLocation.x);
                info.setY(newLocation.y);
                updateConnectionsForComponent(info);
                break;
            }
        }
        canvasPanel.repaint();
    }

    private void updateConnectionsForComponent(ComponentInfo info) {
        for (Connection conn : connections) {
            if (conn.start.getId().equals(info.getId()) || conn.end.getId().equals(info.getId())) {
                JLayeredPane startComponent = (JLayeredPane) getComponentByInfo(conn.start);
                JLayeredPane endComponent = (JLayeredPane) getComponentByInfo(conn.end);
                if (startComponent != null && endComponent != null) {
                    conn.startPoint = getConnectionPoint(startComponent, endComponent);
                    conn.endPoint = getConnectionPoint(endComponent, startComponent);
                }
            }
        }
    }


    /**
     * 根据组件信息获取组件
     * @param info 组件信息
     * @return 组件
     */
    private Component getComponentByInfo(ComponentInfo info) {
        for (Component comp : canvasPanel.getComponents()) {
            if (comp instanceof JLayeredPane) {
                String id = (String) ((JLayeredPane) comp).getClientProperty("id");
                if (id != null && id.equals(info.getId())) {
                    return comp;
                }
            }
        }
        return null;
    }


    private void drawConnection(Graphics2D g2d, Point start, Point end, Point control, boolean isTemporary) {
        // 临时连接线(拖动时的虚线)
        if (isTemporary) {
            g2d.setColor(new Color(255, 0, 0, 150));
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{9}, 0));
            g2d.drawLine(start.x, start.y, end.x, end.y);
            return;
        }

        // 正式连接线
        // 先画一个宽的半透明线条作为底层
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(start.x, start.y, end.x, end.y);

        // 查找当前连接线对象
        Connection currentConn = null;
        for (Connection conn : connections) {
            if (conn.startPoint.equals(start) && conn.endPoint.equals(end)) {
                currentConn = conn;
                break;
            }
        }

        // 设置连接线颜色
        if (currentConn != null && currentConn.getExpression() != null && !currentConn.getExpression().isEmpty()) {
            g2d.setColor(EXPRESSION_LINE_COLOR); // 有表达式时使用紫色
        } else {
            g2d.setColor(new Color(0, 120, 215)); // 没有表达式时使用原来的蓝色
        }

        // 绘制主线
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(start.x, start.y, end.x, end.y);

        // 绘制箭头
        double angle = Math.atan2(end.y - start.y, end.x - start.x);
        drawArrowHead(g2d, end, angle);
    }

    private void drawArrow(Graphics2D g2d, Path2D path) {
        double[] coords = new double[6];
        PathIterator pi = path.getPathIterator(null, 0.01);
        pi.next(); // 跳过起点

        double lastX = 0, lastY = 0;
        double endX = 0, endY = 0;

        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            double x = coords[0];
            double y = coords[1];

            if (type == PathIterator.SEG_LINETO || type == PathIterator.SEG_CLOSE) {
                lastX = endX;
                lastY = endY;
                endX = x;
                endY = y;
            }

            pi.next();
        }

        // 计算箭方向
        double angle = Math.atan2(endY - lastY, endX - lastX);
        drawArrowHead(g2d, new Point((int)endX, (int)endY), angle);
    }

    private void drawArrowHead(Graphics2D g2d, Point tip, double angle) {
        int arrowSize = 10;

        Path2D arrow = new Path2D.Double();
        arrow.moveTo(tip.x, tip.y);
        arrow.lineTo(tip.x - arrowSize * Math.cos(angle - Math.PI / 6),
                tip.y - arrowSize * Math.sin(angle - Math.PI / 6));
        arrow.lineTo(tip.x - arrowSize * Math.cos(angle + Math.PI / 6),
                tip.y - arrowSize * Math.sin(angle + Math.PI / 6));
        arrow.closePath();

        g2d.fill(arrow);
    }


    private Connection getConnectionAtPoint(Point point) {
        for (Connection conn : connections) {
            if (isPointNearConnection(point, conn)) {
                return conn;
            }
        }
        return null;
    }

    private boolean isPointNearConnection(Point point, Connection conn) {
        // 创建一个较宽的线段区域用于检测点击
        BasicStroke stroke = new BasicStroke(CONNECTION_CLICK_TOLERANCE * 2);
        Path2D path = new Path2D.Double();
        path.moveTo(conn.startPoint.x, conn.startPoint.y);
        path.lineTo(conn.endPoint.x, conn.endPoint.y);

        Shape shape = stroke.createStrokedShape(path);
        return shape.contains(point) || isNearControlPoint(point, conn);
    }

    private void startEditingConnectionLabel(Connection connection, Point point) {
        if (editingTextField != null) {
            finishEditing();
        }

        editingConnection = connection;

        // 使用新的中点计算方法
        Point midPoint = calculateMidPoint(connection);

        editingTextField = new JTextField(connection.label);
        editingTextField.setBounds(midPoint.x - 50, midPoint.y - 10, 100, 20);
        editingTextField.setBorder(BorderFactory.createLineBorder(Color.BLUE));
        editingTextField.addActionListener(e -> finishEditing());
        editingTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                finishEditing();
            }
        });

        canvasPanel.add(editingTextField);
        editingTextField.requestFocus();
        canvasPanel.revalidate();
        canvasPanel.repaint();
    }

    private void showDeleteConnectionMenu(Connection connection, Point point) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));


        // 添加表达式配置菜单项
        JMenuItem expressionItem = new JMenuItem("组件配置");
        expressionItem.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        expressionItem.setIcon(IconLoader.getIcon("/icons/settings.svg", VisualLayoutPanel.class));
        expressionItem.addActionListener(e -> showExpressionEditor(connection));

        JMenuItem deleteItem = new JMenuItem("删除连接线");
        deleteItem.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        deleteItem.setIcon(IconLoader.getIcon("/icons/delete.svg", VisualLayoutPanel.class));
        deleteItem.addActionListener(e -> {
            connections.remove(connection);
            PluginCache.sourseCodeUtils.updateSourceCode();
            canvasPanel.repaint();
        });

        JMenuItem editItem = new JMenuItem("编辑标签");
        editItem.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        editItem.setIcon(IconLoader.getIcon("/icons/edit.svg", VisualLayoutPanel.class));
        editItem.addActionListener(e -> startEditingConnectionLabel(connection, point));

        popupMenu.add(expressionItem);
        popupMenu.add(editItem);
        popupMenu.add(deleteItem);
        popupMenu.show(canvasPanel, point.x, point.y);
    }

    // 添加新方法用于显示表达式编辑器
    private void showExpressionEditor(Connection connection) {
        ExpressionEditorDialog dialog = new ExpressionEditorDialog(
            project,
            connection.getExpression(),
            connection.getExpressionLanguage(),
            this.currentFile
        );

        if (dialog.showAndGet()) {
            connection.setExpression(dialog.getExpression());
            connection.setExpressionLanguage(dialog.getLanguage());
            PluginCache.sourseCodeUtils.updateSourceCode();
            // 立即重绘画布以显示新的连接线颜色
            canvasPanel.repaint();
        }
    }

    private void drawConnectionLabel(Graphics2D g2d, String label, Connection connection) {
        if (label.isEmpty()) {
            return;
        }

        // 使用新的中点计算方法
        Point midPoint = calculateMidPoint(connection);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textHeight = fm.getHeight();
        int padding = 4;

        int labelX = midPoint.x - textWidth / 2;
        int labelY = midPoint.y - textHeight / 2;

        g2d.fillRoundRect(labelX - padding, labelY - padding,
                textWidth + 2 * padding,
                textHeight + 2 * padding,
                5, 5);

        g2d.setColor(Color.BLACK);
        g2d.drawString(label, labelX, labelY + fm.getAscent());
    }


    private Point calculateMidPoint(Connection connection) {
        // 直接计算起点和终点的中点
        int x = (connection.startPoint.x + connection.endPoint.x) / 2;
        int y = (connection.startPoint.y + connection.endPoint.y) / 2;
        return new Point(x, y);
    }

    private boolean isOverBorder(JLayeredPane layeredPane, Point point) {
        int borderWidth = 5;
        Rectangle bounds = new Rectangle(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        Rectangle innerBounds = new Rectangle(borderWidth, borderWidth,
                layeredPane.getWidth() - 2 * borderWidth,
                layeredPane.getHeight() - 2 * borderWidth);
        return bounds.contains(point) && !innerBounds.contains(point);
    }

    private void showNodeContextMenu(JLayeredPane layeredPane, Point point) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        String id = (String) layeredPane.getClientProperty("id");
        ComponentInfo info = PluginCache.componentInfoMap.get(PluginCache.sourseCodeUtils.getComponentId(id));

        JMenuItem editItem = new JMenuItem("编辑节点");
        editItem.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        editItem.setIcon(IconLoader.getIcon("/icons/edit.svg", VisualLayoutPanel.class));
        editItem.addActionListener(e -> startEditingComponentName(layeredPane, point));

        JMenuItem copyItem = new JMenuItem("复制节点");
        copyItem.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        copyItem.setIcon(IconLoader.getIcon("/icons/copy.svg", VisualLayoutPanel.class));
        copyItem.addActionListener(e -> copyNode(layeredPane));

        // 修改删除菜单项的处理逻辑
        JMenuItem deleteItem = new JMenuItem(selectedComponents.size() > 1 ?
            "删除选中节点(" + selectedComponents.size() + ")" : "删除节点");
        deleteItem.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        deleteItem.setIcon(IconLoader.getIcon("/icons/delete.svg", VisualLayoutPanel.class));
        deleteItem.addActionListener(e -> {
            // 如果有选中的节点,则删除所有选中的节点
            if (!selectedComponents.isEmpty()) {
                deleteSelectedNodes();
            } else {
                // 如果没有选中的节点,则只删除当前右键的节点
                deleteNode(layeredPane);
                PluginCache.sourseCodeUtils.updateSourceCode();
                canvasPanel.revalidate();
                canvasPanel.repaint();
            }
        });

        // 添加配置菜单，仅对特定类型组件显示
        if (info != null && ("flow-print".equals(info.getType()) || "flow-sql".equals(info.getType()) || "flow-assign".equals(info.getType())
        || "flow-date".equals(info.getType()) || "flow-base64".equals(info.getType()) || "flow-number".equals(info.getType())
        || "flow-random".equals(info.getType()) || "flow-uniqueId".equals(info.getType()) || "flow-sys_config".equals(info.getType())
        || "flow-custom-refer".equals(info.getType()) || "flow-exception".equals(info.getType()) || "flow-groovy".equals(info.getType())
         || "flow-type2type".equals(info.getType()) || info.getType().startsWith("custom-") )) {
            JMenuItem configItem = new JMenuItem("组件配置");
            configItem.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            configItem.setIcon(IconLoader.getIcon("/icons/settings.svg", VisualLayoutPanel.class));
            configItem.addActionListener(e -> showComponentSettings(layeredPane));

            popupMenu.add(configItem);
            popupMenu.addSeparator(); // 添加分隔线
        }

        popupMenu.add(editItem);
        popupMenu.add(copyItem);
        popupMenu.add(deleteItem);
        popupMenu.show(layeredPane, point.x, point.y);
    }

    // 添加新的方法来删除节点
    private void deleteNode(JLayeredPane layeredPane) {
        String id = (String) layeredPane.getClientProperty("id");

        boolean f = components.removeIf(info -> info.getId().equals(id));
        connections.removeIf(conn -> conn.start.getId().equals(id) || conn.end.getId().equals(id));
        canvasPanel.remove(layeredPane);
        selectedComponents.remove(layeredPane);
    }

    // 添加新的方法: 判断点是否在控制点附近
    private boolean isNearControlPoint(Point point, Connection connection) {
        return point.distance(connection.controlPoint) <= 10;
    }

    private ComponentInfo deepClone(ComponentInfo obj) {
        try {
            // 序列化
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            
            // 反序列化
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (ComponentInfo) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    

    // 添加新的方法来复制节点
    private void copyNode(JLayeredPane originalLayeredPane) {
        String originalId = (String) originalLayeredPane.getClientProperty("id");
        ComponentInfo originalInfo = PluginCache.componentInfoMap.get(PluginCache.sourseCodeUtils.getComponentId(originalId));
        //将原始节点信息复制到新的节点
        ComponentInfo cloneObj = deepClone(originalInfo);

        if (originalInfo != null) {
            // 创建新的唯一ID
            String newId = UUID.randomUUID().toString();
            cloneObj.setId(newId);
            PluginCache.componentInfoMap.put(PluginCache.sourseCodeUtils.getComponentId(newId), cloneObj);
            // 创建新的位置（稍微偏移一点，以用户能看到新节点）
            Point newLocation = new Point(originalInfo.getX() + 20, originalInfo.getY() + 20);

            // 获取原始节点的图标
            JLabel nameLabel = (JLabel) originalLayeredPane.getClientProperty("nameLabel");
            String name = nameLabel.getText();

            ComponentItem newItem = PluginCache.componentItemMap.get(originalInfo.getType());
            newItem.setIconPath(newItem.getIconPath());
            newItem.setName(name);
            // 添加新的组件到画布
            addComponent(newItem, newLocation, newId,false);

            // 更新源代码
            PluginCache.sourseCodeUtils.updateSourceCode();

            // 重绘画布
            canvasPanel.revalidate();
            canvasPanel.repaint();
        }
    }

    /**
     * 添加新组件到画布
     * @param item 组件项
     * @param location 位置
     */
    public void addComponent(ComponentItem item, Point location, String id, boolean isNewDrag) {
        //System.out.println("正在添加组件: ID = " + id + ", 类型 = " + item.getName() + ", 位置 = " + location);
        JLayeredPane componentPanel = createComponentPanel(item, id);

        if (isNewDrag) {
            // 对于新拖拽的组件，调整位置使鼠标在组件中心
            int adjustedX = location.x - componentPanel.getWidth() / 2;
            int adjustedY = location.y - componentPanel.getHeight() / 2;
            componentPanel.setLocation(adjustedX, adjustedY);
            location = new Point(adjustedX, adjustedY);
        } else {
            // 对于加载的组件，直接使用传入的位置
            componentPanel.setLocation(location);
        }

        canvasPanel.add(componentPanel);

        ComponentInfo newInfo = PluginCache.componentInfoMap.get(id);
        if(newInfo != null){ //复制组件
            newInfo.setX(location.x);
            newInfo.setY(location.y);
            PluginCache.componentInfoMap.put(PluginCache.sourseCodeUtils.getComponentId(id), newInfo);

        }else{ //拖拽新增组件, 只有一些基本属性
            newInfo = new ComponentInfo(id, item.getName(), location.x, location.y,item.getType());
            newInfo = initNewComponentInfo(newInfo);
            // 不设置相当于没有任何作用
            PluginCache.componentInfoMap.put(PluginCache.sourseCodeUtils.getComponentId(id), newInfo);
        }
        components.add(newInfo);
        System.out.println("组件已添加到画布: ID = " + id + ", 位置 = " + location);

        ApplicationManager.getApplication().invokeLater(PluginCache.sourseCodeUtils::updateSourceCode);
        canvasPanel.revalidate();
        canvasPanel.repaint();
    }

    //创建新组件同步
    private ComponentInfo initNewComponentInfo(ComponentInfo newInfo) {
        ComponentProp prop = new ComponentProp();
        ComponentItem componentItem = PluginCache.componentItemMap.get(newInfo.getType());
        if("print".equals(newInfo.getType())){
            prop.setBeanRef(componentItem.getBeanRef());
            prop.setMethod(componentItem.getMethod());
            prop.setThreadType(ThreadType.SYNC);
        }
        if("assign".equals(newInfo.getType())){
            prop.setBeanRef(componentItem.getBeanRef());
            prop.setMethod(componentItem.getMethod());
            prop.setThreadType(ThreadType.SYNC);
        }
        if("sql".equals(newInfo.getType())){
            prop.setReturnType(ReturnType.MAP);
        }
        newInfo.setComponentProp(prop);
        return newInfo;
    }

    // 仅添加到面板上
    public void addComponentFromSourceCode(ComponentItem item, Point location, String id, boolean isNewDrag) {
        //System.out.println("正在添加组件: ID = " + id + ", 类型 = " + item.getName() + ", 位置 = " + location);
        JLayeredPane componentPanel = createComponentPanel(item, id);

        if (isNewDrag) {
            // 对于新拖拽的组件，调整位置使鼠标在组件中心
            int adjustedX = location.x - componentPanel.getWidth() / 2;
            int adjustedY = location.y - componentPanel.getHeight() / 2;
            componentPanel.setLocation(adjustedX, adjustedY);
        } else {
            // 对于加载的组件，直接使用传入的位置
            componentPanel.setLocation(location);
        }
        canvasPanel.add(componentPanel);
    }


    private void highlightSelectedComponent(JLayeredPane component) {
        //System.out.println("高亮显示节点：" + component.getClientProperty("id"));

        JPanel contentPanel = (JPanel) component.getClientProperty("contentPanel");
        if (contentPanel != null) {
            // 设置蓝色虚线边框
            contentPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 120, 215), 2),
                    BorderFactory.createDashedBorder(new Color(0, 120, 215), 2, 1)
            ));
        }

        component.setBackground(new Color(173, 216, 230, 50));
        component.setOpaque(true);

        // 将选中的组件移到最前层，确保边框可见
        canvasPanel.setComponentZOrder(component, 0);

        // 重新布局和重绘组件以应用边框
        component.revalidate();
        component.repaint();
        //System.out.println("节点高亮显示完成");
    }

    private void unhighlightComponent(JLayeredPane component) {
        //System.out.println("取消高亮显示节点：" + component.getClientProperty("id"));

        JPanel contentPanel = (JPanel) component.getClientProperty("contentPanel");
        if (contentPanel != null) {
            // 移除边框
            contentPanel.setBorder(null);
        }

        component.setBackground(null);
        component.setOpaque(false);

        // 重新布局和重绘组件以移除边框
        component.revalidate();
        component.repaint();
        //System.out.println("节点取消高亮显示完成");
    }

    private void deleteSelectedNodes() {
        if (!selectedComponents.isEmpty()) {
            //System.out.println("开始删除 " + selectedComponents.size() + " 个选中的节���");
            List<JLayeredPane> componentsToDelete = new ArrayList<>(selectedComponents);
            for (JLayeredPane component : componentsToDelete) {
                deleteNode(component);
            }
            selectedComponents.clear();
            PluginCache.sourseCodeUtils.updateSourceCode();
            canvasPanel.revalidate();
            canvasPanel.repaint();
            System.out.println("节点删除完成");
        } else {
            System.out.println("没有选中的节点可删除");
        }
    }

    // 添加新方法来清除选择
    private void clearSelection() {
        for (JLayeredPane component : selectedComponents) {
            unhighlightComponent(component);
        }
        selectedComponents.clear();
    }

    public List<Connection> getConnections(){
        return connections;
    }
    public List<ComponentInfo> getComponents(){
        return components;
    }
    public JPanel getLayeredPane() {
        return this.mainPanel;
    }

    public JPanel getCanvasPanel() {
        return this.canvasPanel; // 返回 canvasPanel 而不是 viewPort
    }

    private void showComponentSettings(JLayeredPane layeredPane) {
        if (project.isDisposed()) {
            return;
        }

        String id = (String) layeredPane.getClientProperty("id");
        ComponentInfo info = PluginCache.componentInfoMap.get(PluginCache.sourseCodeUtils.getComponentId(id));
        

        //打印组件
        if (info != null && "flow-print".equals(info.getType())) {
            PrintComponentSettingsDialog dialog = new PrintComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel, info.getType(), currentFile);
            dialog.setModal(false); // 设置为非模态
            dialog.show();
        }
        if (info != null && "flow-assign".equals(info.getType())) {
            AssignComponentSettingsDialog dialog = new AssignComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel, info.getType(), currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        if (info != null && "flow-sql".equals(info.getType())) {
            SQLComponentSettingsDialog dialog = new SQLComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel, info.getType(), currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        if (info != null && "flow-groovy".equals(info.getType())) {
            GroovyComponentSettingsDialog dialog = new GroovyComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel,info.getType(),currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        if (info != null && "flow-date".equals(info.getType())) {
            DateComponentSettingsDialog dialog = new DateComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel,info.getType(),currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        if (info != null && "flow-base64".equals(info.getType())) {
            Base64ComponentSettingsDialog dialog = new Base64ComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel,info.getType(),currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        if (info != null && "flow-number".equals(info.getType())) {
            NumberComponentSettingsDialog dialog = new NumberComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel,info.getType(),currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        if (info != null && "flow-type2type".equals(info.getType())) {
            Type2TypeComponentSettingsDialog dialog = new Type2TypeComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel,info.getType(),currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        if (info != null && "flow-random".equals(info.getType())) {
            RandomComponentSettingsDialog dialog = new RandomComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel,info.getType(),currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        if (info != null && "flow-uniqueId".equals(info.getType())) {
            UniqueIdComponentSettingsDialog dialog = new UniqueIdComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel,info.getType(),currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        if (info != null && "flow-sys_config".equals(info.getType())) {
            SysConfigComponentSettingsDialog dialog = new SysConfigComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel,info.getType(),currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        if (info != null && "flow-custom-refer".equals(info.getType())) {
            CustomReferComponentSettingsDialog dialog = new CustomReferComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel,info.getType(),currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        if (info != null && "flow-exception".equals(info.getType())) {
            ExceptionComponentSettingsDialog dialog = new ExceptionComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel,info.getType(),currentFile);
            dialog.setModal(false);
            dialog.show();
        }
        //针对于自定义组件, 则统一规则
        if (info != null && info.getType().startsWith("custom-")) {
            CustomComponentSettingsDialog dialog = new CustomComponentSettingsDialog(id, PluginCache.sourseCodeUtils.getComponentId(id), project, canvasPanel,info.getType(),currentFile, info.getName());
            dialog.setModal(false);
            dialog.show();
        }
    }

    // 在类销毁时关闭异步任务
    public void dispose() {
        // 清理所有组件和连接
        components.clear();
        connections.clear();

        // 移除所有画布上的组件
        if (canvasPanel != null) {
            canvasPanel.removeAll();
        }

        // 停止异步任务
        AsyncTaskManager.getInstance().stopSpringBeanLoadTask();
    }

    public void centerComponent(Component component) {
        if (component != null) {
            // 获取滚动面板
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
                JScrollPane.class,
                containerPanel
            );

            if (scrollPane != null) {
                // 获取组件的位置和大小
                Rectangle bounds = component.getBounds();
                Rectangle viewRect = scrollPane.getViewport().getViewRect();

                // 计算滚动位置，使组件位于视图中央
                int centerX = bounds.x - (viewRect.width - bounds.width) / 2;
                int centerY = bounds.y - (viewRect.height - bounds.height) / 2;

                // 确保滚动位置在有效范围内
                centerX = Math.max(0, Math.min(centerX, containerPanel.getWidth() - viewRect.width));
                centerY = Math.max(0, Math.min(centerY, containerPanel.getHeight() - viewRect.height));

                // 设置滚动位置
                scrollPane.getViewport().setViewPosition(new Point(centerX, centerY));

                // 重绘
                containerPanel.revalidate();
                containerPanel.repaint();
            }
        }
    }

    // 获取所有组件的方法
    public List<Component> getAllComponents() {
        List<Component> components = new ArrayList<>();
        // 只收集画布面板上的组件
        for (Component comp : canvasPanel.getComponents()) {
            if (comp instanceof JLayeredPane) {
                JLabel nameLabel = (JLabel) ((JLayeredPane) comp).getClientProperty("nameLabel");
                if (nameLabel != null) {
                    comp.setName(nameLabel.getText()); // 设置组件名称为标签文本
                    components.add(comp);
                }
            }
        }
        return components;
    }

    private JPanel createToolbar() {
        // 创建主工具栏面板
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        // 使用与画布相同的背景色
        toolbar.setBackground(new Color(245, 245, 250));

        // 创建内部工具栏面板
        JPanel innerToolbar = new JPanel();
        innerToolbar.setLayout(new BoxLayout(innerToolbar, BoxLayout.Y_AXIS));
        innerToolbar.setBackground(toolbar.getBackground());

        // 添加顶部弹性空间实现垂直居中
        toolbar.add(Box.createVerticalGlue());

        // 设置内边距和阴影
        innerToolbar.setBorder(BorderFactory.createCompoundBorder(
                new ShadowBorder(),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // 创建工具按钮
        JToggleButton selectButton = createToolButton("/icons/select.svg", "选择工具", SELECT_TOOL);
        JToggleButton panButton = createToolButton("/icons/move.svg", "平移工具", PAN_TOOL);

        // 创建小地图按钮
        JToggleButton miniMapButton = createMiniMapButton();

        // 默认选中选择工具
        panButton.setBackground(new Color(0, 120, 215, 20));
        selectedButton = panButton;

        // 添加按钮到内部工具栏
        innerToolbar.add(panButton);
        innerToolbar.add(Box.createVerticalStrut(4));
        innerToolbar.add(selectButton);
        innerToolbar.add(Box.createVerticalStrut(4));
        innerToolbar.add(miniMapButton);  // 添加小地图按钮
        innerToolbar.add(Box.createVerticalStrut(4));
        //存档工具
        JToggleButton cunDangButton =  createCunDangButton();
        innerToolbar.add(cunDangButton);  // 添加存档按钮
        innerToolbar.add(Box.createVerticalStrut(4));

        //=======================保存按钮===========================
        Icon icon = IconLoader.getIcon("/icons/save.svg", VisualLayoutPanel.class);
        JToggleButton saveButton = ToggleButtonUtils.createToggleButtonNoText("保存", icon);
        innerToolbar.add(saveButton);
        addSaveEvent(saveButton);
        //=======================保存按钮===========================
        // 将内部工具栏添加到主工具栏
        toolbar.add(innerToolbar);
        // 添加底部弹性空间实现垂直居中
        toolbar.add(Box.createVerticalGlue());
        // 设置工具栏的首选大小
        toolbar.setPreferredSize(new Dimension(56, toolbar.getPreferredSize().height));

        return toolbar;
    }

    private void addSaveEvent(JToggleButton button){
        //全局保存
        button.addActionListener(e -> {
            PluginCache.sourseCodeUtils.updateSourceCode();
            AlertUtils.alertOnAbove(button,"保存流程成功");
            ToggleButtonUtils.reset(button);
        });
        // 添加鼠标悬停效果
        button.addMouseListener(new MouseOverBtnAdapter(button));
    }

    // 自定义阴影边框类
    private static class ShadowBorder extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制圆角矩形背景，使用白色半透明背景
            int radius = 6;
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fill(new RoundRectangle2D.Float(x, y, width-1, height-1, radius, radius));

            // 绘制更细腻的阴影效果
            for (int i = 0; i < 3; i++) {
                g2.setColor(new Color(0, 0, 0, 6 - i * 2));
                g2.draw(new RoundRectangle2D.Float(x + i, y + i, width - 1 - i*2, height - 1 - i*2, radius, radius));
            }

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(3, 3, 3, 3);
        }
    }
    private JToggleButton createToolButton(String iconPath, String tooltip, String tool) {
        Icon icon = IconLoader.getIcon(iconPath, VisualLayoutPanel.class);
        JToggleButton button = ToggleButtonUtils.createToggleButtonNoText(tooltip, icon);
        // 修改点击效果处理
        button.addActionListener(e -> {
            if (selectedButton != null && selectedButton != button) {
                ((JToggleButton)selectedButton).setSelected(false);
                selectedButton.setBackground(new Color(255, 255, 255, 0));
                selectedButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            }

            if (button.isSelected()) {
                button.setBackground(new Color(0, 120, 215, 20));
                button.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(4, new Color(0, 120, 215, 40)),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)
                ));
                selectedButton = button;
                currentTool = tool;

                if (PAN_TOOL.equals(tool)) {
                    isPanning = true;
                    viewPort.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else {
                    isPanning = false;
                    viewPort.setCursor(Cursor.getDefaultCursor());
                }
            } else {
                button.setBackground(new Color(255, 255, 255, 0));
                button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                if (selectedButton == button) {
                    selectedButton = null;
                }
                // 重置工具状态
                currentTool = SELECT_TOOL; // 默认使用选择工具
                isPanning = false;
                viewPort.setCursor(Cursor.getDefaultCursor());
            }
        });

        // 添加鼠标悬停效果
        button.addMouseListener(new MouseOverBtnAdapter(button));

        return button;
    }

    // 自定义圆角边框类（保持不变）
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x, y, width-1, height-1, radius, radius));
            g2.dispose();
        }
    }

    // 添加新方法:检测端点
    private Point getEndpointAtPoint(Point point) {
        int tolerance = 8; // 端点检测范围
        for (Connection conn : connections) {
            if (point.distance(conn.startPoint) <= tolerance) {
                return conn.startPoint;
            }
            if (point.distance(conn.endPoint) <= tolerance) {
                return conn.endPoint;
            }
        }
        return null;
    }

    // 添加新方法:绘制端点
    private void drawEndpoint(Graphics2D g2d, Point point) {
        int size = 6;
        g2d.fillOval(point.x - size/2, point.y - size/2, size, size);
    }

    private JToggleButton createMiniMapButton() {
        miniMapButton = new JToggleButton();
        miniMapButton.setUI(new CustomCanvasButtonUI());
        Icon icon = IconLoader.getIcon("/icons/eye.svg", VisualLayoutPanel.class);
        miniMapButton.setIcon(icon);
        miniMapButton.setFocusPainted(false);
        miniMapButton.addActionListener(e -> {
            if (miniMapDialog == null) {
                // 获取正确的 Frame 类型父窗口
                Window window = SwingUtilities.getWindowAncestor(mainPanel);
                Frame frame = null;
                if (window instanceof Frame) {
                    frame = (Frame) window;
                } else {
                    // 如果找不到Frame，则使用新的Frame
                    frame = new JFrame();
                }

                miniMapDialog = new MiniMapDialog(frame, this);

                // 设置对话框位置
                try {
                    Point loc = mainPanel.getLocationOnScreen();
                    Dimension mainSize = mainPanel.getSize();
                    Dimension dialogSize = miniMapDialog.getSize();
                    miniMapDialog.setLocation(
                        loc.x + mainSize.width - dialogSize.width - 20,
                        loc.y + 20
                    );
                } catch (IllegalComponentStateException ex) {
                    // 如果组件还没有显示在屏幕上，使用默认位置
                    miniMapDialog.setLocationRelativeTo(mainPanel);
                }
            }
            miniMapDialog.setVisible(miniMapButton.isSelected());
        });

        miniMapButton.setPreferredSize(new Dimension(40, 40));
        miniMapButton.setMaximumSize(new Dimension(40, 40));
        miniMapButton.setMinimumSize(new Dimension(40, 40));
        miniMapButton.setBorderPainted(false);
        miniMapButton.setContentAreaFilled(false);
        miniMapButton.setFocusPainted(false);
        miniMapButton.setOpaque(true);
        miniMapButton.setMargin(new Insets(0, 0, 0, 0));
        miniMapButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        miniMapButton.setBackground(null);

        // 鼠标悬停效果保持不变...
        miniMapButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!miniMapButton.isSelected()) {
                    miniMapButton.setBackground(new Color(0, 120, 215, 10));
                    miniMapButton.setBorder(new RoundedBorder(4, new Color(0, 120, 215, 20)));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!miniMapButton.isSelected()) {
                    miniMapButton.setBackground(new Color(255, 255, 255, 0));
                    miniMapButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                }
            }
        });

        return miniMapButton;
    }

    private JToggleButton createCunDangButton() {
        cunDangButton = new JToggleButton();
        cunDangButton.setUI(new CustomCanvasButtonUI());
        Icon icon = IconLoader.getIcon("/icons/cundang.svg", VisualLayoutPanel.class);
        cunDangButton.setIcon(icon);
        cunDangButton.setFocusPainted(false);
        cunDangButton.addActionListener(e -> {
            createArchive();
        });
        cunDangButton.setPreferredSize(new Dimension(40, 40));
        cunDangButton.setMaximumSize(new Dimension(40, 40));
        cunDangButton.setMinimumSize(new Dimension(40, 40));
        cunDangButton.setBorderPainted(false);
        cunDangButton.setContentAreaFilled(false);
        cunDangButton.setFocusPainted(false);
        cunDangButton.setOpaque(true);
        cunDangButton.setMargin(new Insets(0, 0, 0, 0));
        cunDangButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        cunDangButton.setBackground(null);

        cunDangButton.addMouseListener(new MouseOverBtnAdapter(cunDangButton));

        return cunDangButton;
    }

    //创建存档
    private void createArchive() {
        String description = Messages.showInputDialog(
                project,
                "请输入存档描述:",
                "创建存档",
                Messages.getQuestionIcon()
        );

        cunDangButton.setSelected(false);
        if (description != null) {
            PluginCache.archiveManager.saveArchive(description);
            Messages.showInfoMessage("存档创建成功!", "提示");
        }
    }


    // 添加新方法
    public void toggleMiniMap(boolean selected) {
        if (miniMapButton != null) {
            miniMapButton.setSelected(selected);
        }
    }


    /**
     * 根据传入的 XML 内容重新渲染画布
     * @param demoContent XML 格式的业务流程内容
     */
    public void renderDemoContent(String demoContent) {
        try {
            // 清空当前画布
            clearCanvas();
            
            // 使用 utils 解析新的内容并渲染
            PluginCache.sourseCodeUtils.loadComponentsFromSource(demoContent);
            
            // 重新布局和绘制
            canvasPanel.revalidate();
            canvasPanel.repaint();
            
            // 展开所有节点
            expandAll(true);
            
            // 更新源代码
            PluginCache.sourseCodeUtils.updateSourceCode();
            
        } catch (Exception e) {
            Messages.showErrorDialog(
                "渲染内容失败: " + e.getMessage(),
                "错误"
            );
        }
    }

    /**
     * 清空画布内容
     */
    private void clearCanvas() {
        // 清空组件列表
        components.clear();
        
        // 清空连接线
        connections.clear();
        
        // 清空选中状态
        selectedComponents.clear();
        
        // 移除画布上的所有组件
        canvasPanel.removeAll();
        
        // 重置编辑状态
        if (editingTextField != null) {
            finishEditing();
        }
        editingConnection = null;
        draggedConnection = null;
        isConnecting = false;
        isDraggingControlPoint = false;
    }

    /**
     * 展开或折叠所有节点
     * @param expand true 展开，false 折叠
     */
    private void expandAll(boolean expand) {
        for (Component comp : canvasPanel.getComponents()) {
            if (comp instanceof JLayeredPane) {
                JLayeredPane pane = (JLayeredPane) comp;
                if (expand) {
                    pane.setVisible(true);
                }
            }
        }
    }

    private Point calculateNextComponentPosition() {
        // 实现组件位置计算逻辑
        int x = 20;
        int y = 20;
        return new Point(x, y);
    }


    private JComponent createComponentUI(ComponentItem component) {
        String type = component.getType();
        String name = component.getName();
        JComponent uiComponent = null;
        return null;
    }
}
