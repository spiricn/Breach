package com.limber.breach;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.limber.breach.analyzer.Grid;
import com.limber.breach.analyzer.GridNode;

import java.util.List;

public class DrawUtils {
    public static Rect getRect(Grid grid) {

        GridNode node = grid.nodes.get(0).get(0);

        int horizontal = node.boundingBox.width() * 4;
        int vertical = node.boundingBox.height() * 4;
        Rect rect = new Rect(grid.boundingBox);

        rect.set(
                Math.max(rect.left - horizontal, 0),
                Math.max(rect.top - vertical, 0),
                rect.right + horizontal,
                rect.bottom + vertical
        );

        return rect;
    }

    public static void scaleFor(Canvas canvas, Rect rect) {

        float sw = (float) rect.width() / (float) canvas.getClipBounds().width();
        float sh = (float) rect.height() / (float) canvas.getClipBounds().height();

        canvas.scale(1 / sw, 1 / sh);
        canvas.translate(-rect.left, -rect.top);
    }

    public static void drawGrid(Grid grid, Bitmap bitmap, Canvas canvas) {
        Rect rect = getRect(grid);

        canvas.drawBitmap(
                bitmap,
                rect,
                canvas.getClipBounds(),
                new Paint()
        );
    }

    public static void highlightNodes(Grid grid, Canvas canvas) {
        Paint boundaryPaint = new Paint();
        boundaryPaint.setColor(Color.GREEN);
        boundaryPaint.setStyle(Paint.Style.STROKE);
        boundaryPaint.setStrokeWidth(2);


        highlightNodes(grid, canvas, null, boundaryPaint);
    }

    public static void highlightNodes(Grid grid, Canvas canvas, List<GridNode> nodes, Paint boundaryPaint) {
        scaleFor(canvas, getRect(grid));

        for (List<GridNode> nodeRow : grid.nodes) {
            for (GridNode node : nodeRow) {
                if (nodes == null || nodes.contains(node)) {
                    canvas.drawRect(node.boundingBox, boundaryPaint);
                }
            }
        }
    }
}
