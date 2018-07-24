package com.example.romeo.gpstracker;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.romeo.gpstracker.utils.Location;
import com.example.romeo.gpstracker.utils.ToTimeStamp;
import com.example.romeo.gpstracker.utils.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import static android.support.constraint.Constraints.TAG;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private GoogleMap mMap;
    private static final int LOCATIONREQUESTCODE = 999;
    MapView mapView;

    MainActivity mainActivity;
    List<Location> locationList;

    SharedPreferences pref;
    SharedPreferences.Editor editor;


    Set<String> uidset;
    Set<String> nameset;
    Iterator<String> uidI;
    Iterator<String> nameI;

    LatLng mainLocation;

    List<String> namelist;
    List<String> idliist;

    FirebaseDatabase database;
    DatabaseReference databaseReference;


    public MapFragment() {
    }

    void setMain(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MapFragment newInstance(String param1, String param2) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("items");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_map, container, false);
        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        pref = getActivity().getSharedPreferences("locationPref", Context.MODE_PRIVATE);
        editor = pref.edit();

        uidset = pref.getStringSet("uid",new HashSet<String>());
        nameset = pref.getStringSet("name",new HashSet<String>());

        mapView.onResume();
        locationList = new ArrayList<>();
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(this);
        return view;
    }


    private void draw(){
        mMap.clear();
        for(Location location : locationList){
            Log.i(TAG, "draw: "+(location.getName() != null));
            MarkerOptions markerOptions = new MarkerOptions();
            LatLng latLng = new LatLng(Double.parseDouble(location.getLat()+""), Double.parseDouble(location.getLng()+""));
            markerOptions.position(latLng);
            markerOptions.title(location.getName());
            markerOptions.snippet("Timestamp : "+location.getTimestamp());
            mMap.addMarker(markerOptions);
        }
        Gson gson = new Gson();
        String temp = pref.getString("mLocationmap","");
        List<Location> locationArrayList = new ArrayList<>();
        Type type = new TypeToken<ArrayList<Location>>(){}.getType();
        if(!temp.equals("")){
            locationArrayList = (ArrayList<Location>) gson.fromJson(temp,type);
        }

        for (int i = 0; i < locationArrayList.size(); i++) {
            Log.i(TAG, "draw: "+i+" : "+locationArrayList.get(i).getName()+" lat "+locationArrayList.get(i).getLat()+" lng "+locationArrayList.get(i).getLng());
        }

            for (Location a:locationArrayList) {
                MarkerOptions markerOptions = new MarkerOptions();
                LatLng latLng = new LatLng(a.getLat(), a.getLng());
                markerOptions.position(latLng);
                markerOptions.title(a.getName());
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                mMap.addMarker(markerOptions).setDraggable(true);

                String raduis = pref.getString("radius","250");

                // Circle
                mMap.addCircle(new CircleOptions()
                        .center(latLng).radius(Double.parseDouble(raduis))
                        .fillColor(Color.argb(100, 255, 0, 0))
                        .strokeWidth(0)
                        .visible(true)
                );
            }

                mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                    @Override
                    public void onMarkerDragStart(Marker marker) {

                    }

                    @Override
                    public void onMarkerDrag(Marker marker) {

                    }

                    @Override
                    public void onMarkerDragEnd(Marker marker) {
                        // update pin
                        mainLocation = marker.getPosition();
                        Gson gson = new Gson();
                        String temp = pref.getString("mLocationmap","");
                        List<Location> locationArrayList1 = new ArrayList<>();
                        Type type = new TypeToken<ArrayList<Location>>(){}.getType();
                        if(!temp.equals("")){
                            locationArrayList1 = (ArrayList<Location>) gson.fromJson(temp,type);
                        }
                        Location tempc = new Location(marker.getTitle());
                        if(locationArrayList1.contains(tempc))
                            tempc = locationArrayList1.get(locationArrayList1.indexOf(tempc));
                        tempc.setLat(mainLocation.latitude);
                        tempc.setLng(mainLocation.longitude);
                        editor.putString("mLocationmap",gson.toJson(locationArrayList1));
                        editor.putString("mainlocation",gson.toJson(mainLocation));
                        editor.apply();
                        update();


                    }
                });

    }

    public void update(){
        mMap.clear();
        locationList.clear();

        final Gson gson = new Gson();
        ArrayList<Location> locatemp = new ArrayList<>();
        String temp = pref.getString("locations","");
        Type type = new TypeToken<ArrayList<Location>>(){}.getType();
        if(!temp.equals(""))
            locatemp = (ArrayList<Location>) gson.fromJson(temp,type);
        final ArrayList<Location> arrayList = locatemp;
        for (Location a:locatemp) {
            databaseReference.child(a.getKey()).getRef().child("locat").getRef().orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                        Location location = dataSnapshot1.getValue(Location.class);
                        location.setKey(dataSnapshot.getRef().getParent().getKey());
                        String temp = dataSnapshot1.getKey();
                        Log.d(TAG, "timestamp : "+temp);
                        location.setTimestamp(ToTimeStamp.parse(temp));
                        Location search = new Location();
                        int index = arrayList.indexOf(location);
                        if(index > -1) {
                            location.setName(arrayList.get(index).getName());
                        }
                        if (locationList.contains(location))
                            locationList.remove(location);
                        locationList.add(location);
                    }
                    draw();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        mainActivity.fetchdata();
        draw();
    }


    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)){

            }else {
                ActivityCompat.requestPermissions(getActivity(),new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATIONREQUESTCODE);
            }
        }else {
            mMap.setMyLocationEnabled(true);
            Type latlngtype = new TypeToken<LatLng>(){}.getType();
            String maintemp = pref.getString("mainlocation","");
            Gson gson = new Gson();
            if(!maintemp.equals("")){
                mainLocation = gson.fromJson(maintemp,latlngtype);
            }

            if(mainLocation != null){
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mainLocation, 15));
            }

            update();
        }

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker2) {
                final Marker marker = marker2;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("ลบ");
                builder.setMessage("ต้องการลบ "+marker.getTitle()+" ?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final Gson gson = new Gson();
                        ArrayList<Location> arrayList = new ArrayList<>();
                        String temp = pref.getString("mLocationmap","");
                        Type type = new TypeToken<ArrayList<Location>>(){}.getType();
                        HashMap<String,Location> hashMap = new HashMap<>();
                        String temp2 = pref.getString("maplocation","");
                        Type type2 = new TypeToken<HashMap<String,Location>>(){}.getType();

                        ArrayList<Location> arrayList1 = new ArrayList<>();
                        String templcoation = pref.getString("locations","");
                        if(!templcoation.equals("")){
                            arrayList1 = gson.fromJson(templcoation,type);
                        }

                        if(!temp.equals("")){
                            arrayList = (ArrayList<Location>)gson.fromJson(temp,type);
                            Log.i(TAG, "onInfoWindowClick:list before "+arrayList.size());
                            arrayList.remove(new Location(marker.getTitle()));
                            Log.i(TAG, "onInfoWindowClick:list after "+arrayList.size());
                            Log.i(TAG, "contain: "+arrayList1.size());
                        }

                        if(!temp2.equals("")){
                            hashMap = (HashMap<String,Location>)gson.fromJson(temp2,type2);
                            Iterator it = hashMap.entrySet().iterator();
                            Log.i(TAG, "onInfoWindowClick:map size "+hashMap.size());
                            while(it.hasNext()){
                                Map.Entry<String,Location> pair = (Map.Entry<String,Location>) it.next();
                                Log.i(TAG, "onInfoWindowClick: "+pair.getValue().getName().equals(marker.getTitle()));
                                if (pair.getValue().getName().equals(marker.getTitle())){
                                    it.remove();
                                    Log.i(TAG, "onInfoWindowClick: "+hashMap.size());
                                }

                                Location ssss = new Location();
                                ssss.setKey(pair.getKey());
                                if(arrayList1.contains(ssss)){
                                    arrayList1.remove(ssss);
                                }
                                Log.i(TAG, "contain: "+arrayList1.size());
                            }
                        }
                        editor.putString("locations",gson.toJson(arrayList1));
                        editor.putString("mLocationmap",gson.toJson(arrayList));
                        editor.putString("maplocation",gson.toJson(hashMap));
                        editor.apply();
                        Toast.makeText(getContext(),"ลบข้อมูลแล้ว",Toast.LENGTH_SHORT).show();
                        update();
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                builder.show();
            }
        });


        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng latLng) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("ใส่ชื่อ");
                final EditText input = new EditText(getActivity());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = input.getText().toString();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyymmddHHmm");
                        String now = simpleDateFormat.format(new Date());
                        Location pinlocation = new Location(now,name,Utils.round(latLng.latitude,8),Utils.round(latLng.longitude,8));
                        Gson gson = new Gson();
                        ArrayList<Location> pinlocationArray;
                        Type type = new TypeToken<ArrayList<Location>>(){}.getType();
                        String temp = pref.getString("mLocationmap","");
                        if(!temp.equals(""))
                            pinlocationArray = (ArrayList<Location>) gson.fromJson(temp,type);
                        else
                            pinlocationArray = new ArrayList<>();

                        if (!pinlocationArray.contains(pinlocation)){
                            pinlocationArray.add(pinlocation);
                        }
                        mainLocation = latLng;
                        String json = gson.toJson(pinlocationArray);
                        editor.putString("mLocationmap",json);
                        editor.putString("mainlocation",gson.toJson(mainLocation));
                        editor.apply();
                        update();
                        dialogInterface.dismiss();

                    }
                });
                builder.show();

            }
        });

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case LOCATIONREQUESTCODE :
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                    mMap.setMyLocationEnabled(true);
                }else {

                }
                return;
        }
    }


}
