package com.scitequest.martin.view;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.scitequest.martin.DrawOptions;
import com.scitequest.martin.settings.Settings;

public final class SlideViewer extends ImageViewer {

    private final Controlable control;
    private final Settings settings;

    /** X coordinate when the mouse was pressed. */
    private int lastX = 0;
    /** Y coordinate when the mouse was pressed. */
    private int lastY = 0;

    public SlideViewer(Controlable control, Settings settings) {
        super();
        this.control = control;
        this.settings = settings;

        setupEventHandlers();
    }

    private void setupEventHandlers() {
        imagePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, false), "repositionSlide");
        imagePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "measureFieldFit");
        imagePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F, 0, false), "toggleFilter");
        imagePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, false), "measure");

        imagePanel.getActionMap().put("repositionSlide", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.repositionSlide();
                control.update();
            }
        });
        imagePanel.getActionMap().put("measureFieldFit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.measureFieldFit();
            }
        });
        imagePanel.getActionMap().put("toggleFilter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.toggleFilter();
            }
        });
        imagePanel.getActionMap().put("measure", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.measure();
            }
        });
    }

    /**
     * Draws the separately calculated elements of a slide mask.
     *
     * @param g instance of Graphics
     */
    @Override
    protected void drawElements(Graphics g) {
        DrawOptions drawOptions = new DrawOptions(true,
                settings.getDisplaySettings().isShowSpotfieldGrids(),
                false, true,
                settings.getMeasurementSettings().isSubtractBackground(),
                settings.getDisplaySettings().isShowMeasureCircles());
        control.drawElements(new GraphicsPen(g, this), drawOptions);
    }

    @Override
    protected void mousePressedHook(MouseEvent e) {
        lastX = worldX(e.getX());
        lastY = worldY(e.getY());

        control.updateClickedMeasureCircles(lastX, lastY);
        control.updateClickedRectPolygons(lastX, lastY);
    }

    @Override
    protected void mouseReleasedHook(MouseEvent e) {
        control.releaseMouseGrip();
    }

    @Override
    protected void mouseDraggedHook(MouseEvent e) {
        int worldX = worldX(e.getX());
        int worldY = worldY(e.getY());
        int prevX = lastX;
        int prevY = lastY;
        lastX = worldX;
        lastY = worldY;

        if (!control.moveGrabbedElement(worldX, worldY, prevX, prevY, 1, 1,
                false)) {
            control.rotateSlideViaMouse(worldX, worldY, prevX, prevY);
        }
    }
}
