package com.newpage.hack2;

import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.transition.Scene;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.easywaylocation.EasyWayLocation;
import com.example.easywaylocation.Listener;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.Metadata;
import com.here.sdk.core.Point2D;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.gestures.TapListener;
import com.here.sdk.mapviewlite.MapImage;
import com.here.sdk.mapviewlite.MapImageFactory;
import com.here.sdk.mapviewlite.MapMarker;
import com.here.sdk.mapviewlite.MapMarkerImageStyle;
import com.here.sdk.mapviewlite.MapPolyline;
import com.here.sdk.mapviewlite.MapPolylineStyle;
import com.here.sdk.mapviewlite.MapStyle;
import com.here.sdk.mapviewlite.MapViewLite;
import com.here.sdk.mapviewlite.PixelFormat;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.Waypoint;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ArrayList<HerePoint> currentRoute;
    private boolean isMarked = false;

    private boolean isPolylined = false;

    AlertDialog.Builder builder;

    private GeoPolyline polyline;
    private MapPolyline mapPolyline;

    class DownloadRouteTask extends AsyncTask<Integer, Void, ArrayList<HerePoint>> {

        @Override
        protected ArrayList<HerePoint> doInBackground(Integer... voids) {
            ArrayList<HerePoint> a = new ArrayList<>();

            MediaType type = MediaType.parse("application/x-www-form-urlencoded");
            OkHttpClient client = new OkHttpClient();

            RequestBody body = RequestBody.create("id=" + voids[0], type);
            Request request = new Request.Builder().url("https://newpage.ddns.net/tangle/api/points.php")
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                StringBuilder ans = new StringBuilder(response.body().string());
                ans.insert(0, "[");
                ans.append("]");
                if (ans.equals("false")) {
                    return a;
                } else {
                    JSONArray array = new JSONArray(ans.toString());
                    for (int i = 0; i < array.length(); i++) {
                        HerePoint point = new HerePoint(new GeoCoordinates(
                                array.getJSONObject(i).getDouble("lat"),
                                array.getJSONObject(i).getDouble("alt")
                        ), array.getJSONObject(i).getString("title"),
                                MapImageFactory.fromResource(MainActivity.this.getResources(), R.drawable.marker));
                        a.add(point);
                    }
                }
            } catch (IOException ex) {
                Log.e("ea", "ead");
            } catch (JSONException jex) {
                Log.e("Json", "jex");
            }

            return a;
        }

        @Override
        protected void onPostExecute(ArrayList<HerePoint> result) {
            currentRoute = result;

            ArrayList<Waypoint> waypoints = new ArrayList<>();
            waypoints.add(new Waypoint(myLocation));
            for (HerePoint h : result) {
                waypoints.add(h.waypoint);
            }

            if (!isMarked)
                makeMarkers(result);
            else {
                deleteMarker(result);
                makeMarkers(result);
            }
            updateRoute(waypoints);
        }
    }

    private void makeMarkers(ArrayList<HerePoint> result) {
        for (HerePoint h : result) {
            mapView.getMapScene().addMapMarker(h.mapMarker);
        }
    }

    private void deleteMarker(ArrayList<HerePoint> result) {
        for (HerePoint h : result) {
            mapView.getMapScene().removeMapMarker(h.mapMarker);
        }
    }

    private void updateRoute(ArrayList<Waypoint> waypoints) {
        engine.calculateRoute(waypoints, new RoutingEngine.CarOptions(), (routingError, list) -> {
            if (routingError == null) {
                showRouteOnMap(list.get(0));
            }
        });
    }

    private static final String TAG = MainActivity.class.getSimpleName();
    private MapViewLite mapView;

    private EasyWayLocation easyLocation;

    private GeoCoordinates myLocation = new GeoCoordinates(0, 0);

    private RoutingEngine engine = null;

    private MapImage myMarkerImage = null;

    private MapMarker myMarker = new MapMarker(myLocation);

    boolean isMyMarkerLanded = false;

    private void from1to2() {
        ViewGroup rootLayout = findViewById(R.id.odinLayout);
        final Scene scene2 = Scene.getSceneForLayout(rootLayout, R.layout.expandedbuttons, this);
        TransitionSet set = new TransitionSet();
        set.addTransition(new Slide(Gravity.LEFT));
        set.setOrdering(TransitionSet.ORDERING_TOGETHER);
        set.setDuration(600);
        set.setInterpolator(new AccelerateInterpolator());
        TransitionManager.go(scene2, set);

        View view2 = findViewById(R.id.closeButton);
        View lkButton = findViewById(R.id.lkButton);
        lkButton.setOnClickListener(v -> {
            startActivity(new Intent(this, Lk.class));
        });
        view2.setOnClickListener(k -> {
            final Scene scene1 = Scene.getSceneForLayout(rootLayout, R.layout.startwithexoand, this);
            set.setOrdering(TransitionSet.ORDERING_TOGETHER);
            set.setDuration(1000);
            set.setInterpolator(new AccelerateInterpolator());
            TransitionManager.go(scene1, set);

            View newView1 = findViewById(R.id.fab);
            newView1.setOnClickListener(v -> from1to2());
        });
    }

    ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        builder = new AlertDialog.Builder(this);

        Intent intent = getIntent();
        int id = intent.getIntExtra("id", 0);

        // Get a MapView instance from the layout.
        mapView = findViewById(R.id.map_view);
        myMarkerImage = MapImageFactory.fromResource(MainActivity.this.getResources(), R.drawable.marker);
        mapView.onCreate(savedInstanceState);
        myMarker.addImage(myMarkerImage, new MapMarkerImageStyle());
        setTapGestureHandler();

        View view = findViewById(R.id.fab);
        view.setOnClickListener(v -> from1to2());

        initEasyGeo();

        try {
            engine = new RoutingEngine();
            new DownloadRouteTask().execute(id);
        } catch (InstantiationErrorException iex) {
            Toast.makeText(MainActivity.this, "Couldnt create rout", Toast.LENGTH_LONG);
        }
    }


    private void setTapGestureHandler() {
        mapView.getGestures().setTapListener(new TapListener() {
            @Override
            public void onTap(Point2D touchPoint) {
                pickMapMarker(touchPoint);
            }
        });
    }

    private void pickMapMarker(final Point2D touchPoint) {
        float radiusInPixel = 2;
        mapView.pickMapItems(touchPoint, radiusInPixel, pickMapItemsResult -> {
            if (pickMapItemsResult == null) {
                return;
            }

            MapMarker topmostMapMarker = pickMapItemsResult.getTopmostMarker();
            if (topmostMapMarker == null) {
                return;
            }
            Metadata metadata = topmostMapMarker.getMetadata();
            String message = metadata.getString("message");
            String title = metadata.getString("title");

            AlertDialog.Builder dialog = builder
                    .setMessage(message)
                    .setTitle(title);
            if (distance(myLocation, topmostMapMarker.getCoordinates()) < 100) {
                builder.setPositiveButton("я там был", (dialog1, which) -> {
                    int i = 0;
                    for (; i < currentRoute.size(); i++) {
                        if (currentRoute.get(i).description.equals(
                                message
                        )) {
                            break;
                        }
                    }
                    deleteMarker(currentRoute);
                    removePolyline();
                    currentRoute.remove(i);
                    ArrayList<Waypoint> waypoints = new ArrayList<>();
                    waypoints.add(new Waypoint(myLocation));
                    for (HerePoint h : currentRoute) {
                        waypoints.add(h.waypoint);
                    }
                    updateRoute(waypoints);
                    makeMarkers(currentRoute);

                });
            }


            dialog.show();
        });
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6372.8;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }


    private double distance(GeoCoordinates myLocation, GeoCoordinates coordinates) {
        return haversine(
                myLocation.latitude, myLocation.longitude,
                coordinates.latitude, coordinates.longitude
        );
    }


    private void showRouteOnMap(Route route) {
        if (isPolylined) {
            removePolyline();
        }
        try {
            polyline = new GeoPolyline(route.getPolyline());
            MapPolylineStyle mapPolylineStyle = new MapPolylineStyle();
            mapPolylineStyle.setColor(0x00908AA0, PixelFormat.RGBA_8888);
            mapPolylineStyle.setWidth(10);
            mapPolyline = new MapPolyline(polyline, mapPolylineStyle);
            mapView.getMapScene().addMapPolyline(mapPolyline);
            isPolylined = true;
        } catch (InstantiationErrorException iex) {
            Toast.makeText(MainActivity.this, "er", Toast.LENGTH_LONG);
        }
    }

    private void removePolyline() {
        mapView.getMapScene().removeMapPolyline(mapPolyline);
    }

    private void initEasyGeo() {
        easyLocation = new EasyWayLocation(MainActivity.this, false, new Listener() {
            @Override
            public void locationOn() {

            }

            @Override
            public void currentLocation(Location location) {
                myLocation = new GeoCoordinates(location.getLatitude(), location.getLongitude());
                if (mapView != null) {
                    try {

                        if (isMyMarkerLanded) {
                            mapView.getMapScene().removeMapMarker(myMarker);
                            isMyMarkerLanded = false;
                        }
                        mapView.getCamera().setTarget(myLocation);
                        myMarker.setCoordinates(myLocation);
                        mapView.getMapScene().addMapMarker(myMarker);
                        isMyMarkerLanded = true;
                    } catch (NullPointerException ex) {
                        Log.e("wtf", "wtf WTF");
                    }
                    if (currentRoute != null) {
                        ArrayList<Waypoint> waypoints = new ArrayList<>();
                        waypoints.add(new Waypoint(myLocation));
                        for (HerePoint h : currentRoute) {
                            waypoints.add(h.waypoint);
                        }
                        updateRoute(waypoints);
                    }

                }
            }

            @Override
            public void locationCancelled() {

            }
        });

        loadMapScene();
        if (easyLocation != null) {
            easyLocation.startLocation();
        }
    }


    private void loadMapScene() {

        mapView.getMapScene().loadScene(MapStyle.NORMAL_DAY, errorCode -> {
            if (errorCode == null) {
                mapView.getCamera().setTarget(myLocation);
                mapView.getCamera().setZoomLevel(14);
            } else {
                Log.d(TAG, "onLoadScene failed: " + errorCode.toString());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        easyLocation.endUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        easyLocation.startLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        easyLocation.endUpdates();
    }
}
