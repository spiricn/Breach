package com.limber.breach.analyzer;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

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

    public interface SuccessCallback {
        void onAnalyzed(Result result);
    }

    public interface FailedCallback {
        void onFailed(Exception e);
    }

    static Bitmap mBmp;

    public static void analyze(Bitmap bitmap, SuccessCallback successCallback, FailedCallback failedCallback) {
        mBmp = bitmap;

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient();

        recognizer.process(image)
                .addOnSuccessListener(
                        texts -> {
                            Result result;
                            try {
                                result = processTextRecognitionResult(texts);
                            } catch (Exception e) {
                                failedCallback.onFailed(e);
                                return;
                            }

                            successCallback.onAnalyzed(result);
                        })
                .addOnFailureListener(
                        failedCallback::onFailed);
    }

    enum Coord {
        row,
        column
    }

    static class Node {
        Text.Element element;

        Node(@NonNull Text.Element element) {
            this.element = element;
        }

        double get(Coord coord) {
            switch (coord) {
                case row:
                    return Objects.requireNonNull(element.getBoundingBox()).top;
                case column:
                    return Objects.requireNonNull(element.getBoundingBox()).left;
            }

            return 0;
        }
    }

    static Pair<Double, Double> getAverageSize(List<Node> nodes) {
        double averageWidth = 0;
        double averageHeight = 0;

        for (Node node : nodes) {
            averageWidth += Objects.requireNonNull(node.element.getBoundingBox()).width();
            averageHeight += node.element.getBoundingBox().height();
        }
        averageHeight /= nodes.size();
        averageWidth /= nodes.size();

        return new Pair<>(averageWidth, averageHeight);
    }


    static List<Node> find(Node reference, double average, List<Node> nodes) {
        return find(Coord.row, reference.get(Coord.row), average, nodes);
    }

    static List<Node> find(Coord coord, double reference, double average, List<Node> nodes) {
        List<Node> result = new ArrayList<>();

        for (Node node : nodes) {
            if (Math.abs(node.get(coord) - reference) <= average) {
                result.add(node);
            }
        }

        Coord otherCoord = coord == Coord.row ? Coord.column : Coord.row;

        Collections.sort(result, (o1, o2) -> Double.compare(o1.get(otherCoord), o2.get(otherCoord)));

        return result;
    }

    static List<List<Node>> splitRows(double average, List<Node> nodes) {
        List<List<Node>> rows = new ArrayList<>();

        while (!nodes.isEmpty()) {
            List<Node> row = new ArrayList<>();

            Node currentNode = nodes.get(0);
            nodes.remove(0);
            row.add(currentNode);

            row.addAll(find(currentNode, average, nodes));
            nodes.removeAll(row);

            rows.add(row);
        }

        return rows;
    }

    static Grid convertGrid(List<List<Node>> nodes) {
        Grid grid = new Grid();

        for (List<Node> row : nodes) {
            ArrayList<GridNode> resRow = new ArrayList<>();

            for (Node node : row) {
                if (grid.boundingBox == null) {
                    grid.boundingBox = new Rect(node.element.getBoundingBox());
                } else {
                    grid.boundingBox.union(node.element.getBoundingBox());
                }

                resRow.add(new GridNode(node.element.getText(), node.element.getBoundingBox()));
            }

            grid.nodes.add(resRow);
        }

        return grid;
    }

    private static Result processTextRecognitionResult(Text texts) {
        Result result = new Result();

        MatrixResult matrixRes = processMatrix(texts);

        assert matrixRes != null;
        result.matrix = convertGrid(matrixRes.matrix);
        result.bitmap = mBmp;

        List<Node> sequenceNodes = new ArrayList<>();
        for (Node node : matrixRes.nodes) {
            if (Objects.requireNonNull(node.element.getBoundingBox()).intersect(matrixRes.boundingBox)) {
                continue;
            }

            if ((node.element.getBoundingBox().top) < (matrixRes.boundingBox.top - matrixRes.averageHeight * 2)) {
                continue;
            }

            if (node.element.getBoundingBox().bottom > matrixRes.boundingBox.bottom) {
                continue;
            }

            sequenceNodes.add(node);
        }

        result.sequences = convertGrid(splitRows(matrixRes.averageWidth, sequenceNodes));

        return result;
    }


    static class MatrixResult {
        List<Node> nodes;
        List<List<Node>> matrix;
        Rect boundingBox;
        double averageWidth;
        double averageHeight;
    }


    private static final int kMIN_MATRIX_SIZE = 4;

    private static MatrixResult processMatrix(Text texts) {

        // Pre-process nodes
        List<Node> nodes = preprocessNodes(texts);

        List<Node> allNodes = new ArrayList<>(nodes);


        // Average size
        Pair<Double, Double> averageSize = getAverageSize(nodes);
        double averageWidth = averageSize.first;
        double averageHeight = averageSize.second;

        List<List<Node>> rows = splitRows(averageHeight, nodes);

        Collections.sort(rows, (o1, o2) -> Integer.compare(Objects.requireNonNull(o1.get(0).element.getBoundingBox()).top,
                Objects.requireNonNull(o2.get(0).element.getBoundingBox()).top));

        if (rows.size() < kMIN_MATRIX_SIZE) {
            throw new RuntimeException("Not found");
        }

        for (List<Node> row : rows) {

            if (row.size() < kMIN_MATRIX_SIZE) {
                continue;
            }

            List<List<Node>> columns = new ArrayList<>();

            int size = find(Coord.column, Objects.requireNonNull(row.get(kMIN_MATRIX_SIZE / 2).element.getBoundingBox()).left,
                    averageWidth, allNodes).size();

            for (int i = 0; i < size; i++) {
                List<Node> column = find(Coord.column, Objects.requireNonNull(row.get(i).element.getBoundingBox()).left,
                        averageWidth, allNodes);
                if (column.size() < size) {
                    break;
                }

                columns.add(column);
            }

            if (columns.size() == size) {
                MatrixResult result = new MatrixResult();
                result.averageHeight = averageHeight;
                result.averageWidth = averageWidth;


                List<List<Node>> matrix = new ArrayList<>();

                for (int i = 0; i < size; i++) {
                    List<Node> resultRow = new ArrayList<>();

                    for (List<Node> column : columns) {
                        Node node = column.get(i);
                        resultRow.add(node);

                        if (result.boundingBox == null) {
                            result.boundingBox = new Rect(node.element.getBoundingBox());
                        } else {
                            result.boundingBox.union(node.element.getBoundingBox());
                        }
                    }

                    matrix.add(resultRow);
                }


                result.nodes = allNodes;
                result.matrix = matrix;

                return result;
            } else {
                Log.e("@#", "disard row");
            }
        }

        throw new RuntimeException("Not found");
    }

    private static List<Node> preprocessNodes(Text texts) {
        Pattern pattern = Pattern.compile("[0-9a-fA-F]{2}", Pattern.CASE_INSENSITIVE);

        List<Node> elements = new ArrayList<>();

        for (Text.TextBlock block : texts.getTextBlocks()) {
            List<Text.Line> lines = block.getLines();
            for (Text.Line line : lines) {
                for (Text.Element element : line.getElements()) {
                    if (pattern.matcher(element.getText()).matches()) {
                        elements.add(new Node(element));
                    }
                }
            }
        }

        return elements;
    }
}
