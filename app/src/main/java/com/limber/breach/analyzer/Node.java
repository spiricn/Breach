package com.limber.breach.analyzer;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Single grid node representing a piece of analyzed text
 */
public class Node implements Parcelable {
    /**
     * Detected text
     */
    public String text;

    /**
     * Text boudning box in the source bitmap
     */
    public Rect boundingBox;

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