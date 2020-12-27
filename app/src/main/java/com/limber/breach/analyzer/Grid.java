package com.limber.breach.analyzer;


import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Grid implements Parcelable {
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
