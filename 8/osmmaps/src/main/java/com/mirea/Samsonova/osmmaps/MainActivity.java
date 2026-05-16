package com.mirea.Samsonova.osmmaps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 300;

    private static final GeoPoint START_POINT = new GeoPoint(55.794229, 37.700772);
    private static final String MARKER_TITLE = "Точка на карте";
    private static final String MARKER_DESCRIPTION = "Маркер OpenStreetMap";

    private MapView mapView;
    private TextView textViewStatus;

    private MyLocationNewOverlay locationNewOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );

        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        textViewStatus = findViewById(R.id.textViewStatus);

        setupMap();
        checkPermissionAndAddLocationOverlay();
        addCompass();
        addScaleBar();
        addMarker();

        textViewStatus.setText("Карта OpenStreetMap загружена");
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setZoomRounding(true);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);
        mapController.setCenter(START_POINT);
    }

    private void checkPermissionAndAddLocationOverlay() {
        int fineLocationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        );

        int coarseLocationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        );

        if (fineLocationPermission == PackageManager.PERMISSION_GRANTED
                || coarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            addLocationOverlay();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_CODE_LOCATION_PERMISSION
            );
        }
    }

    private void addLocationOverlay() {
        locationNewOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(getApplicationContext()),
                mapView
        );

        locationNewOverlay.enableMyLocation();
        locationNewOverlay.enableFollowLocation();

        mapView.getOverlays().add(locationNewOverlay);
        mapView.invalidate();

        textViewStatus.setText("Слой местоположения включен");
    }

    private void addCompass() {
        CompassOverlay compassOverlay = new CompassOverlay(
                getApplicationContext(),
                new InternalCompassOrientationProvider(getApplicationContext()),
                mapView
        );

        compassOverlay.enableCompass();

        mapView.getOverlays().add(compassOverlay);
    }

    private void addScaleBar() {
        Context context = getApplicationContext();
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(mapView);
        scaleBarOverlay.setCentred(true);
        scaleBarOverlay.setScaleBarOffset(displayMetrics.widthPixels / 2, 10);

        mapView.getOverlays().add(scaleBarOverlay);
    }

    private void addMarker() {
        Marker marker = new Marker(mapView);

        marker.setPosition(START_POINT);
        marker.setTitle(MARKER_TITLE);
        marker.setSubDescription(MARKER_DESCRIPTION);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        marker.setIcon(
                ResourcesCompat.getDrawable(
                        getResources(),
                        org.osmdroid.library.R.drawable.osm_ic_follow_me_on,
                        null
                )
        );

        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                Toast.makeText(
                        getApplicationContext(),
                        marker.getTitle() + "\n" + MARKER_DESCRIPTION,
                        Toast.LENGTH_SHORT
                ).show();

                marker.showInfoWindow();

                return true;
            }
        });

        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }

        if (locationNewOverlay != null) {
            locationNewOverlay.enableMyLocation();
        }
    }

    @Override
    protected void onPause() {
        if (locationNewOverlay != null) {
            locationNewOverlay.disableMyLocation();
        }

        if (mapView != null) {
            mapView.onPause();
        }

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            boolean granted = false;

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                addLocationOverlay();
            } else {
                textViewStatus.setText("Разрешение на местоположение не выдано");

                Toast.makeText(
                        this,
                        "Карта работает, но местоположение пользователя не отображается",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}