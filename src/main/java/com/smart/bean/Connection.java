package com.smart.bean;

import java.awt.*;
import java.util.UUID;

public class Connection {

    public ComponentInfo start;
    public ComponentInfo end;
    public Point startPoint;
    public Point endPoint;
    public Point controlPoint;
    public String id;
    public String label;
    public boolean isSelected = false;
    private String expression;
    private String expressionLanguage;

    public Connection(ComponentInfo start, ComponentInfo end, Point startPoint, Point endPoint) {
        this.start = start;
        this.end = end;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.controlPoint = new Point((startPoint.x + endPoint.x) / 2, (startPoint.y + endPoint.y) / 2);
        this.id = UUID.randomUUID().toString();
        this.label = "";
    }

    public String getExpression() {
        return expression != null ? expression : "";
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getExpressionLanguage() {
        return expressionLanguage != null ? expressionLanguage : "javascript";
    }

    public void setExpressionLanguage(String language) {
        this.expressionLanguage = language;
    }

}
