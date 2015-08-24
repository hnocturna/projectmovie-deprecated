package com.hnoct.projectmovie;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.OutputStream;

/**
 * Created by Nocturna on 8/22/2015.
 */
public class Movie implements Parcelable {
    Bitmap thumbnail;
    int movieId;
    String movieTitle;
    String overview;
    String releaseDate;
    String rating;

    public Movie(int movieId, String movieTitle, String overview, String releaseDate, String rating, Bitmap thumbnail) {
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.rating = rating;
        this.thumbnail = thumbnail;
    }

    public static final Parcelable.Creator<Movie> CREATOR = new Parcelable.Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel source) {
            return new Movie(source);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    private Movie(Parcel in) {
        this.movieId = in.readInt();
        this.movieTitle = in.readString();
        this.overview = in.readString();
        this.releaseDate = in.readString();
        this.rating = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(movieId);
        dest.writeString(movieTitle);
        dest.writeString(overview);
        dest.writeString(releaseDate);
        dest.writeString(rating);
    }

    public int getMovieId() {
        return movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public String getOverview() {
        return overview;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getRating() {
        return rating;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void addThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }
}
