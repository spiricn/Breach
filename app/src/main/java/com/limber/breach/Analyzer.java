package com.limber.breach;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Analyzer {
    public static class GridNode implements Parcelable {
        public String text;
        public Rect boundingBox;

        protected GridNode(Parcel in) {
            text = in.readString();
            boundingBox = in.readParcelable(Rect.class.getClassLoader());
        }

        public static final Creator<GridNode> CREATOR = new Creator<GridNode>() {
            @Override
            public GridNode createFromParcel(Parcel in) {
                return new GridNode(in);
            }

            @Override
            public GridNode[] newArray(int size) {
                return new GridNode[size];
            }
        };

        public GridNode(String text, Rect boundingBox) {
            this.text = text;
            this.boundingBox = boundingBox;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.text);
            this.boundingBox.writeToParcel(parcel, i);
        }
    }

    public static class Grid implements Parcelable {
        public List<List<GridNode>> nodes;
        public Rect boundingBox;

        protected Grid(Parcel in) {
            boundingBox = in.readParcelable(Rect.class.getClassLoader());

            nodes = new ArrayList<>();

            int numRows = in.readInt();
            for (int i = 0; i < numRows; i++) {
                nodes.add(in.createTypedArrayList(GridNode.CREATOR));
            }
        }

        public static final Creator<Grid> CREATOR = new Creator<Grid>() {
            @Override
            public Grid createFromParcel(Parcel in) {
                return new Grid(in);
            }

            @Override
            public Grid[] newArray(int size) {
                return new Grid[size];
            }
        };

        public Grid() {
            this.nodes = new ArrayList<>();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(nodes.size());
            for (List<GridNode> rows : this.nodes) {
                parcel.writeArray(rows.toArray());
            }

            parcel.writeParcelable(boundingBox, i);
        }
    }

    public static class Result implements Parcelable {
        public Grid matrix;
        public Grid sequences;
        public Bitmap bitmap;

        protected Result(Parcel in) {
            matrix = in.readParcelable(Grid.class.getClassLoader());
            sequences = in.readParcelable(Grid.class.getClassLoader());
            bitmap = in.readParcelable(Bitmap.class.getClassLoader());
        }

        public Result() {
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(matrix, flags);
            dest.writeParcelable(sequences, flags);
            dest.writeParcelable(bitmap, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Result> CREATOR = new Creator<Result>() {
            @Override
            public Result createFromParcel(Parcel in) {
                return new Result(in);
            }

            @Override
            public Result[] newArray(int size) {
                return new Result[size];
            }
        };
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

        public int value() {
            return Integer.valueOf(element.getText(), 16);
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

        result.matrix = convertGrid(matrixRes.matrix);
        result.bitmap = mBmp;

        List<Node> sequenceNodes = new ArrayList<>();
        for (Node node : matrixRes.nodes) {
            if (node.element.getBoundingBox().intersect(matrixRes.boundingBox)) {
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

        result.sequences = convertGrid(splitBy(Coord.row, matrixRes.averageWidth, sequenceNodes));

        return result;
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
