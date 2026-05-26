# Практическая работа №8. Картографические сервисы в Android

**Студент:** Самсонова Ольга Павловна  
**Группа:** БСБО-09-23  
**Дисциплина:** Разработка мобильных приложений

---

## 1. Цель работы

Изучить принципы интеграции картографических сервисов в Android-приложения. Освоить работу с двумя картографическими SDK: **Яндекс MapKit** (коммерческий SDK для Яндекс.Карт) и **osmdroid** (open-source библиотека для OpenStreetMap). Реализовать отображение карты, местоположения пользователя, маркеров, наложений (компас, масштабная линейка) и построение маршрутов через Яндекс Routing API.

---

## 2. Архитектура проекта

Практическая работа состоит из трёх самостоятельных модулей и контрольного задания в рамках проекта **MireaProject**:

| Модуль | Картографический SDK | Описание |
|---|---|---|
| `yandexmaps` | Яндекс MapKit 4.x | Базовое отображение карты, слой местоположения пользователя |
| `yandexdriver` | Яндекс MapKit + DirectionsFactory | Построение автомобильных маршрутов (до 4 вариантов) |
| `osmmaps` | osmdroid 6.1.x | OpenStreetMap: компас, масштаб, местоположение, маркер |
| `MireaProject` | osmdroid | Фрагмент «Места» с 4 ресторанами Москвы и радиусом 500 м |

---

## 3. Ход работы

### 3.1 Модуль YandexMaps — карта Яндекс и местоположение пользователя

#### Описание

Модуль демонстрирует базовую интеграцию **Яндекс MapKit SDK**. При запуске карта центрируется на Москве с помощью плавной анимации (`Animation.Type.SMOOTH`). По нажатию кнопки или автоматически при наличии разрешения активируется **`UserLocationLayer`** — встроенный слой SDK для отображения текущего местоположения пользователя с кружком точности и иконкой направления.

Приложение реализует интерфейс `UserLocationObjectListener`, который уведомляет об изменениях объекта местоположения: добавлении (`onObjectAdded`), удалении (`onObjectRemoved`) и обновлении (`onObjectUpdated`). В методе `onObjectAdded` настраивается внешний вид: якорь слоя, иконка стрелки, пин и кружок точности.

Жизненный цикл MapKit корректно связан с жизненным циклом активности: `MapKitFactory.getInstance().onStart()` / `onStop()`.

![Экран YandexMaps — карта Москвы](report-images/yandexmaps_main.png)

#### Ключевые фрагменты кода

Инициализация Яндекс MapKit выполняется **до** вызова `setContentView`:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    MapKitFactory.initialize(this);

    setContentView(R.layout.activity_main);
    // ...
}
```

Перемещение камеры на центр Москвы с анимацией:

```java
private static final Point MOSCOW_CENTER = new Point(55.751574, 37.573856);

private void moveCameraToMoscow() {
    mapView.getMap().move(
            new CameraPosition(MOSCOW_CENTER, 11.0f, 0.0f, 0.0f),
            new Animation(Animation.Type.SMOOTH, 1.0f),
            null
    );
}
```

Загрузка слоя местоположения пользователя с настройкой отображения:

```java
private void loadUserLocationLayer() {
    MapKit mapKit = MapKitFactory.getInstance();

    mapKit.resetLocationManagerToDefault();

    userLocationLayer = mapKit.createUserLocationLayer(mapView.getMapWindow());
    userLocationLayer.setVisible(true);
    userLocationLayer.setHeadingEnabled(true);
    userLocationLayer.setObjectListener(this);

    textViewStatus.setText("Слой местоположения включен");
}
```

Настройка внешнего вида при обнаружении местоположения:

```java
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
```

Запрос разрешений на местоположение во время выполнения:

```java
private void checkLocationPermissionAndLoadLayer() {
    int fineLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION);
    int coarseLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION);

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
```

Связка с жизненным циклом активности:

```java
@Override
protected void onStart() {
    super.onStart();
    if (mapView != null) mapView.onStart();
    MapKitFactory.getInstance().onStart();
}

@Override
protected void onStop() {
    if (mapView != null) mapView.onStop();
    MapKitFactory.getInstance().onStop();
    super.onStop();
}
```

Зависимость в `build.gradle`:

```groovy
implementation 'com.yandex.android:maps.mobile:4.3.1-full'
```

API-ключ в `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.yandex.android.mapkit.API_KEY"
    android:value="YOUR_YANDEX_MAPKIT_API_KEY" />
