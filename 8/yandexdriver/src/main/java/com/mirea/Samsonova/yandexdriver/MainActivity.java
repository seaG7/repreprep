package com.mirea.Samsonova.yandexdriver;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.network.NetworkError;
import com.yandex.runtime.network.RemoteError;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DrivingSession.DrivingRouteListener {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 200;

    private static final Point FALLBACK_START_LOCATION = new Point(55.751574, 37.573856);

    private static final Point ROUTE_END_LOCATION = new Point(55.761294, 37.609186);
    private static final String FAVORITE_PLACE_NAME = "Любимое заведение";
    private static final String FAVORITE_PLACE_INFO = "Здесь отображается краткая информация о заведении";

    private MapView mapView;
    private TextView textViewStatus;
    private TextView textViewRouteInfo;
    private Button buttonBuildRoute;

    private MapObjectCollection mapObjects;
    private DrivingRouter drivingRouter;
    private DrivingSession drivingSession;

    private Point routeStartLocation = FALLBACK_START_LOCATION;

    private final int[] colors = {
            0xFFFF0000,
            0xFF00AA00,
            0xFF0000FF,
            0xFFFF8800
    };

    private final MapObjectTapListener favoritePlaceTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(@NonNull MapObject mapObject, @NonNull Point point) {
            Toast.makeText(
                    MainActivity.this,
                    FAVORITE_PLACE_NAME + "\n" + FAVORITE_PLACE_INFO,
                    Toast.LENGTH_LONG
            ).show();

            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapKitFactory.initialize(this);
        DirectionsFactory.initialize(this);

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapview);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewRouteInfo = findViewById(R.id.textViewRouteInfo);
        buttonBuildRoute = findViewById(R.id.buttonBuildRoute);

        mapView.getMap().setRotateGesturesEnabled(false);

        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter();
        mapObjects = mapView.getMap().getMapObjects().addCollection();

        buttonBuildRoute.setOnClickListener(view -> checkPermissionAndBuildRoute());

        checkPermissionAndBuildRoute();
    }

    private void checkPermissionAndBuildRoute() {
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
            prepareRouteFromUserLocation();
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

    private void prepareRouteFromUserLocation() {
        Location location = getLastKnownLocation();

        if (location != null) {
            routeStartLocation = new Point(location.getLatitude(), location.getLongitude());
            textViewStatus.setText("Маршрут строится от текущего местоположения");
        } else {
            routeStartLocation = FALLBACK_START_LOCATION;
            textViewStatus.setText("Местоположение не найдено, используется точка по умолчанию");
        }

        moveCameraToRoute();
        submitRequest();
    }

    private Location getLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            return null;
        }

        Location gpsLocation = null;
        Location networkLocation = null;

        try {
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException e) {
            return null;
        }

        if (gpsLocation != null) {
            return gpsLocation;
        }

        return networkLocation;
    }

    private void moveCameraToRoute() {
        Point screenCenter = new Point(
                (routeStartLocation.getLatitude() + ROUTE_END_LOCATION.getLatitude()) / 2,
                (routeStartLocation.getLongitude() + ROUTE_END_LOCATION.getLongitude()) / 2
        );

        mapView.getMap().move(
                new CameraPosition(screenCenter, 11.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 1.0f),
                null
        );
    }

    private void submitRequest() {
        textViewRouteInfo.setText("Выполняется запрос маршрута...");

        mapObjects.clear();

        addStartMarker();
        addFavoritePlaceMarker();

        DrivingOptions drivingOptions = new DrivingOptions();
        VehicleOptions vehicleOptions = new VehicleOptions();

        drivingOptions.setRoutesCount(4);

        ArrayList<RequestPoint> requestPoints = new ArrayList<>();

        requestPoints.add(
                new RequestPoint(
                        routeStartLocation,
                        RequestPointType.WAYPOINT,
                        null
                )
        );

        requestPoints.add(
                new RequestPoint(
                        ROUTE_END_LOCATION,
                        RequestPointType.WAYPOINT,
                        null
                )
        );

        drivingSession = drivingRouter.requestRoutes(
                requestPoints,
                drivingOptions,
                vehicleOptions,
                this
        );
    }

    private void addStartMarker() {
        PlacemarkMapObject marker = mapObjects.addPlacemark(
                routeStartLocation,
                ImageProvider.fromResource(this, android.R.drawable.ic_menu_mylocation)
        );

        marker.setText("Старт");
    }

    private void addFavoritePlaceMarker() {
        PlacemarkMapObject marker = mapObjects.addPlacemark(
                ROUTE_END_LOCATION,
                ImageProvider.fromResource(this, android.R.drawable.star_big_on)
        );

        marker.setText(FAVORITE_PLACE_NAME);
        marker.addTapListener(favoritePlaceTapListener);
    }

    @Override
    public void onDrivingRoutes(@NonNull List<DrivingRoute> routes) {
        if (routes.isEmpty()) {
            textViewRouteInfo.setText("Маршруты не найдены");
            return;
        }

        for (int i = 0; i < routes.size(); i++) {
            int color = colors[i % colors.length];

            mapObjects.addPolyline(routes.get(i).getGeometry()).setStrokeColor(color);
        }

        textViewRouteInfo.setText("Построено маршрутов: " + routes.size());
    }

    @Override
    public void onDrivingRoutesError(@NonNull Error error) {
        String errorMessage = getString(R.string.unknown_error_message);

        if (error instanceof RemoteError) {
            errorMessage = getString(R.string.remote_error_message);
        } else if (error instanceof NetworkError) {
            errorMessage = getString(R.string.network_error_message);
        }

        textViewRouteInfo.setText(errorMessage);

        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mapView != null) {
            mapView.onStart();
        }

        MapKitFactory.getInstance().onStart();
    }

    @Override
    protected void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }

        MapKitFactory.getInstance().onStop();

        super.onStop();
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
                prepareRouteFromUserLocation();
            } else {
                textViewStatus.setText("Разрешение на местоположение не выдано");
                Toast.makeText(
                        this,
                        "Маршрут будет построен от точки по умолчанию",
                        Toast.LENGTH_SHORT
                ).show();

                routeStartLocation = FALLBACK_START_LOCATION;
                moveCameraToRoute();
                submitRequest();
            }
        }
    }
}