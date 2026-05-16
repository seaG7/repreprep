package com.mirea.Samsonova.mireaproject.ui.places;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.mirea.Samsonova.mireaproject.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class PlacesFragment extends Fragment {

    private static final int REQUEST_CODE_LOCATION = 801;
    private static final double RADIUS_METERS = 500.0;

    private MapView mapView;

    private TextView textViewStatus;
    private TextView textViewPlaceName;
    private TextView textViewPlaceAddress;
    private TextView textViewPlaceDescription;

    private Button buttonPlaceOne;
    private Button buttonPlaceTwo;
    private Button buttonPlaceThree;
    private Button buttonPlaceFour;
    private Button buttonShowAllPlaces;
    private Button buttonMyLocation;

    private MyLocationNewOverlay locationOverlay;
    private Polygon radiusOverlay;

    private final Place[] places = new Place[]{
            new Place(
                    "Кафе Пушкинъ",
                    "Тверской бульвар, 26А, Москва",
                    "Известное кафе-ресторан с историческим интерьером и русской кухней.",
                    new GeoPoint(55.765463, 37.604980)
            ),
            new Place(
                    "Столовая №57",
                    "Красная площадь, 3, ГУМ, Москва",
                    "Популярное заведение в ГУМе с атмосферой советской столовой.",
                    new GeoPoint(55.754675, 37.621611)
            ),
            new Place(
                    "Ресторан Белуга",
                    "Моховая улица, 15/1, стр. 1, Москва",
                    "Ресторан рядом с Кремлем, известный блюдами русской кухни.",
                    new GeoPoint(55.755820, 37.613730)
            ),
            new Place(
                    "Хлеб Насущный",
                    "Никольская улица, 8/1, стр. 1, Москва",
                    "Кафе-пекарня в центре Москвы, подходящая для завтрака и встречи.",
                    new GeoPoint(55.757190, 37.623118)
            )
    };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        Configuration.getInstance().load(
                requireContext().getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext())
        );

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View root = inflater.inflate(R.layout.fragment_places, container, false);

        mapView = root.findViewById(R.id.mapViewPlaces);

        textViewStatus = root.findViewById(R.id.textViewPlacesStatus);
        textViewPlaceName = root.findViewById(R.id.textViewPlaceName);
        textViewPlaceAddress = root.findViewById(R.id.textViewPlaceAddress);
        textViewPlaceDescription = root.findViewById(R.id.textViewPlaceDescription);

        buttonPlaceOne = root.findViewById(R.id.buttonPlaceOne);
        buttonPlaceTwo = root.findViewById(R.id.buttonPlaceTwo);
        buttonPlaceThree = root.findViewById(R.id.buttonPlaceThree);
        buttonPlaceFour = root.findViewById(R.id.buttonPlaceFour);
        buttonShowAllPlaces = root.findViewById(R.id.buttonShowAllPlaces);
        buttonMyLocation = root.findViewById(R.id.buttonMyLocation);

        setupMap();
        setupButtons();
        addMapTools();
        addPlaceMarkers();

        showAllPlaces();
        showPlace(places[0], false);

        return root;
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setZoomRounding(true);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(13.0);
        mapController.setCenter(new GeoPoint(55.755864, 37.617698));

        textViewStatus.setText("Карта загружена. Выберите заведение или нажмите на маркер.");
    }

    private void setupButtons() {
        buttonPlaceOne.setText(places[0].name);
        buttonPlaceTwo.setText(places[1].name);
        buttonPlaceThree.setText(places[2].name);
        buttonPlaceFour.setText(places[3].name);

        buttonPlaceOne.setOnClickListener(view -> showPlace(places[0], true));
        buttonPlaceTwo.setOnClickListener(view -> showPlace(places[1], true));
        buttonPlaceThree.setOnClickListener(view -> showPlace(places[2], true));
        buttonPlaceFour.setOnClickListener(view -> showPlace(places[3], true));

        buttonShowAllPlaces.setOnClickListener(view -> showAllPlaces());

        buttonMyLocation.setOnClickListener(view -> checkLocationPermissionAndShowUser());
    }

    private void addMapTools() {
        CompassOverlay compassOverlay = new CompassOverlay(
                requireContext().getApplicationContext(),
                new InternalCompassOrientationProvider(requireContext().getApplicationContext()),
                mapView
        );

        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(mapView);
        scaleBarOverlay.setCentred(true);
        scaleBarOverlay.setScaleBarOffset(displayMetrics.widthPixels / 2, 20);

        mapView.getOverlays().add(scaleBarOverlay);
    }

    private void addPlaceMarkers() {
        for (Place place : places) {
            addMarkerForPlace(place);
        }

        mapView.invalidate();
    }

    private void addMarkerForPlace(Place place) {
        Marker marker = new Marker(mapView);

        marker.setPosition(place.point);
        marker.setTitle(place.name);
        marker.setSubDescription(place.address + "\n" + place.description);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        marker.setOnMarkerClickListener((clickedMarker, clickedMapView) -> {
            showPlace(place, false);

            Toast.makeText(
                    requireContext(),
                    place.name + "\n" + place.address + "\n" + place.description,
                    Toast.LENGTH_LONG
            ).show();

            clickedMarker.showInfoWindow();

            return true;
        });

        mapView.getOverlays().add(marker);
    }

    private void showPlace(Place place, boolean moveCamera) {
        textViewPlaceName.setText(place.name);
        textViewPlaceAddress.setText("Адрес: " + place.address);
        textViewPlaceDescription.setText("Описание: " + place.description);

        textViewStatus.setText("Выбрано заведение: " + place.name + ". Построен радиус 500 м.");

        drawRadiusAroundPlace(place);

        if (moveCamera) {
            IMapController mapController = mapView.getController();
            mapController.setZoom(17.0);
            mapController.animateTo(place.point);
        }

        mapView.invalidate();
    }

    private void showAllPlaces() {
        IMapController mapController = mapView.getController();
        mapController.setZoom(14.0);
        mapController.animateTo(new GeoPoint(55.758000, 37.615500));

        textViewStatus.setText("Показаны все заведения на карте.");
    }

    private void drawRadiusAroundPlace(Place place) {
        if (radiusOverlay != null) {
            mapView.getOverlays().remove(radiusOverlay);
        }

        radiusOverlay = new Polygon(mapView);
        radiusOverlay.setPoints(createCirclePoints(place.point, RADIUS_METERS));
        radiusOverlay.setFillColor(Color.argb(60, 30, 136, 229));
        radiusOverlay.setStrokeColor(Color.rgb(30, 136, 229));
        radiusOverlay.setStrokeWidth(4.0f);

        mapView.getOverlays().add(radiusOverlay);
    }

    private List<GeoPoint> createCirclePoints(GeoPoint center, double radiusMeters) {
        List<GeoPoint> points = new ArrayList<>();

        double earthRadius = 6378137.0;
        double latitude = Math.toRadians(center.getLatitude());
        double longitude = Math.toRadians(center.getLongitude());

        for (int angle = 0; angle <= 360; angle += 10) {
            double bearing = Math.toRadians(angle);

            double pointLatitude = Math.asin(
                    Math.sin(latitude) * Math.cos(radiusMeters / earthRadius)
                            + Math.cos(latitude) * Math.sin(radiusMeters / earthRadius) * Math.cos(bearing)
            );

            double pointLongitude = longitude + Math.atan2(
                    Math.sin(bearing) * Math.sin(radiusMeters / earthRadius) * Math.cos(latitude),
                    Math.cos(radiusMeters / earthRadius) - Math.sin(latitude) * Math.sin(pointLatitude)
            );

            points.add(new GeoPoint(Math.toDegrees(pointLatitude), Math.toDegrees(pointLongitude)));
        }

        return points;
    }

    private void checkLocationPermissionAndShowUser() {
        int finePermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        );

        int coarsePermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
        );

        if (finePermission == PackageManager.PERMISSION_GRANTED
                || coarsePermission == PackageManager.PERMISSION_GRANTED) {
            showUserLocation();
        } else {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_CODE_LOCATION
            );
        }
    }

    private void showUserLocation() {
        if (locationOverlay == null) {
            locationOverlay = new MyLocationNewOverlay(
                    new GpsMyLocationProvider(requireContext().getApplicationContext()),
                    mapView
            );

            locationOverlay.enableMyLocation();
            locationOverlay.enableFollowLocation();

            mapView.getOverlays().add(locationOverlay);
        } else {
            locationOverlay.enableMyLocation();
            locationOverlay.enableFollowLocation();
        }

        locationOverlay.runOnFirstFix(() -> {
            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (locationOverlay.getMyLocation() != null) {
                    mapView.getController().setZoom(17.0);
                    mapView.getController().animateTo(locationOverlay.getMyLocation());
                    textViewStatus.setText("Отображено текущее местоположение пользователя.");
                } else {
                    textViewStatus.setText("Местоположение пока не найдено.");
                }

                mapView.invalidate();
            });
        });

        Toast.makeText(
                requireContext(),
                "Определение местоположения включено",
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onResume() {
        super.onResume();

        Configuration.getInstance().load(
                requireContext().getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext())
        );

        if (mapView != null) {
            mapView.onResume();
        }

        if (locationOverlay != null) {
            locationOverlay.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        if (locationOverlay != null) {
            locationOverlay.disableMyLocation();
        }

        if (mapView != null) {
            mapView.onPause();
        }

        Configuration.getInstance().save(
                requireContext().getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext())
        );

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (locationOverlay != null) {
            locationOverlay.disableMyLocation();
        }

        if (mapView != null) {
            mapView.onDetach();
        }

        mapView = null;
        locationOverlay = null;
        radiusOverlay = null;

        super.onDestroyView();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION) {
            boolean granted = false;

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                showUserLocation();
            } else {
                textViewStatus.setText("Разрешение на местоположение не выдано.");

                Toast.makeText(
                        requireContext(),
                        "Карта работает, но местоположение пользователя не отображается",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private static class Place {

        final String name;
        final String address;
        final String description;
        final GeoPoint point;

        Place(String name, String address, String description, GeoPoint point) {
            this.name = name;
            this.address = address;
            this.description = description;
            this.point = point;
        }
    }
}