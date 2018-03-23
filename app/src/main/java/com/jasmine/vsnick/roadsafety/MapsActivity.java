package com.jasmine.vsnick.roadsafety;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity
        implements
        OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback{

    private GoogleMap mMap;
    private boolean mPermissionDenied = false;

    private static final int RADIUS_IN_METERS = 100;
    private static final int CIRCLE_STROKE_COLOR = 0x5500ff00;
    private static final int CIRCLE_FILL_COLOR = 0x55ff0000;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        readCoordinates();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        for (LatLng i : coordinates) {
            mMap.addCircle(new CircleOptions()
                    .center(i)
                    .radius(RADIUS_IN_METERS)
                    .fillColor(CIRCLE_FILL_COLOR)
                    .strokeColor(CIRCLE_STROKE_COLOR));
        }
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();
    }
    private List<LatLng> coordinates = new ArrayList<>();
    private void readCoordinates(){
        InputStream in = getResources().openRawResource(R.raw.result);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));

        String line;

        try {
            reader.readLine();
            while( (line = reader.readLine())!=null){
                String[] tokens = line.split(",");
                LatLng coord = new LatLng(Double.parseDouble(tokens[1]),Double.parseDouble(tokens[2]));
                coordinates.add(coord);
                Log.d("MyActivity","Created:"+coord);
            }
        } catch (IOException e) {
            Log.wtf("MyActivity","Error",e);
            e.printStackTrace();
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
            Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the network location provider.
                    //showWarning(location);
                    Log.d("MyActivity","Changed");
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            //Location l = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //mMap.addMarker(new MarkerOptions().position(new LatLng(l.getLatitude(),l.getLongitude())).title("Marker in Sydney"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(17.38,78.48),10));
        }
    }

    private void showWarning(Location location) {
        Double x = location.getLatitude();
        Double y = location.getLongitude();


        for(LatLng i: coordinates){
            Double dlat = x  - i.latitude;
            Double dlon = y - i.longitude;

            Double a = Math.pow(Math.sin(dlat/2),2) + Math.cos(x) * Math.cos(y) * Math.pow((Math.sin(dlon/2)),2);
            Double c = 2 * Math.atan2( Math.sqrt(a), Math.sqrt(1-a) );
            Double d = 6371 * c;
            //Log.d("MyActivity",d.toString());
            if((d*d) <= (RADIUS_IN_METERS*RADIUS_IN_METERS)){
                Toast.makeText(this, "Warning", Toast.LENGTH_LONG).show();
            }
        }
    }
}
