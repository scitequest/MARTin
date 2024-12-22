package com.scitequest.martin.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Polygon;

public final class GraphicsPen implements Drawable {
    private final Graphics g;
    private final ImageViewer viewer;

    public GraphicsPen(Graphics g, ImageViewer viewer) {
        this.g = g;
        this.viewer = viewer;
    }

    @Override
    public void setColor(Color colour) {
        g.setColor(colour);
    }

    @Override
    public void drawPolygon(int[] polygonX, int[] polygonY) {
        for (int i = 0; i < polygonY.length; i++) {
            polygonX[i] = viewer.screenX(polygonX[i]);
            polygonY[i] = viewer.screenY(polygonY[i]);
        }
        g.drawPolygon(new Polygon(polygonX, polygonY, polygonY.length));
    }

    @Override
    public void drawString(String text, int x, int y) {
        // Graphics does not have a method like setJustification, so we have to
        // live with the kind of ugly implementation below.
        int fontSize = (int) Math.round(10 * viewer.getMagnification());
        g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));

        x = viewer.screenX(x) - fontSize / 2;
        y = viewer.screenY(y) - fontSize / 2;

        g.drawString(text, x, y);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        x = viewer.screenX(x);
        y = viewer.screenY(y);

        width = (int) Math.round(width * viewer.getMagnification());
        height = (int) Math.round(height * viewer.getMagnification());

        g.drawOval(x, y, width, height);
    }

}
