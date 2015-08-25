package com.hnoct.projectmovie;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * A placeholder fragment containing a simple view.
 */
public class MoviesFragment extends Fragment {
    MovieAdapter mThumbnailAdapter;
    List<Movie> moviesArray;
    String sortMode;

    public MoviesFragment() {
        setHasOptionsMenu(true);
        Activity activity = getActivity();
        if (activity != null && isAdded()) {
            sortMode = getResources().getString(R.string.sort_dialog_popularity);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_movies, container, false);

        final ArrayList<Movie> movieArray = new ArrayList<>();

        GridView movieGrid = (GridView) rootView.findViewById(R.id.movie_grid);
        mThumbnailAdapter = new MovieAdapter(movieArray, getActivity());

        movieGrid.setAdapter(mThumbnailAdapter);

        movieGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // Pass the selected movie to the next activity as a parcelable.

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected movie from the Array
                Movie selectedMovie = movieArray.get(position);

                // Thumbnail must be passed separately because it is parcelable and I don't feel like writing another method for it specifically.
                Bitmap movieThumbnail = selectedMovie.getThumbnail();

                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra("movie", selectedMovie);
                intent.putExtra("thumbnail", movieThumbnail);
                startActivity(intent);
            }
        });

        new FetchMoviePosters().execute();

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sort) {
            final Dialog dialog = new Dialog(getActivity());
            dialog.setTitle(getString(R.string.sort_dialog_title));
            dialog.setContentView(R.layout.sort_dialog);

            final RadioGroup sortRadioGroup = (RadioGroup) dialog.findViewById(R.id.sort_dialog_radiogroup);
            final RadioButton popularityRadio = (RadioButton) dialog.findViewById(R.id.sort_dialog_popularity_radio);
            final RadioButton ratingRadio = (RadioButton) dialog.findViewById(R.id.sort_dialog_rating_radio);
            final Button sortButton = (Button) dialog.findViewById(R.id.sort_dialog_sort_button);

            if (sortMode == null || sortMode.equals(getString(R.string.sort_dialog_popularity))) {
                sortRadioGroup.check(R.id.sort_dialog_popularity_radio);
            } else {
                sortRadioGroup.check(R.id.sort_dialog_rating_radio);
            }

            sortButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (sortRadioGroup.getCheckedRadioButtonId() == R.id.sort_dialog_popularity_radio) {
                        sortMode = getString(R.string.sort_dialog_popularity);
                        mThumbnailAdapter.clear();
                        for (Movie movie : moviesArray) {
                            mThumbnailAdapter.add(movie);
                        }
                    } else if (sortRadioGroup.getCheckedRadioButtonId() == R.id.sort_dialog_rating_radio) {
                        sortMode = getString(R.string.sort_dialog_rating);
                        Map<Double, Movie[]> popularityMap = new TreeMap<>(Collections.reverseOrder());
                        mThumbnailAdapter.clear();
                        for (Movie movie : moviesArray) {
                            if (popularityMap.get(Double.parseDouble(movie.getRating())) == null) {
                                popularityMap.put(Double.parseDouble(movie.getRating()), new Movie[] {movie});
                            } else {
                                Movie[] tempMovieArray = popularityMap.get(Double.parseDouble(movie.getRating()));
                                Movie[] newTempMovieArray = new Movie[tempMovieArray.length + 1];
                                for (int i = 0; i < tempMovieArray.length; i++) {
                                    newTempMovieArray[i] = tempMovieArray[i];
                                }
                                newTempMovieArray[newTempMovieArray.length -1] = movie;
                                popularityMap.put(Double.parseDouble(movie.getRating()), newTempMovieArray);
                            }
                        }
                        for (Movie[] movieArray : popularityMap.values()) {
                            for (Movie movie : movieArray) {
                                mThumbnailAdapter.add(movie);
                            }

                        }
                    }
                    dialog.dismiss();
                }
            });


            dialog.show();
        }

        return super.onOptionsItemSelected(item);
    }

    /*
            * Fetches the movie posters from The MovieDB and returns an array of posters to display on the MainActivity
             */
    private class FetchMoviePosters extends AsyncTask<Void, Void, Movie[]> {

        private Movie[] getMoviesFromJson(String movieJsonStr) throws JSONException {
            // Array to hold all the movie objects from The MovieDB
            Movie[] movieArray;

            // Variables to be used to select the JSON objects and build each movie object
            final String TMD_POSTER_BASE = "https://image.tmdb.org/t/p/w185";
            final String TMD_RESULTS = "results";
            final String TMD_ID = "id";
            final String TMD_TITLE = "title";
            final String TMD_POSTER = "poster_path";
            final String TMD_OVERVIEW = "overview";
            final String TMD_VOTE = "vote_average";
            final String TMD_RELEASE_DATE = "release_date";

            JSONObject movieJson = new JSONObject(movieJsonStr);
            JSONArray movieJsonArray = movieJson.getJSONArray(TMD_RESULTS);

            movieArray = new Movie[movieJsonArray.length()];
            for (int i = 0; i < movieJsonArray.length(); i++) {
                // Populate the values for each of the movie object
                int movieId;
                String movieTitle;
                String movieOverview;
                String movieReleaseDate;
                String movieRating;
                Bitmap movieThumbnail = null;

                JSONObject movieJsonObj = movieJsonArray.getJSONObject(i);
                movieId = movieJsonObj.getInt(TMD_ID);
                movieTitle = movieJsonObj.getString(TMD_TITLE);
                movieOverview = movieJsonObj.getString(TMD_OVERVIEW);
                movieReleaseDate = movieJsonObj.getString(TMD_RELEASE_DATE);
                movieRating = movieJsonObj.getString(TMD_VOTE);

                // Build the URL from the base string and the movie poster ID path for each of the
                // movies and download the Bitmap to be saved to the movie object.
                String moviePosterId = movieJsonObj.getString(TMD_POSTER);
                String moviePosterPath = TMD_POSTER_BASE + moviePosterId;
                try {
                    // Build the URL, set the connection, and connect
                    URL url = new URL(moviePosterPath);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setDoInput(true);
                    urlConnection.connect();

                    // Download the poster thumbnail as a bitmap and add it to the movie object.
                    InputStream bitmapStream = urlConnection.getInputStream();
                    movieThumbnail = BitmapFactory.decodeStream(bitmapStream);
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, "Malformed movie poster URL", e);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error downloading poster image", e);
                }
                // Add the movie to the array. Array will then be used to populate the Adapter.
                movieArray[i] = new Movie(movieId, movieTitle, movieOverview, movieReleaseDate, movieRating, movieThumbnail);
            }
            return movieArray;
        }

        private Bitmap[] getPostersFromJson(String movieJsonStr) throws JSONException {
            // Generates the URLs of the posters to display.

            // Variables to be used to select the JSON objects and build the final poster path
            final String TMD_POSTER_BASE = "https://image.tmdb.org/t/p/w185";

            final String TMD_RESULTS = "results";
            final String TMD_POSTER = "poster_path";

            // Holds an ArrayList of all the poster thumbnails
            Bitmap[] thumbnailArray;

            // Convert the String to a JSON object, select the array, and then individually add each URL for the poster thumbnail to an Array.
            JSONObject movieJson = new JSONObject(movieJsonStr);

            JSONArray movieArray = movieJson.getJSONArray(TMD_RESULTS);

            URL[] posterArray = new URL[movieArray.length()];
            for (int i = 0; i < movieArray.length(); i++) {
                JSONObject moviePoster = movieArray.getJSONObject(i);

                String posterId = moviePoster.getString(TMD_POSTER);
                String posterPath = TMD_POSTER_BASE + posterId;
                try {
                    posterArray[i] = new URL(posterPath);
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, posterPath + " is not a URL", e);
                }
            }

            // Array to hold all thumbnail images
            thumbnailArray = new Bitmap[posterArray.length];

            // For each generated URL, download the image as a bitmap and store it in the Array.
            for (int i = 0; i < posterArray.length; i++) {
                HttpURLConnection urlConnection;

                URL posterUrl = posterArray[i];

                try {
                    urlConnection = (HttpURLConnection) posterUrl.openConnection();
                    urlConnection.setDoInput(true);
                    urlConnection.connect();

                    InputStream bitStream = urlConnection.getInputStream();
                    Bitmap movieThumbnail = BitmapFactory.decodeStream(bitStream);

                    thumbnailArray[i] = movieThumbnail;
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error downloading movie poster thumbnail", e);
                }
            }
            return thumbnailArray;
        }

        String LOG_TAG = FetchMoviePosters.class.getSimpleName();

        @Override
        protected Movie[] doInBackground(Void... params) {
            // Variables are declared outside of the try/catch block so they can be used in the finally block
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Contains RAW JSON response as a String
            String movieJsonStr = null;
            try {
                // Parameters used to query the API
                final String BASE_URL = "https://api.themoviedb.org/3";
                final String TMD_DISCOVER = "discover";
                final String TMD_MOVIE = "movie";
                final String TMD_API_PARAM = "api_key";
                final String API_KEY = "b1f582365e9ca840bbf384a03c4c37cd";
                final String TMD_ADULT_PARAM = "include_adult";

                // Build the Uri using the above parameters
                Uri uri = Uri.parse(BASE_URL).buildUpon()
                        .appendPath(TMD_DISCOVER)
                        .appendPath(TMD_MOVIE)
                        .appendQueryParameter(TMD_ADULT_PARAM, "true")
                        .appendQueryParameter(TMD_API_PARAM, API_KEY)
                        .build();

                Log.v(LOG_TAG, "Built Uri: " + uri.toString());
                URL url = new URL(uri.toString());

                // Create request and open a connection to the API
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if (inputStream == null) {
                    // Do nothing if no data from website.
                    return null;
                }

                if (buffer == null) {
                    // Empty stream. Do nothing.
                    return null;
                }

                reader = new BufferedReader((new InputStreamReader(inputStream)));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Make the JSON data easier to read by breaking into lines.
                    buffer.append(line + "\n");
                }

                movieJsonStr = buffer.toString();
                // Log.v(LOG_TAG + "/JSON", movieJsonStr);

            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Malformed url: ", e);
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to connect to the website!", e);
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error closing stream.", e);
                    }
                }
            }
            try {
                return getMoviesFromJson(movieJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON Exception: " + e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Movie[] movies) {
            mThumbnailAdapter.clear();
            moviesArray = new ArrayList<>();
            if (movies != null) {
                for (Movie movie: movies) {
                    moviesArray.add(movie);
                    mThumbnailAdapter.add(movie);
                }
            }
            super.onPostExecute(movies);
        }
    }
}
