package com.limber.breach.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.limber.breach.analyzer.Grid;
import com.limber.breach.analyzer.Node;

import java.util.List;

public class DrawUtils {
    /**
     * Paint used to draw node boundaries
     */
    public static final Paint kBOUNDARY_PAINT;

    /**
     * Grid rect indentation
     */
    private static final int kGRID_RECT_SIZE_MODIFIER = 4;

    static {
        kBOUNDARY_PAINT = new Paint();
        kBOUNDARY_PAINT.setColor(Color.GREEN);
        kBOUNDARY_PAINT.setStyle(Paint.Style.STROKE);
        kBOUNDARY_PAINT.setStrokeWidth(2);
    }

    /**
     * Get a rect which encompasses a grid
     */
    public static Rect getGridRect(Grid grid) {
        // Enlarge the rect by N node sizes
        Node node = grid.rows.get(0).get(0);

        int horizontalDelta = node.boundingBox.width() * kGRID_RECT_SIZE_MODIFIER;
        int verticalDelta = node.boundingBox.height() * kGRID_RECT_SIZE_MODIFIER;

        Rect rect = grid.getBoundingBox();

        rect.set(
                Math.max(rect.left - horizontalDelta, 0),
                Math.max(rect.top - verticalDelta, 0),

                rect.right + horizontalDelta,
                rect.bottom + verticalDelta
        );

        return rect;
    }

    /**
     * Scale the canvas for given rect
     */
    public static void scaleFor(Canvas canvas, Rect rect) {
        float sw = (float) rect.width() / (float) canvas.getClipBounds().width();
        float sh = (float) rect.height() / (float) canvas.getClipBounds().height();

        canvas.scale(1 / sw, 1 / sh);
        canvas.translate(-rect.left, -rect.top);
    }

    /**
     * Draw the scaled grid bitmap
     */
    public static void drawGrid(Grid grid, Bitmap bitmap, Canvas canvas) {
        Rect rect = getGridRect(grid);

        canvas.drawBitmap(
                bitmap,
                rect,
                canvas.getClipBounds(),
                new Paint()
        );
    }

    public static void highlightNodes(Grid grid, Canvas canvas) {
        highlightNodes(grid, canvas, null, kBOUNDARY_PAINT);
    }

    public static void highlightNodes(Grid grid, Canvas canvas, List<Node> nodes) {
        highlightNodes(grid, canvas, nodes, kBOUNDARY_PAINT);
    }

    /**
     * Highlight listed nodes on the grid
     */
    public static void highlightNodes(Grid grid, Canvas canvas, List<Node> nodes, Paint boundaryPaint) {
        scaleFor(canvas, getGridRect(grid));

        for (List<Node> nodeRow : grid.rows) {
            for (Node node : nodeRow) {
                if (nodes == null || nodes.contains(node)) {
                    canvas.drawRect(node.boundingBox, boundaryPaint);
                }
            }
        }
    }
}
