package com.mirea.Samsonova.lesson8;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKit;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.ObjectEvent;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CompositeIcon;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.mapkit.user_location.UserLocationObjectListener;
import com.yandex.mapkit.user_location.UserLocationView;
import com.yandex.runtime.image.ImageProvider;

public class MainActivity extends AppCompatActivity implements UserLocationObjectListener {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 100;
    private static final Point MOSCOW_CENTER = new Point(55.751574, 37.573856);

    private MapView mapView;
    private TextView textViewStatus;
    private Button buttonMyLocation;

    private UserLocationLayer userLocationLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapKitFactory.initialize(this);

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapview);
        textViewStatus = findViewById(R.id.textViewStatus);
        buttonMyLocation = findViewById(R.id.buttonMyLocation);

        moveCameraToMoscow();

        buttonMyLocation.setOnClickListener(view -> checkLocationPermissionAndLoadLayer());

        checkLocationPermissionAndLoadLayer();
    }

    private void moveCameraToMoscow() {
        mapView.getMap().move(
                new CameraPosition(MOSCOW_CENTER, 11.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 1.0f),
                null
        );
    }

    private void checkLocationPermissionAndLoadLayer() {
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
            loadUserLocationLayer();
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

    private void loadUserLocationLayer() {
        MapKit mapKit = MapKitFactory.getInstance();

        mapKit.resetLocationManagerToDefault();

        userLocationLayer = mapKit.createUserLocationLayer(mapView.getMapWindow());
        userLocationLayer.setVisible(true);
        userLocationLayer.setHeadingEnabled(true);
        userLocationLayer.setObjectListener(this);

        textViewStatus.setText("Слой местоположения включен");
    }

    @Override
    public void onObjectAdded(@NonNull UserLocationView userLocationView) {
        userLocationLayer.setAnchor(
                new PointF((float) (mapView.getWidth() * 0.5), (float) (mapView.getHeight() * 0.5)),
                new PointF((float) (mapView.getWidth() * 0.5), (float) (mapView.getHeight() * 0.83))
        );

        userLocationView.getArrow().setIcon(
                ImageProvider.fromResource(this, android.R.drawable.arrow_up_float)
        );

        CompositeIcon pinIcon = userLocationView.getPin().useCompositeIcon();

        pinIcon.setIcon(
                "pin",
                ImageProvider.fromResource(this, android.R.drawable.ic_menu_mylocation),
                new IconStyle()
                        .setAnchor(new PointF(0.5f, 0.5f))
                        .setRotationType(RotationType.ROTATE)
                        .setZIndex(1f)
                        .setScale(0.8f)
        );

        userLocationView.getAccuracyCircle().setFillColor(Color.BLUE & 0x99ffffff);

        textViewStatus.setText("Местоположение пользователя найдено");
    }

    @Override
    public void onObjectRemoved(@NonNull UserLocationView userLocationView) {
        textViewStatus.setText("Объект местоположения удален");
    }

    @Override
    public void onObjectUpdated(
            @NonNull UserLocationView userLocationView,
            @NonNull ObjectEvent objectEvent
    ) {
        textViewStatus.setText("Местоположение обновлено");
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
                loadUserLocationLayer();
            } else {
                textViewStatus.setText("Разрешение на местоположение не выдано");
                Toast.makeText(
                        this,
                        "Для отображения местоположения нужно разрешение",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}