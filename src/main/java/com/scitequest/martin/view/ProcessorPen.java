package com.scitequest.martin.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Polygon;

public final class ProcessorPen implements Drawable {
    private final Graphics g;

    public ProcessorPen(Graphics g) {
        this.g = g;
    }

    @Override
    public void setColor(Color colour) {
        g.setColor(colour);
    }

    @Override
    public void drawPolygon(int[] polygonX, int[] polygonY) {
        g.drawPolygon(new Polygon(polygonX, polygonY, polygonY.length));
    }

    @Override
    public void drawString(String text, int x, int y) {
        g.setFont(new Font("TimesRoman", Font.PLAIN, 10));
        g.drawString(text, x, y);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        g.drawOval(x, y, width, height);
    }
}
