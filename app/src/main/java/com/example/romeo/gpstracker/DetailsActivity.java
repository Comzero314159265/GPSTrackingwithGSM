package com.example.romeo.gpstracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.romeo.gpstracker.utils.Location;
import com.example.romeo.gpstracker.utils.ToTimeStamp;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "DetailActivity";
    private GoogleMap mMap;
    private MapView mapView;
    private static final int LOCATIONREQUESTCODE = 999;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private FusedLocationProviderClient mFusedLocationClient;

    ListView listView;
    List<Location> locationList;
    MyAdapter myAdapter;
    LatLng last_position;

    Set<String> uidset;
    Set<String> nameset;
    String name;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        prefs = getSharedPreferences("locationPref", MODE_PRIVATE);
        editor = prefs.edit();

        Bundle bundle = getIntent().getExtras();
        String id = bundle.getString("uid");
        name = bundle.getString("name");
        setTitle(name);

        TextView textView = findViewById(R.id.txtTitle);
        textView.setText(name);

        mapView = (MapView) findViewById(R.id.mapView2);
        mapView.onCreate(savedInstanceState);

        mapView.onResume();

        try {
            MapsInitializer.initialize(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(this);

        fetchdata(id);

        listView = findViewById(R.id.listView);
        locationList = new ArrayList<>();
        myAdapter = new MyAdapter(DetailsActivity.this, locationList);
        listView.setAdapter(myAdapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Location location = locationList.get(position);
                if(location != null)
                    focus(location);



                return true;
            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<android.location.Location>() {
            @Override
            public void onSuccess(android.location.Location location) {
                if (location != null){
                    last_position = new LatLng(location.getLatitude(),location.getLongitude());
                }
            }
        });
    }

    private void fetchdata(String uid) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mLocationRef = database.getReference("items");

        DatabaseReference logRef = database.getReference("log");

        Log.i(TAG, "fetchdata: "+uid);

        mLocationRef.child(uid).getRef().child("locat").getRef().orderByKey().limitToLast(5).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                locationList.clear();
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                    Location location =  dataSnapshot1.getValue(Location.class);
                    location.setTimestamp(ToTimeStamp.parse(dataSnapshot1.getKey()));
                    Log.i(TAG, "onDataChangeindetail: "+location.getTimestamp());
                    locationList.add(location);
                }
                myAdapter.setList(locationList);
                listView.setAdapter(myAdapter);
                update();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(),"Internet เสถียร!!!",Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void focus(Location location){
        LatLng latLng = new LatLng(Double.parseDouble(location.getLat()+""),Double.parseDouble(location.getLng()+""));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(18).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void update(){
        boolean first = true;
        if(!locationList.isEmpty()){
            for (Location location : locationList){
                Marker marker;
                MarkerOptions markerOptions = new MarkerOptions();
                if(location.getTimestamp() != null && location.getLat() != 0 && location.getLng() != 0){
                    markerOptions.title(location.getTimestamp());
                    markerOptions.snippet("Position : "+location.getLat()+","+location.getLng());
                    LatLng latLng = new LatLng(Double.parseDouble(location.getLat()+""),Double.parseDouble(location.getLng()+""));
                    markerOptions.position(latLng);
                    if(first) {
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        first = false;
                    }else {
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }
                    marker = mMap.addMarker(markerOptions);
                    marker.showInfoWindow();


                    last_position = latLng;
                }
            }
        }

        CameraPosition cameraPosition = new CameraPosition.Builder().target(last_position).zoom(16).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(DetailsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)){

            }else {
                ActivityCompat.requestPermissions(DetailsActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},LOCATIONREQUESTCODE);
            }
        }
        mMap.setMyLocationEnabled(true);

//        LatLng sydney = new LatLng(13.9668672, 100.583236);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        CameraPosition cameraPosition = new CameraPosition.Builder().target(sydney).zoom(15).build();
//        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public class MyAdapter extends BaseAdapter {
        List<Location> locationList;
        Context mcontext;

        MyAdapter(Context mcontext,List<Location> locationList){
            this.mcontext = mcontext;
            this.locationList = locationList;
        }

        public void setList(List<Location> locationList){
            this.locationList = locationList;
        }

        @Override
        public int getCount() {
            return locationList.size();
//            return 10;
        }

        @Override
        public Object getItem(int i) {
            return locationList.get(i);
//            return 0;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View row = null;
            LayoutInflater inflater = (LayoutInflater) mcontext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.custom_details_rows, viewGroup,false);
            TextView time = row.findViewById(R.id.txtTime);
            TextView txtLat = row.findViewById(R.id.txtLat);
            TextView txtLng = row.findViewById(R.id.txtLng);

            time.setText(locationList.get(i).getTimestamp());
            txtLat.setText(locationList.get(i).getLat()+"");
            txtLng.setText(locationList.get(i).getLng()+"");
            return row;
        }
    }

}
