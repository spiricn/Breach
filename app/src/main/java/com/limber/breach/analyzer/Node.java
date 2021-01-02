package com.limber.breach.analyzer;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Single grid node representing a piece of analyzed text
 */
public class Node implements Parcelable {
    public enum Coord {
        row,
        column
    }


    double get(Coord coord) {
        switch (coord) {
            case row:
                return Objects.requireNonNull(boundingBox).top;
            case column:
                return Objects.requireNonNull(boundingBox).left;
        }

        return 0;
    }

    /**
     * Detected text
     */
    public String text;

    /**
     * Text bounding box in the source bitmap
     */
    public Rect boundingBox;

    public Node(Rect boundingBox, String text) {
        this.text = text;
        this.boundingBox = new Rect(boundingBox);
    }

    protected Node(Parcel in) {
        text = in.readString();
        boundingBox = in.readParcelable(Rect.class.getClassLoader());
    }

    public static final Creator<Node> CREATOR = new Creator<Node>() {
        @Override
        public Node createFromParcel(Parcel in) {
            return new Node(in);
        }

        @Override
        public Node[] newArray(int size) {
            return new Node[size];
        }
    };

    public Node(String text, Rect boundingBox) {
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