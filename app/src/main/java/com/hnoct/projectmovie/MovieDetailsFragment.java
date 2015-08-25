package com.hnoct.projectmovie;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A placeholder fragment containing a simple view.
 */
public class MovieDetailsFragment extends Fragment {
    String LOG_TAG = MovieDetailsFragment.class.getSimpleName();
    Movie movie;

    // Initialize the views that need to be populated. These are global variables such that the
    // FetchMovieImages method can populate them if need be.
    ImageView movieBackdropIv;
    ImageView moviePosterIv;
    TextView movieTitleTv;
    TextView movieReleaseTv;
    TextView movieRatingTv;
    TextView movieOverviewTv;

    public MovieDetailsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_details, container, false);
        // Get the selected Movie object and add the Bitmap thumbnail back to the object.
        Bundle extras = getActivity()
                .getIntent()
                .getExtras();

        if (extras.getParcelable("movie") != null) {
            movie = extras.getParcelable("movie");
            movie.addThumbnail((Bitmap) extras.getParcelable("thumbnail"));
        } else {
            // If no is passed to this activity, show a toast indicating and end the Activity
            Toast toast;
            int duration = Toast.LENGTH_SHORT;
            String toastText = "No movie passed! Select a movie.";

            toast = Toast.makeText(getActivity().getApplication(),
                    toastText,
                    duration);

            toast.show();
            getActivity().finish();
            Log.d(LOG_TAG, "No activity passed to MovieDetailsFragment");
        }

        // Populate the movie variables that will be used to populate the views below
        final String movieTitle = movie.getMovieTitle();
        final String movieOverview = movie.getOverview();
        final String movieReleaseDate = movie.getReleaseDate();
        final String movieRating = movie.getRating();
        final int movieId = movie.getMovieId();
        final Bitmap moviePoster = movie.getThumbnail();

        // Initialize the layout items to be populated
        movieBackdropIv = (ImageView) rootView.findViewById(R.id.movie_backdrop);
        moviePosterIv = (ImageView) rootView.findViewById(R.id.movie_poster);
        movieTitleTv = (TextView) rootView.findViewById(R.id.movie_title);
        movieReleaseTv = (TextView) rootView.findViewById(R.id.movie_release);
        movieRatingTv = (TextView) rootView.findViewById(R.id.movie_rating);
        movieOverviewTv = (TextView) rootView.findViewById(R.id.movie_overview);

        // Populate the views with the data from the movie object
        movieTitleTv.setText(movieTitle);
        movieReleaseTv.setText(movieReleaseDate);
        movieRatingTv.setText(movieRating);
        movieOverviewTv.setText(movieOverview);
        moviePosterIv.setImageBitmap(moviePoster);

        new FetchMovieImages().execute(movieId);

        return rootView;
    }

    private class FetchMovieImages extends AsyncTask<Integer, Void, Bitmap[]> {
        String LOG_TAG = FetchMovieImages.class.getSimpleName();

        private Bitmap[] getMovieImages(String imageJsonStr) throws JSONException {
            // Method to get the file paths and images for the movie's backdrop and poster to be
            // used in the Activity

            // Variables that need to be returned
            Bitmap[] movieImages = new Bitmap[1];
            Bitmap movieBackdrop;
            Bitmap moviePoster;

            // Variables used to select the JSON objects, arrays, and eventually the String
            // file paths of the poster and backdrop
            final String JSON_BACKDROPS = "backdrops";
            final String JSON_POSTERS = "posters";
            final String JSON_FILE_PATH = "file_path";

            // Save the file path of the backdrop as a String to be used to build the URL for the
            // backdrop image
            JSONObject imageJsonObj = new JSONObject(imageJsonStr);

            JSONArray backdropJsonArray = imageJsonObj.getJSONArray(JSON_BACKDROPS);
            // Use the highest voted backdrop (the backdrop at position 0)
            JSONObject backdropJsonObj = backdropJsonArray.getJSONObject(0);
            String backdropFilePath = backdropJsonObj.getString(JSON_FILE_PATH);

            JSONArray posterJsonArray = imageJsonObj.getJSONArray(JSON_POSTERS);
            // Use the highest voted poster (the poster at position 0)
            JSONObject posterJsonObj = posterJsonArray.getJSONObject(0);
            String posterFilePath = posterJsonObj.getString(JSON_FILE_PATH);

            // Variables to be used to build the URL for the poster and backdrop images
            final String TMD_POSTER_BASE = "https://image.tmdb.org/t/p/w185/";
            final String TMD_BACKDROP_BASE = "https://image.tmdb.org/t/p/w780";
            try {
                // Set and open the connection, then download the backdrop image
                URL backdropUrl = new URL(TMD_BACKDROP_BASE + backdropFilePath);
                HttpURLConnection urlConnection = (HttpURLConnection) backdropUrl.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.connect();

                InputStream bitmapStream = urlConnection.getInputStream();
                movieBackdrop = BitmapFactory.decodeStream(bitmapStream);
                movieImages[0] = movieBackdrop;

            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Malformed URL for movie poster or image", e);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error downloading backdrop image", e);
            }

            return movieImages;
        }

        @Override
        protected Bitmap[] doInBackground(Integer... params) {
            // Variables declared outside of the try/catch block so they can be used in the finally
            // block
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Variables used to build the Uri and download the poster images and backdrop
            int movieId = params[0];
            String movieIdStr = Integer.toString(movieId);

            // Variable to hold the JSON data downloaded from the website
            String imageJsonStr = null;

            final String TMD_BASE_URL = "http://api.themoviedb.org/3/movie/";
            final String TMD_IMAGES = "images";
            final String TMD_API_PARAM = "api_key";
            final String API_KEY = "b1f582365e9ca840bbf384a03c4c37cd";

            try {
                // Build the URL using the above parameters for the specific movie selected,
                // set and open the connection to the website, and get the links for the movie
                // poster and backdrop
                Uri builtUri = Uri.parse(TMD_BASE_URL)
                        .buildUpon()
                        .appendPath(movieIdStr)
                        .appendPath(TMD_IMAGES)
                        .appendQueryParameter(TMD_API_PARAM, API_KEY)
                        .build();

                Log.v(LOG_TAG, "Built Uri: " + builtUri.toString());

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if (inputStream == null) {
                    // Input is null. Do nothing.
                    return null;
                }
                if (buffer == null) {
                    // There is no data in the InputStream. Do nothing.
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                imageJsonStr = buffer.toString();

            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Malformed URL", e);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to website!", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error closing Buffered Reader", e);
                    }
                }
            }

            try {
                return getMovieImages(imageJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON Exception downloading movie images", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap[] bitmaps) {
            if (bitmaps != null) {
                Bitmap movieBackdrop = bitmaps[0];
                movieBackdropIv.setImageBitmap(movieBackdrop);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    movieBackdropIv.setImageAlpha(100);
                } else {
                    movieBackdropIv.setAlpha(100);
                }

            } else {
                Log.d(LOG_TAG, "No backdrop image found");
            }
            super.onPostExecute(bitmaps);
        }
    }
}