```

![YandexMaps — местоположение пользователя](report-images/yandexmaps_location.png)

---

### 3.2 Модуль YandexDriver — построение маршрутов

#### Описание

Модуль демонстрирует построение автомобильных маршрутов с помощью **Яндекс Directions API** (модуль `DirectionsFactory`). Приложение строит маршруты от текущего местоположения пользователя (или от резервной точки — центра Москвы) до заданной целевой точки. Запрашивается до 4 вариантов маршрута (`drivingOptions.setRoutesCount(4)`), каждый отображается полилинией своего цвета.

На карте размещается маркер стартовой точки и маркер конечной точки («Любимое заведение») с обработчиком нажатия, выводящим Toast с описанием места.

Для получения последнего известного местоположения используется `LocationManager` с двумя провайдерами: `GPS_PROVIDER` имеет приоритет над `NETWORK_PROVIDER`.

![Экран YandexDriver — маршруты](report-images/yandexdriver_main.png)

#### Ключевые фрагменты кода

Инициализация фабрик MapKit и Directions:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    MapKitFactory.initialize(this);
    DirectionsFactory.initialize(this);

    setContentView(R.layout.activity_main);
    // ...
    drivingRouter = DirectionsFactory.getInstance().createDrivingRouter();
    mapObjects = mapView.getMap().getMapObjects().addCollection();
}
```

Константы маршрута и цвета полилиний:

```java
private static final Point FALLBACK_START_LOCATION = new Point(55.751574, 37.573856);
private static final Point ROUTE_END_LOCATION = new Point(55.761294, 37.609186);

private final int[] colors = {
        0xFFFF0000,
        0xFF00AA00,
        0xFF0000FF,
        0xFFFF8800
};
```

Получение последнего известного местоположения через `LocationManager`:

```java
private Location getLastKnownLocation() {
    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    if (locationManager == null) return null;

    Location gpsLocation = null;
    Location networkLocation = null;

    try {
        gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    } catch (SecurityException e) {
        return null;
    }

    if (gpsLocation != null) return gpsLocation;

    return networkLocation;
}
```

Центрирование камеры между стартовой и конечной точками:

```java
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
```

Формирование запроса маршрутов:

```java
private void submitRequest() {
    textViewRouteInfo.setText("Выполняется запрос маршрута...");
    mapObjects.clear();

    addStartMarker();
    addFavoritePlaceMarker();

    DrivingOptions drivingOptions = new DrivingOptions();
    VehicleOptions vehicleOptions = new VehicleOptions();

    drivingOptions.setRoutesCount(4);

    ArrayList<RequestPoint> requestPoints = new ArrayList<>();

    requestPoints.add(new RequestPoint(
            routeStartLocation, RequestPointType.WAYPOINT, null));

    requestPoints.add(new RequestPoint(
            ROUTE_END_LOCATION, RequestPointType.WAYPOINT, null));

    drivingSession = drivingRouter.requestRoutes(
            requestPoints, drivingOptions, vehicleOptions, this);
}
```

Обработка полученных маршрутов — каждый рисуется полилинией своего цвета:

```java
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
```

Маркер конечной точки с обработчиком нажатия:

```java
private void addFavoritePlaceMarker() {
    PlacemarkMapObject marker = mapObjects.addPlacemark(
            ROUTE_END_LOCATION,
            ImageProvider.fromResource(this, android.R.drawable.star_big_on)
    );

    marker.setText(FAVORITE_PLACE_NAME);
    marker.addTapListener(favoritePlaceTapListener);
}

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
```

![YandexDriver — построенные маршруты](report-images/yandexdriver_routes.png)

---

### 3.3 Модуль OSMMaps — OpenStreetMap

#### Описание

Модуль демонстрирует работу с **osmdroid** — open-source Android-библиотекой для отображения тайловых карт OpenStreetMap. Реализованы:

- Настройка `MapView` с источником тайлов `MAPNIK`, мультисенсорным управлением и управлением масштабом
- **`MyLocationNewOverlay`** — наложение с текущим местоположением пользователя и следованием камеры
- **`CompassOverlay`** — компас в углу карты
- **`ScaleBarOverlay`** — масштабная линейка, центрированная по горизонтали
- **`Marker`** — маркер с заголовком, описанием и кастомной иконкой, с обработчиком нажатия

Конфигурация osmdroid (`Configuration.getInstance()`) инициализируется в `onCreate()` с `UserAgent`, равным имени пакета — это обязательное требование для корректной загрузки тайлов.

![Экран OSMMaps — карта OpenStreetMap](report-images/osmmaps_main.png)

#### Ключевые фрагменты кода

Инициализация конфигурации osmdroid:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Configuration.getInstance().load(
            getApplicationContext(),
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
    );

    Configuration.getInstance().setUserAgentValue(getPackageName());

    setContentView(R.layout.activity_main);
    // ...
}
```

Настройка карты:

```java
private static final GeoPoint START_POINT = new GeoPoint(55.794229, 37.700772);

