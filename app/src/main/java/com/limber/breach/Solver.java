package com.limber.breach;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Solver {


    public static PathScore solve(List<List<Integer>> codeMatrix, List<List<Integer>> sequences, int bufferSize) {
        List<Path> paths = generatePaths(codeMatrix, bufferSize);

        PathScore maxScore = null;
        for (Path path : paths) {
            PathScore c = new PathScore(path, sequences, bufferSize, codeMatrix);
            if (maxScore == null || c.score() > maxScore.score()) {
                maxScore = c;
            }
        }

        return maxScore;
    }

    private static List<Coordinate> candidateCoords(List<List<Integer>> codeMatrix) {
        return candidateCoords(codeMatrix, 0, Coordinate.from(0, 0));
    }

    /**
     * Return next available row/column for specified turn and coordinate.
     * If it's the 1st turn the index is 0 so next_line would return the
     * first row. For the second turn, it would return the nth column, with n
     * being the coordinate's row
     */
    private static List<Coordinate> candidateCoords(List<List<Integer>> codeMatrix, int turn, Coordinate coordinate) {
        List<Coordinate> coords = new ArrayList<>();

        for (int i = 0; i < codeMatrix.size(); i++) {
            Coordinate coord;
            if (turn % 2 == 0) {
                coord = new Coordinate(coordinate.row, i);
            } else {
                coord = new Coordinate(i, coordinate.column);
            }

//            System.out.println("generate " + coord);
            coords.add(coord);
        }

        return coords;
    }

    /**
     * Returns all possible paths with size equal to the buffer
     */
    private static List<Path> generatePaths(List<List<Integer>> codeMatrix, int bufferSize) {
        Stack<Path> partialPathsStack = new Stack<>();
        partialPathsStack.push(new Path());

        List<Coordinate> candidateCoords = candidateCoords(codeMatrix);

        List<Path> completedPaths = new ArrayList<>();

        walkPaths(codeMatrix, bufferSize, completedPaths, partialPathsStack, 0, candidateCoords);

        return completedPaths;
    }

    static void walkPaths(List<List<Integer>> codeMatrix, int bufferSize, List<Path> completedPaths,
                          Stack<Path> partialPathsStack, int turn, List<Coordinate> candidateCoords) {
//        System.out.println(">>>>>>>>>>>>>>>");
        Path path = partialPathsStack.pop();

//        for (Coordinate coord : candidateCoords) {
//            System.out.print(" " + coord + ",");
//        }
//        System.out.print("\n");


        for (Coordinate coord : candidateCoords) {
//            System.out.println("push " + coord);
            Path newPath = path.add(new Path(coord));


//            System.out.println("path " + newPath);

            if (newPath == null) {
                // Skip coordinate if it has already been visited
//                System.out.println("skip");
                continue;
            }

            // Full path is added to the final return list and removed from the partial paths stack
            if (newPath.coordinates().size() == bufferSize) {
                completedPaths.add(newPath);
            } else {
                // Add new, lengthier partial path back into the stack
//                System.out.println(newPath.toString());
                partialPathsStack.push(newPath);
//
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                walkPaths(
                        codeMatrix, bufferSize, completedPaths, partialPathsStack, turn + 1,
                        candidateCoords(codeMatrix, turn + 1, coord)
                );
            }

        }
    }

    private List<List<Character>> mCodeMatrix;
//    private List<Path> mCompletedPath = new ArrayList<>();
}
