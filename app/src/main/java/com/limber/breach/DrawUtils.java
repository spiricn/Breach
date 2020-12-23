package com.limber.breach;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.widget.TextView;

import java.util.List;

public class DrawUtils {
    public static Rect getRect(Analyzer.Grid grid) {

        int offset = 200;

        Rect rect = new Rect(grid.boundingBox);

        rect.set(
                rect.left - offset,
                rect.top - offset,
                rect.right + offset,
                rect.bottom + offset
        );

        return rect;
    }

    public static void scaleFor(Canvas canvas, Rect rect) {

        float sw = (float) rect.width() / (float) canvas.getClipBounds().width();
        float sh = (float) rect.height() / (float) canvas.getClipBounds().height();

        canvas.scale(1 / sw, 1 / sh);
        canvas.translate(-rect.left, -rect.top);
    }

    public static void drawGrid(Analyzer.Grid grid, Bitmap bitmap, Canvas canvas) {

        Paint boundaryPaint = new Paint();
        boundaryPaint.setColor(Color.GREEN);
        boundaryPaint.setStyle(Paint.Style.STROKE);
        boundaryPaint.setStrokeWidth(2);

        Rect rect = getRect(grid);
        canvas.drawBitmap(
                bitmap,
                rect,
                canvas.getClipBounds(),
                new Paint()
        );

        scaleFor(canvas, rect);

        for (List<Analyzer.GridNode> nodeRow : grid.nodes) {
            for (Analyzer.GridNode node : nodeRow) {
                canvas.drawRect(node.boundingBox, boundaryPaint);
            }
        }
    }
}
