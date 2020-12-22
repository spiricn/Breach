package com.limber.breach;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Analyzer {

    static class Result {
        List<List<Integer>> matrix;
        Rect matrixBoundingBox;

        List<List<Integer>> sequences;
        Rect sequencesBoundingBox;
    }


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
                            try {
                                successCallback.onAnalyzed(processTextRecognitionResult(texts));
                            } catch (Exception e) {
                                failedCallback.onFailed(e);
                            }
                        })
                .addOnFailureListener(
                        failedCallback::onFailed);
    }

    enum Coord {
        row,
        column
    }

    ;

    static class Node {
        Text.Element element;

        boolean sorted = false;

        Node(Text.Element element) {
            this.element = element;
        }

        double get(Coord coord) {
            switch (coord) {
                case row:
                    return element.getBoundingBox().top;
                case column:
                    return element.getBoundingBox().left;
            }

            return 0;
        }
    }

    class NodeRow {
        List<Node> nodes = new ArrayList<>();
    }


    static Pair<Double, Double> getAverageSize(List<Node> nodes) {
        double averageWidth = 0;
        double averageHeight = 0;

        for (Node node : nodes) {
            averageWidth += node.element.getBoundingBox().width();
            averageHeight += node.element.getBoundingBox().height();
        }
        averageHeight /= nodes.size();
        averageWidth /= nodes.size();

        return new Pair<>(averageWidth, averageHeight);
    }


    static List<Node> find(Coord coord, Node reference, double average, List<Node> nodes) {
        return find(coord, reference.get(coord), average, nodes);
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

    static List<List<Node>> splitBy(Coord coord, double average, List<Node> nodes) {
        List<List<Node>> rows = new ArrayList<>();

        while (!nodes.isEmpty()) {
            List<Node> row = new ArrayList<>();

            Node currentNode = nodes.get(0);
            nodes.remove(0);
            row.add(currentNode);

            row.addAll(find(coord, currentNode, average, nodes));
            nodes.removeAll(row);

            rows.add(row);
        }

        return rows;
    }

    static String print(List<Node> nodes) {
        StringBuilder b = new StringBuilder();
        for (Node node : nodes) {
            b.append(node.element.getText() + String.format(" [%3d.%3d]     ",
                    node.element.getBoundingBox().left, node.element.getBoundingBox().top));
        }

        return b.toString();
    }

    private static Result processTextRecognitionResult(Text texts) {
        Result result = new Result();

        MatrixResult matrixRes = processMatrix(texts);


        List<Node> filteredNodes = new ArrayList<>();

        for (Node node : matrixRes.nodes) {
            if (node.element.getBoundingBox().intersect(matrixRes.boundingBox)) {
                continue;
            }

            if ((node.element.getBoundingBox().top ) < (matrixRes.boundingBox.top - matrixRes.averageHeight * 2)) {
                continue;
            }

            if (node.element.getBoundingBox().bottom > matrixRes.boundingBox.bottom) {
                continue;
            }

            filteredNodes.add(node);
        }

        result.sequences = convertRows(splitBy(Coord.row, matrixRes.averageWidth, filteredNodes));

        result.matrixBoundingBox = matrixRes.boundingBox;
        result.matrix = convertRows(matrixRes.matrix);

        return result;
    }

    static List<List<Integer>> convertRows(List<List<Node>> inRows) {
        List<List<Integer>> rows = new ArrayList<>();

        for (List<Node> row : inRows) {
            List<Integer> irow = new ArrayList<>();
            for (Node node : row) {
                irow.add(Integer.parseInt(node.element.getText(), 16));
            }

            rows.add(irow);
        }

        return rows;
    }


    static class MatrixResult {
        List<Node> nodes;
        List<List<Node>> matrix;
        Rect boundingBox;
        double averageWidth;
        double averageHeight;
    }

    static class SequenceResult {
        List<List<Node>> sequences;
        Rect boundingBox;
    }

    private static final int kMIN_MATRIX_SIZE = 4;

    private static MatrixResult processMatrix(Text texts) {

        // Preprocess nodes
        List<Node> nodes = preprocessNodes(texts);

        List<Node> allNodes = new ArrayList<>();
        allNodes.addAll(nodes);


        // Average size
        Pair<Double, Double> averageSize = getAverageSize(nodes);
        double averageWidth = averageSize.first;
        double averageHeight = averageSize.second;

        List<List<Node>> rows = splitBy(Coord.row, averageHeight, nodes);

        Collections.sort(rows, (o1, o2) -> Integer.compare(o1.get(0).element.getBoundingBox().top,
                o2.get(0).element.getBoundingBox().top));

        if (rows.size() < kMIN_MATRIX_SIZE) {
            return null;
        }

        int rowIndex = -1;
        for (List<Node> row : rows) {
            rowIndex += 1;

            if (row.size() < kMIN_MATRIX_SIZE) {
                Log.e("@#", "too small row");
                continue;
            }

            List<List<Node>> columns = new ArrayList<>();

            int size = find(Coord.column, row.get(kMIN_MATRIX_SIZE / 2).element.getBoundingBox().left,
                    averageWidth, allNodes).size();


            Log.e("@#", "deteced size " + size);

            for (int i = 0; i < size; i++) {
                List<Node> column = find(Coord.column, row.get(i).element.getBoundingBox().left,
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
                            result.boundingBox = node.element.getBoundingBox();
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