private void setupMap() {
    mapView.setTileSource(TileSourceFactory.MAPNIK);
    mapView.setBuiltInZoomControls(true);
    mapView.setZoomRounding(true);
    mapView.setMultiTouchControls(true);

    IMapController mapController = mapView.getController();
    mapController.setZoom(15.0);
    mapController.setCenter(START_POINT);
}
```

Наложение местоположения пользователя:

```java
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
```

Добавление компаса:

```java
private void addCompass() {
    CompassOverlay compassOverlay = new CompassOverlay(
            getApplicationContext(),
            new InternalCompassOrientationProvider(getApplicationContext()),
            mapView
    );

    compassOverlay.enableCompass();
    mapView.getOverlays().add(compassOverlay);
}
```

Добавление масштабной линейки с центрированием:

```java
private void addScaleBar() {
    Context context = getApplicationContext();
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

    ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(mapView);
    scaleBarOverlay.setCentred(true);
    scaleBarOverlay.setScaleBarOffset(displayMetrics.widthPixels / 2, 10);

    mapView.getOverlays().add(scaleBarOverlay);
}
```

Добавление маркера с кастомной иконкой из библиотеки osmdroid и обработчиком нажатия:

```java
private static final String MARKER_TITLE = "Точка на карте";
private static final String MARKER_DESCRIPTION = "Маркер OpenStreetMap";

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
```

Корректная обработка жизненного цикла карты:

```java
@Override
protected void onResume() {
    super.onResume();
    if (mapView != null) mapView.onResume();
    if (locationNewOverlay != null) locationNewOverlay.enableMyLocation();
}

@Override
protected void onPause() {
    if (locationNewOverlay != null) locationNewOverlay.disableMyLocation();
    if (mapView != null) mapView.onPause();
    super.onPause();
}
```

Зависимость в `build.gradle`:

```groovy
implementation 'org.osmdroid:osmdroid-android:6.1.16'
```

Разрешения в `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

![OSMMaps — маркер и компас](report-images/osmmaps_marker.png)

---

### 3.4 Контрольное задание — MireaProject

#### Описание

В рамках контрольного задания в проект **MireaProject** добавлен **`PlacesFragment`** — фрагмент навигационного меню, реализующий карту OpenStreetMap с 4 московскими заведениями в качестве маркеров.

#### Функциональность PlacesFragment

- Карта osmdroid с источником тайлов `MAPNIK`, масштаб 13, центр Москвы (55.755864, 37.617698)
- 4 маркера заведений: Кафе Пушкинъ, Столовая №57, Ресторан Белуга, Хлеб Насущный
- При нажатии на маркер или кнопку заведения показывается информация и строится полигон-круг радиусом **500 м** (синяя заливка с полупрозрачностью `Color.argb(60, 30, 136, 229)`)
- Кнопка «Все места» центрирует карту на масштабе 14
- Кнопка «Моё местоположение» запрашивает разрешение и включает `MyLocationNewOverlay` с анимированным переходом к найденному местоположению
- Компас и масштабная линейка добавлены через наложения osmdroid
- Состояние выбранного заведения (имя, адрес, описание) отображается в блоке `TextView` под картой

Расчёт точек круга реализован по формуле сферической тригонометрии с радиусом Земли 6 378 137 м, шаг угла — 10°.

![PlacesFragment — карта с заведениями](report-images/mirea_places_map.png)

#### Ключевые фрагменты кода

Данные о заведениях объявлены массивом внутренних объектов `Place`:

```java
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
```

Добавление маркера для каждого заведения с обработчиком нажатия:

```java
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
```

Отображение заведения: показ информации и перемещение камеры при необходимости:

```java
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
```

Рисование круга радиусом 500 м как полигона osmdroid:

```java
private static final double RADIUS_METERS = 500.0;

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
```

Включение местоположения пользователя с ожиданием первого фикса:

```java
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
        if (!isAdded()) return;

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
}
```

![PlacesFragment — радиус 500 м вокруг заведения](report-images/mirea_places_radius.png)

---

## 4. Результаты работы

В ходе практической работы реализованы и изучены следующие картографические возможности Android:

| Модуль | SDK / Библиотека | Реализованные функции |
|---|---|---|
| YandexMaps | Яндекс MapKit 4.x | Базовая карта, плавная анимация камеры, `UserLocationLayer`, настройка иконок местоположения |
| YandexDriver | Яндекс MapKit + Directions | Получение текущего местоположения, построение до 4 маршрутов, цветные полилинии, маркеры |
| OSMMaps | osmdroid 6.1.x | OpenStreetMap, компас, масштабная линейка, `MyLocationNewOverlay`, маркер с обработчиком |
| MireaProject PlacesFragment | osmdroid 6.1.x | 4 маркера московских заведений, полигон-круг 500 м, кнопки навигации, местоположение пользователя |

Изучены два принципиально разных подхода к картографии в Android: коммерческий SDK Яндекс MapKit с богатым API маршрутизации и бесплатная open-source библиотека osmdroid на базе OpenStreetMap. Оба SDK требуют корректной обработки разрешений на местоположение во время выполнения (runtime permissions) и привязки к жизненному циклу Activity/Fragment. Реализован алгоритм построения окружности через сферическую тригонометрию для отображения зоны покрытия вокруг выбранного места.
