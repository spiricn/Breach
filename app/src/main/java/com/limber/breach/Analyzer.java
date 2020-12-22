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

            filteredNodes.add(node);
        }

        result.matrixBoundingBox = matrixRes.boundingBox;
        result.matrix = new ArrayList<>();

        for (List<Node> matrixRow : matrixRes.matrix) {
            List<Integer> row = new ArrayList<>();
            for (Node node : matrixRow) {
                row.add(Integer.parseInt(node.element.getText(), 16));
            }

            result.matrix.add(row);
        }

        return result;
    }


    static class MatrixResult {
        List<Node> nodes;
        List<List<Node>> matrix;
        Rect boundingBox;
    }

    static class SequenceResult {
        List<List<Node>> sequences;
        Rect boundingBox;
    }

    private static MatrixResult processMatrix(Text texts) {

        List<Node> nodes = getElements(texts);

        List<Node> allNodes = new ArrayList<>();
        allNodes.addAll(nodes);

        int size = mBmp.getHeight();

        List<List<Node>> rows = new ArrayList<>();

        // Average size
        Pair<Double, Double> averageSize = getAverageSize(nodes);
        double averageWidth = averageSize.first;
        double averageHeight = averageSize.second;

        rows = splitBy(Coord.row, averageHeight, nodes);

        Collections.sort(rows, (o1, o2) -> Integer.compare(o1.get(0).element.getBoundingBox().top,
                o2.get(0).element.getBoundingBox().top));

        if (rows.size() < 5) {
            return null;
        }

        int rowIndex = -1;
        for (List<Node> row : rows) {
            rowIndex += 1;

            if (row.size() < 5) {
                continue;
            }

            List<List<Node>> columns = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                List<Node> column = find(Coord.column, row.get(i).element.getBoundingBox().left,
                        averageWidth, allNodes);
                if (column.size() < 5) {
                    break;
                }


                columns.add(column);
            }

            if (columns.size() == 5) {
                MatrixResult result = new MatrixResult();


                List<List<Node>> matrix = new ArrayList<>();

                for (int i = 0; i < 5; i++) {
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
            }
        }

        throw new RuntimeException("Not found");
    }

    private static List<Node> getElements(Text texts) {
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
