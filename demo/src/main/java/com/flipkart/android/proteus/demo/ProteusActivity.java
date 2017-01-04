/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION
 *
 * Copyright (c) 2017 Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.flipkart.android.proteus.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import com.flipkart.android.proteus.Layout;
import com.flipkart.android.proteus.Value;
import com.flipkart.android.proteus.inflater.DataAndViewParsingLayoutInflater;
import com.flipkart.android.proteus.inflater.LayoutInflaterFactory;
import com.flipkart.android.proteus.inflater.ProteusLayoutInflater;
import com.flipkart.android.proteus.demo.converter.GsonConverterFactory;
import com.flipkart.android.proteus.demo.models.JsonResource;
import com.flipkart.android.proteus.gson.ProteusTypeAdapterFactory;
import com.flipkart.android.proteus.parser.BaseTypeParser;
import com.flipkart.android.proteus.toolbox.BitmapLoader;
import com.flipkart.android.proteus.toolbox.EventType;
import com.flipkart.android.proteus.toolbox.ImageLoaderCallback;
import com.flipkart.android.proteus.toolbox.LayoutInflaterCallback;
import com.flipkart.android.proteus.toolbox.Styles;
import com.flipkart.android.proteus.view.ProteusView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import retrofit2.Call;
import retrofit2.Retrofit;


public class ProteusActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8080/data/";
    private Retrofit retrofit;
    private JsonResource resources;

    private ViewGroup container;

    private DataAndViewParsingLayoutInflater layoutInflater;

    private JsonObject data;
    private Layout layout;

    private Styles styles;
    private Map<String, Layout> layouts;

    /**
     * Simple implementation of BitmapLoader for loading images from url in background.
     */
    private BitmapLoader bitmapLoader = new BitmapLoader() {

        @Override
        public Future<Bitmap> getBitmap(String imageUrl, ProteusView view) {
            return null;
        }

        @Override
        public void getBitmap(ProteusView view, String imageUrl, final ImageLoaderCallback callback, Layout layout) {
            URL url;

            try {
                url = new URL(imageUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return;
            }

            new AsyncTask<URL, Integer, Bitmap>() {
                @Override
                protected Bitmap doInBackground(URL... params) {
                    try {
                        return BitmapFactory.decodeStream(params[0].openConnection().getInputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                protected void onPostExecute(Bitmap result) {
                    callback.onResponse(result);
                }
            }.execute(url);
        }
    };

    /**
     * Implementation of LayoutInflaterCallback. This is where we get callbacks from proteus regarding
     * errors and events.
     */
    private LayoutInflaterCallback callback = new LayoutInflaterCallback() {

        @Override
        public void onUnknownAttribute(ProteusView view, int attribute, Value value) {

        }

        @Nullable
        @Override
        public ProteusView onUnknownViewType(String type, View parent, Layout layout, JsonObject data, Styles styles, int index) {
            return null;
        }

        @Override
        public Layout onLayoutRequired(String type, Layout include) {
            return null;
        }

        @Override
        public void onViewBuiltFromViewProvider(ProteusView view, View parent, String type, int index) {

        }

        @Override
        public View onEvent(ProteusView view, EventType eventType, Value value) {
            return null;
        }

        @Override
        public PagerAdapter onPagerAdapterRequired(ProteusView parent, List<ProteusView> children, Layout layout) {
            return null;
        }

        @Override
        public Adapter onAdapterRequired(ProteusView parent, List<ProteusView> children, Layout layout) {
            return null;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (null == retrofit) {
            ProteusTypeAdapterFactory factory =  new ProteusTypeAdapterFactory();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapterFactory(factory)
                    .create();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }

        if (null == resources) {
            resources = retrofit.create(JsonResource.class);
        }

        // create a new DataAndViewParsingLayoutInflater
        // and set layouts, callback and image loader.
        layoutInflater = new LayoutInflaterFactory().getDataAndViewParsingLayoutInflater(layouts);
        layoutInflater.setCallback(callback);
        layoutInflater.setBitmapLoader(bitmapLoader);

        registerCustomViews(layoutInflater);

        ProteusTypeAdapterFactory.PROTEUS_INSTANCE_HOLDER.setInflater(layoutInflater);

        fetch();
    }

    private void registerCustomViews(ProteusLayoutInflater layoutInflater) {
        BaseTypeParser parser = (BaseTypeParser) layoutInflater.getParser("View");
        layoutInflater.registerParser("CircleView", new CircleViewParser(parser));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_proteus);

        // set the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // handle refresh button click
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetch();
            }
        });

        container = (ViewGroup) findViewById(R.id.content_main);
    }

    private void setup() {
        container.removeAllViews();
        layoutInflater.setLayouts(layouts);
    }

    private void render() {
        // Inflate a new view using proteus
        long start = System.currentTimeMillis();
        ProteusView view = layoutInflater.inflate(container, layout, data, styles, 0);
        System.out.println(System.currentTimeMillis() - start);
        container.addView((View) view);
    }

    private void fetch() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {

                    Call<JsonObject> callData = resources.get("user.json");
                    data = callData.execute().body();

                    Call<Layout> callLayout = resources.getLayout();
                    layout = callLayout.execute().body();

                    Call<Map<String, Layout>> layoutsCall = resources.getLayouts();
                    layouts = layoutsCall.execute().body();

                    //Call<Styles> stylesCall = resources.getStyles();
                    //styles = stylesCall.execute().body();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                try {
                    setup();
                    render();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.fetch) {
            fetch();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
