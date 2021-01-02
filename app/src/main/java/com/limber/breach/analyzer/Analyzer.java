package com.limber.breach.analyzer;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class Analyzer {
    /**
     * Log tag
     */
    private static final String kTAG = Analyzer.class.getSimpleName();

    /**
     * Called on processing success
     */
    public interface SuccessCallback {
        void onAnalyzed(Result result);
    }

    /**
     * Called on processing failure
     */
    public interface FailedCallback {
        void onFailed(Exception e);
    }

    /**
     * Processed texts result
     */
    private static class ProcessedText {
        /**
         * Average node width
         */
        double averageWidth = 0;

        /**
         * Average node height
         */
        double averageHeight = 0;

        /**
         * All nodes
         */
        List<Node> nodes = new ArrayList<>();

        List<Node> findNodesInColumn(double reference) {
            return findNodes(
                    Node.Coord.column,
                    reference,
                    getAverage(Node.Coord.column),
                    this.nodes);
        }

        List<Node> findNodesInRow(double reference, @Nullable List<Node> nodes) {
            return findNodes(
                    Node.Coord.row,
                    reference,
                    getAverage(Node.Coord.row),
                    nodes != null ? nodes : this.nodes);
        }

        /**
         * Get average width or height
         */
        double getAverage(Node.Coord coord) {
            return coord == Node.Coord.row ? this.averageHeight : this.averageWidth;
        }

        /**
         * Find all nodes in row or column of given list
         *
         * @param coord     Row or column
         * @param reference Reference value (left or top)
         * @param average   Average width or height
         * @param nodes     List of nodes
         */
        private static List<Node> findNodes(Node.Coord coord, double reference, double average, List<Node> nodes) {
            List<Node> result = new ArrayList<>();

            for (Node node : nodes) {
                if (Math.abs(node.get(coord) - reference) <= average) {
                    result.add(node);
                }
            }

            // Sort by other coordinate
            Node.Coord otherCoord = coord == Node.Coord.row ? Node.Coord.column : Node.Coord.row;
            Collections.sort(result, (o1, o2) -> Double.compare(o1.get(otherCoord), o2.get(otherCoord)));

            return result;
        }
    }

    /**
     * Minimum valid matrix size
     */
    private static final int kMIN_MATRIX_SIZE = 4;

    /**
     * Analyze a bitmap and provide matrix & sequence results
     *
     * @param bitmap          Input bitmap
     * @param successCallback Called on success
     * @param failedCallback  Called on failure
     * @param handler         Handler on which the callback is executed
     */
    public static void analyze(Bitmap bitmap, SuccessCallback successCallback, FailedCallback failedCallback, Handler handler) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient();

        recognizer.process(image)
                .addOnSuccessListener(
                        texts -> handler.post(() -> {
                            Result result;
                            try {
                                result = processTextRecognitionResult(bitmap, texts);
                            } catch (Exception e) {
                                e.printStackTrace();
                                failedCallback.onFailed(e);
                                return;
                            }

                            successCallback.onAnalyzed(result);
                        }))
                .addOnFailureListener(e -> handler.post(() -> failedCallback.onFailed(e)));
    }


    /**
     * Process texts
     *
     * @param bitmap Input bitmap
     * @param texts  Analyzed text
     */
    private static Result processTextRecognitionResult(Bitmap bitmap, Text texts) {
        Result result = new Result();

        result.bitmap = bitmap;

        ProcessedText processedText = processTexts(texts);
        result.matrix = findMatrix(processedText);
        result.sequences = findSequences(processedText, result.matrix);

        return result;
    }

    /**
     * Split the list of nodes in rows
     */
    private static List<List<Node>> splitNodesByRows(ProcessedText processedText) {
        return splitNodesByRows(processedText, new ArrayList<>(processedText.nodes));
    }

    /**
     * Split a list of given nodes in rows
     */
    private static List<List<Node>> splitNodesByRows(ProcessedText processedText, List<Node> nodes) {
        List<Node> allNodes = new ArrayList<>(nodes);
        List<List<Node>> rows = new ArrayList<>();

        while (!allNodes.isEmpty()) {
            Node currentNode = allNodes.get(0);
            allNodes.remove(0);

            List<Node> row = new ArrayList<>(processedText.findNodesInRow(currentNode.boundingBox.top, nodes));
            allNodes.removeAll(row);

            rows.add(row);
        }

        return rows;
    }


    /**
     * Find matrix
     *
     * @param processedText Processed text
     */
    private static Grid findMatrix(ProcessedText processedText) {
        // Split all the nodes by rows
        List<List<Node>> rows = splitNodesByRows(processedText);

        // Debug log the rows
        StringBuilder sb = new StringBuilder();
        for (List<Node> row : rows) {
            for (Node node : row) {
                sb.append(node.text).append(" ");
            }

            sb.append("\n");
        }
        Log.v(kTAG, "Found " + rows.size() + " rows:\n" + sb.toString());

        // Sort the rows vertically
        Collections.sort(rows, (o1, o2) -> Integer.compare(Objects.requireNonNull(o1.get(0).boundingBox).top,
                Objects.requireNonNull(o2.get(0).boundingBox).top));

        if (rows.size() < kMIN_MATRIX_SIZE) {
            throw new RuntimeException("Not enough rows: " + rows.size());
        }

        int expectedColumnSize =
                processedText.findNodesInColumn(rows.get(rows.size() / 2).get(kMIN_MATRIX_SIZE / 2).boundingBox.left).size();

        Log.v(kTAG, "Expected column size: " + expectedColumnSize);

        for (List<Node> row : rows) {
            // Ignore rows that are too small
            if (row.size() < kMIN_MATRIX_SIZE) {
                continue;
            }

            List<List<Node>> columns = new ArrayList<>();

            for (int i = 0; i < expectedColumnSize; i++) {
                List<Node> column = processedText.findNodesInColumn(Objects.requireNonNull(row.get(i).boundingBox).left);
                if (column.size() < expectedColumnSize) {
                    break;
                }

                columns.add(column);
            }

            if (columns.size() != expectedColumnSize) {
                // Discard row
                continue;
            }

            List<List<Node>> matrix = new ArrayList<>();

            for (int i = 0; i < expectedColumnSize; i++) {
                List<Node> resultRow = new ArrayList<>();

                for (List<Node> column : columns) {
                    Node node = column.get(i);
                    resultRow.add(node);
                }

                matrix.add(resultRow);
            }

            return new Grid(matrix);
        }

        throw new RuntimeException();
    }

    /**
     * Find sequences
     *
     * @param processedText Processed text
     * @param matrix        Matrix grid
     */
    private static Grid findSequences(ProcessedText processedText, Grid matrix) {
        Rect matrixBoundingBox = matrix.getBoundingBox();


        List<Node> sequenceNodes = new ArrayList<>();
        for (Node node : processedText.nodes) {
            if (node.boundingBox.intersect(matrixBoundingBox)) {
                // Ignore nodes which intersect with matrix
                continue;
            }

            if ((node.boundingBox.top) < (matrixBoundingBox.top - processedText.averageHeight * 2)) {
                // Ignore nodes above the matrix
                continue;
            }

            if (node.boundingBox.bottom > matrixBoundingBox.bottom) {
                // Ignore nodes below the matrix
                continue;
            }

            sequenceNodes.add(node);
        }

        return new Grid(splitNodesByRows(processedText, sequenceNodes));
    }


    /**
     * Pre-process raw text result
     */
    private static ProcessedText processTexts(Text texts) {
        ProcessedText result = new ProcessedText();

        Pattern pattern = Pattern.compile("[0-9a-fA-F]{2}", Pattern.CASE_INSENSITIVE);

        for (Text.TextBlock block : texts.getTextBlocks()) {
            List<Text.Line> lines = block.getLines();
            for (Text.Line line : lines) {
                for (Text.Element element : line.getElements()) {
                    if (pattern.matcher(element.getText()).matches()) {
                        result.averageWidth += Objects.requireNonNull(element.getBoundingBox()).width();
                        result.averageHeight += element.getBoundingBox().height();

                        result.nodes.add(new Node(element.getBoundingBox(), element.getText()));

                        Log.v(kTAG, "Found node: " + element.getText());
                    }
                }
            }
        }

        result.averageWidth /= result.nodes.size();
        result.averageHeight /= result.nodes.size();

        Log.v(kTAG, "Average node size (" + result.averageWidth + " x " + result.averageHeight + ")");

        return result;
    }
}
