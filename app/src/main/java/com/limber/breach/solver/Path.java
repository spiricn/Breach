package com.limber.breach.solver;


import androidx.annotation.NonNull;

import java.util.LinkedHashSet;
import java.util.Set;

public class Path {
    public Path(Set<Coordinate> coords) {
        mCoords.addAll(coords);
    }

    public Path() {
    }

    public Path(Coordinate coord) {
        mCoords.add(coord);
    }

    public Path add(Path other) {
        Set<Coordinate> newCoords = new LinkedHashSet<>(mCoords);

        for (Coordinate otherCoord : other.mCoords) {
            if (!newCoords.add(otherCoord)) {
                return null;
            }
        }

        return new Path(newCoords);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("[ ");

        for (Coordinate coord : mCoords) {
            builder.append(coord.toString());
        }

        builder.append(" ]");

        return builder.toString();
    }

    public Set<Coordinate> coordinates() {
        return mCoords;
    }

    private final Set<Coordinate> mCoords = new LinkedHashSet<>();
}