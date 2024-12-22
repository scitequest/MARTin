package com.scitequest.martin.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

public class ImageViewer extends JFrame {
    private static final double ZOOM_FACTOR = 1.1;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 10.0;
    // Minimum pixels of image that must remain visible
    private static final double MIN_VISIBLE = 50;

    private Optional<BufferedImage> image;
    private static boolean canvasActive;
    private double zoomLevel;
    private double translateX;
    private double translateY;
    private Point lastMousePosition;
    private boolean isPanning;
    private final JLabel infoLabel;
    private final ImagePanel imagePanel;

    public ImageViewer() {
        super("Image Viewer");

        this.image = Optional.empty();
        this.zoomLevel = 1.0;
        this.translateX = 0;
        this.translateY = 0;
        this.isPanning = false;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        infoLabel = new JLabel();
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        imagePanel = new ImagePanel();

        add(infoLabel, BorderLayout.SOUTH);
        add(imagePanel, BorderLayout.CENTER);

        setupEventHandlers();

        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private class ImagePanel extends JPanel {
        public ImagePanel() {
            setBackground(Color.LIGHT_GRAY);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawImage((Graphics2D) g);
        }
    }

    private void setupEventHandlers() {
        // Mouse wheel zoom handler
        imagePanel.addMouseWheelListener(e -> {
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                double delta = e.getWheelRotation() < 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
                Point mousePoint = e.getPoint();
                zoom(delta, mousePoint);
            }
        });

        // Mouse motion listener for panning
        MouseMotionAdapter motionAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isPanning || (e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
                    if (lastMousePosition != null) {
                        int dx = e.getX() - lastMousePosition.x;
                        int dy = e.getY() - lastMousePosition.y;
                        pan(dx, dy);
                    }
                    lastMousePosition = e.getPoint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!canvasActive) {
                    return;
                }
                lastMousePosition = e.getPoint();

                mouseDraggedHook(e);
            }
        };

        // Mouse listener for right-click panning
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!canvasActive) {
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON3) {
                    lastMousePosition = e.getPoint();
                }

                mousePressedHook(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    lastMousePosition = null;
                }

                mouseReleasedHook(e);
            }
        };

        imagePanel.addMouseListener(mouseAdapter);
        imagePanel.addMouseMotionListener(motionAdapter);

        // Key bindings
        imagePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
        imagePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        imagePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "enablePanning");
        imagePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "disablePanning");

        // Action mappings
        imagePanel.getActionMap().put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Point center = new Point(imagePanel.getWidth() / 2, imagePanel.getHeight() / 2);
                zoom(ZOOM_FACTOR, center);
            }
        });

        imagePanel.getActionMap().put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Point center = new Point(imagePanel.getWidth() / 2, imagePanel.getHeight() / 2);
                zoom(1 / ZOOM_FACTOR, center);
            }
        });

        imagePanel.getActionMap().put("enablePanning", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isPanning = true;
                imagePanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
        });

        imagePanel.getActionMap().put("disablePanning", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isPanning = false;
                lastMousePosition = null;
                imagePanel.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    protected void mousePressedHook(MouseEvent e) {
    }

    protected void mouseReleasedHook(MouseEvent e) {
    }

    protected void mouseDraggedHook(MouseEvent e) {
    }

    private void pan(int dx, int dy) {
        if (image.isEmpty())
            return;

        BufferedImage img = image.get();
        int imgWidth = (int) (img.getWidth() * zoomLevel);
        int imgHeight = (int) (img.getHeight() * zoomLevel);

        // Calculate the maximum allowed translation that keeps MIN_VISIBLE_RATIO of the
        // image visible
        double maxTranslateX = imgWidth / 2 + imagePanel.getWidth() / 2 - MIN_VISIBLE;
        double maxTranslateY = imgHeight / 2 + imagePanel.getHeight() / 2 - MIN_VISIBLE;

        // Calculate new translations
        double newTranslateX = translateX + dx;
        double newTranslateY = translateY + dy;

        // Bound the translations
        translateX = Math.max(-maxTranslateX, Math.min(maxTranslateX, newTranslateX));
        translateY = Math.max(-maxTranslateY, Math.min(maxTranslateY, newTranslateY));

        imagePanel.repaint();
    }

    private void zoom(double factor, Point zoomPoint) {
        if (image.isEmpty())
            return;

        // Calculate relative position of zoom point
        double relX = (zoomPoint.x - (imagePanel.getWidth() / 2 + translateX)) / zoomLevel;
        double relY = (zoomPoint.y - (imagePanel.getHeight() / 2 + translateY)) / zoomLevel;

        // Calculate new zoom level
        double newZoom = zoomLevel * factor;
        if (newZoom < MIN_ZOOM || newZoom > MAX_ZOOM)
            return;

        // Update zoom level
        zoomLevel = newZoom;

        // Adjust translation to keep zoom point stable
        translateX = zoomPoint.x - (imagePanel.getWidth() / 2 + relX * zoomLevel);
        translateY = zoomPoint.y - (imagePanel.getHeight() / 2 + relY * zoomLevel);

        imagePanel.repaint();
    }

    private void drawImage(Graphics2D g2d) {
        if (image.isEmpty())
            return;

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        BufferedImage img = image.get();
        int imgWidth = (int) (img.getWidth() * zoomLevel);
        int imgHeight = (int) (img.getHeight() * zoomLevel);

        // Calculate center position
        int centerX = imagePanel.getWidth() / 2;
        int centerY = imagePanel.getHeight() / 2;

        // Draw image with translation
        g2d.drawImage(img,
                (int) (centerX - imgWidth / 2 + translateX),
                (int) (centerY - imgHeight / 2 + translateY),
                imgWidth, imgHeight, null);

        // Call the external drawing method for overlays
        drawElements(g2d);
    }

    protected void drawElements(Graphics g) {
    }

    public void setImage(BufferedImage newImage, boolean keepTransform) {
        if (!keepTransform) {
            zoomLevel = 1.0;
            translateX = 0;
            translateY = 0;
        }

        this.image = Optional.ofNullable(newImage);
        updateInfoLabel();
        imagePanel.repaint();
    }

    public static void toggleCanvas(boolean toggle) {
        canvasActive = toggle;
    }

    public double getMagnification() {
        return zoomLevel;
    }

    public int screenX(int pixelX) {
        BufferedImage img = image.get();
        int imgWidth = (int) (img.getWidth() * zoomLevel);
        int centerX = imagePanel.getWidth() / 2;

        return (int) (centerX - imgWidth / 2.0 + pixelX * zoomLevel + translateX);
    }

    public int screenY(int pixelY) {
        BufferedImage img = image.get();
        int imgHeight = (int) (img.getHeight() * zoomLevel);
        int centerY = imagePanel.getHeight() / 2;

        return (int) (centerY - imgHeight / 2.0 + pixelY * zoomLevel + translateY);
    }

    public int worldX(int screenX) {
        BufferedImage img = image.get();
        int imgWidth = (int) (img.getWidth() * zoomLevel);
        int centerX = imagePanel.getWidth() / 2;

        double worldX = (screenX - translateX - centerX + imgWidth / 2.0) / zoomLevel;
        return (int) worldX;
    }

    public int worldY(int screenY) {
        BufferedImage img = image.get();
        int imgHeight = (int) (img.getHeight() * zoomLevel);
        int centerY = imagePanel.getHeight() / 2;

        double worldY = (screenY - translateY - centerY + imgHeight / 2.0) / zoomLevel;
        return (int) worldY;
    }

    private void updateInfoLabel() {
        if (image.isEmpty()) {
            infoLabel.setText("No image loaded");
            return;
        }

        BufferedImage img = image.get();
        String resolution = String.format("%dx%d", img.getWidth(), img.getHeight());

        String type;
        switch (img.getType()) {
            case BufferedImage.TYPE_INT_RGB:
                type = "RGB";
                break;
            case BufferedImage.TYPE_INT_ARGB:
                type = "ARGB";
                break;
            case BufferedImage.TYPE_INT_ARGB_PRE:
                type = "ARGB Pre-Multiplied";
                break;
            case BufferedImage.TYPE_INT_BGR:
                type = "BGR";
                break;
            case BufferedImage.TYPE_3BYTE_BGR:
                type = "3-Byte BGR";
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
                type = "4-Byte ABGR";
                break;
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                type = "4-Byte ABGR Pre-Multiplied";
                break;
            case BufferedImage.TYPE_BYTE_GRAY:
                type = "Grayscale";
                break;
            case BufferedImage.TYPE_BYTE_BINARY:
                type = "Binary";
                break;
            case BufferedImage.TYPE_BYTE_INDEXED:
                type = "Indexed";
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                type = "Unsigned Short Grayscale";
                break;
            case BufferedImage.TYPE_USHORT_565_RGB:
                type = "Unsigned Short 565 RGB";
                break;
            case BufferedImage.TYPE_USHORT_555_RGB:
                type = "Unsigned Short 555 RGB";
                break;
            case BufferedImage.TYPE_CUSTOM:
                type = "Custom";
                break;
            default:
                type = "Unknown";
                break;
        }

        // Calculate image size
        int bytesPerPixel;
        switch (img.getType()) {
            case BufferedImage.TYPE_BYTE_BINARY:
            case BufferedImage.TYPE_BYTE_GRAY:
                bytesPerPixel = 1;
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                bytesPerPixel = 2;
                break;
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_3BYTE_BGR:
                bytesPerPixel = 3;
                break;
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                bytesPerPixel = 4;
                break;
            default:
                bytesPerPixel = 4;
                break;
        }

        long sizeInBytes = (long) img.getWidth() * img.getHeight() * bytesPerPixel;
        String sizeStr = formatSize(sizeInBytes);

        infoLabel.setText(String.format("%s | %s | %s", resolution, type, sizeStr));
    }

    private String formatSize(long bytes) {
        DecimalFormat df = new DecimalFormat("#.##");
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return df.format(bytes / 1024.0) + " KiB";
        if (bytes < 1024 * 1024 * 1024)
            return df.format(bytes / (1024.0 * 1024)) + " MiB";
        return df.format(bytes / (1024.0 * 1024 * 1024)) + " GiB";
    }
}
