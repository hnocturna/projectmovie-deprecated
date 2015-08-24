package com.hnoct.projectmovie;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by hnoct on 8/21/2015.
 */
public class MovieAdapter extends BaseAdapter {
    ArrayList<Movie> movieArray;
    LayoutInflater inflater;

    public MovieAdapter(ArrayList<Movie> movieArray, Context context) {
        this.movieArray = movieArray;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return movieArray.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        movieArray.clear();
    }

    public void add(Movie movie) {
        movieArray.add(movie);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Initialize movie variables
        String movieTitle;
        String movieOverview;
        String movieReleaseDate;
        String movieRating;
        Bitmap movieThumbnail;

        // Get the movie at the current position
        Movie movie = movieArray.get(position);

        // Get movie variables
        movieTitle = movie.getMovieTitle();
        movieOverview = movie.getOverview();
        movieReleaseDate = movie.getReleaseDate();
        movieRating = movie.getRating();
        movieThumbnail = movie.getThumbnail();

        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.grid_item_thumbnail, parent, false);
            viewHolder.itemImageView = (ImageView) convertView.findViewById(R.id.grid_item_thumbnail_image);
            viewHolder.itemTextView = (TextView) convertView.findViewById(R.id.grid_item_thumbnail_text);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.itemTextView.setText(movieTitle);
        viewHolder.itemImageView.setImageBitmap(movieThumbnail);
        return convertView;
    }

    private class ViewHolder {
        ImageView itemImageView;
        TextView itemTextView;
    }
}
