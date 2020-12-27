package com.limber.breach.analyzer;


import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class Result implements Parcelable {
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
