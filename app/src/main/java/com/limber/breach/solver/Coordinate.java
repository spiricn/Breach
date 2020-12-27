package com.limber.breach.solver;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Coordinate {
    public int row;

    public int column;

    Coordinate(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public static Coordinate from(int row, int column) {
        return new Coordinate(row, column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Coordinate other = (Coordinate) obj;
        if (column != other.column) {
            return false;
        }
        return row == other.row;
    }

    @NonNull
    @Override
    public String toString() {
        return "{" + row + "," + column + "}";
    }
}