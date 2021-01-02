package com.limber.breach.analyzer;


import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid of analyzed text pieces
 */
public class Grid implements Parcelable {
    /**
     * List of node rows
     */
    public List<List<Node>> rows;

    /**
     * Grid bounding box (union of all node bounding boxes)
     */
    public Rect boundingBox;

    protected Grid(Parcel in) {
        boundingBox = in.readParcelable(Rect.class.getClassLoader());

        rows = new ArrayList<>();

        int numRows = in.readInt();
        for (int i = 0; i < numRows; i++) {
            rows.add(in.createTypedArrayList(Node.CREATOR));
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
        this.rows = new ArrayList<>();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(rows.size());
        for (List<Node> rows : this.rows) {
            parcel.writeArray(rows.toArray());
        }

        parcel.writeParcelable(boundingBox, i);
    }
}
