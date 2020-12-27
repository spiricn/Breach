package com.limber.breach.analyzer;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

public class GridNode implements Parcelable {
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