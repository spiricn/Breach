package com.limber.breach;


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
        Set<Coordinate> newCoords = new LinkedHashSet<>();
        newCoords.addAll(mCoords);

//        for(Coordinate c : newCoords){
//            System.out.println("bla " + c);
//        }

//        System.out.println("add " + mCoords.size() + " " + other.mCoords.size());

        for (Coordinate otherCoord : other.mCoords) {
//            System.out.println("bla2 " + otherCoord);
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

    private Set<Coordinate> mCoords = new LinkedHashSet<>();
}