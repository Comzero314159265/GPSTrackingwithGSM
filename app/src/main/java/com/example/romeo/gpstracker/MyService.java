package com.example.romeo.gpstracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.romeo.gpstracker.utils.Location;
import com.example.romeo.gpstracker.utils.ToTimeStamp;
import com.example.romeo.gpstracker.utils.Utils;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.firebase.ui.auth.ui.phone.SubmitConfirmationCodeFragment.TAG;


public class MyService extends Service {
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private Set<String> uidset;
    private Set<String> nameset;
    Iterator<String> uidI;
    Iterator<String> nameI;

    LatLng mainLocation;
    List<String> namelist;
    List<String> idliist;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final Gson gson = new Gson();
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference databaseReference = database.getReference("items");

        preferences = getSharedPreferences("locationPref", MODE_PRIVATE);
        editor = preferences.edit();


        ArrayList<Location> locations = new ArrayList<>();
        // fetch location in pref
        String json = preferences.getString("locations", "");
        Type type = new TypeToken<ArrayList<Location>>() {
        }.getType();
        if (!json.equals("")) {
            locations = (ArrayList<Location>) gson.fromJson(json, type);
        }

        Log.i("test", "onCreate: " + locations.size());

        // Loop for fetch data
        for (final Location loc : locations) {
            Log.i("test", "onCreate: " + loc.getKey());
            databaseReference.child(loc.getKey()).getRef().child("locat").getRef().orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    HashMap<String, String> hashMap = new HashMap<>();
                    String temp2 = preferences.getString("locationmap", "");
                    Type type = new TypeToken<HashMap<String, String>>() {
                    }.getType();
                    if (!temp2.equals("")) {
                        hashMap = (HashMap<String, String>) gson.fromJson(temp2, type);
                    }

                    String temp1 = preferences.getString("mLocationmap", "");
                    List<Location> locationArrayList1 = new ArrayList<>();
                    Type thistype = new TypeToken<ArrayList<Location>>() {
                    }.getType();
                    if (!temp1.equals("")) {
                        locationArrayList1 = (ArrayList<Location>) gson.fromJson(temp1, thistype);
                    }
                    Log.i("test", "onDataChange: " + loc.getName());

                    for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                        Location nowloc = dataSnapshot1.getValue(Location.class);
                        nowloc.setKey(loc.getKey());
                        nowloc.setName(loc.getName());
                        nowloc.setTimestamp(ToTimeStamp.parse(dataSnapshot1.getKey()));

                        Log.i("test", "timestamp: " + nowloc.getTimestamp());

                        String test = hashMap.get(dataSnapshot1.getRef().getParent().getRef().getParent().getKey());
                        if (locationArrayList1.contains(new Location(test))) {
                            Location pinlocation = locationArrayList1.get(locationArrayList1.indexOf(new Location(test)));
                            double lat = pinlocation.getLat();
                            double lng = pinlocation.getLng();

                            if (lat > 0 && lng > 0) {
                                mainLocation = new LatLng(lat, lng);
                            }

                            double distance = 0;

                            Log.i("test", "onservice: loc " + nowloc.getLat() + " " + nowloc.getLng());
                            Log.i("test", "onservice: pin " + mainLocation.latitude + " " + mainLocation.longitude);

                            if (nowloc.getLat() > 0 && nowloc.getLng() > 0) {
                                distance = Utils.CalculationByDistance(mainLocation, new LatLng(nowloc.getLat(), nowloc.getLng()));
                                Log.i("test", "onservice: distance " + nowloc.getTimestamp());
                                String time = nowloc.getTimestamp();
                                if (distance > 0.250) {
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    PendingIntent contentintent = PendingIntent.getActivities(getApplicationContext(), 0, new Intent[]{intent}, PendingIntent.FLAG_UPDATE_CURRENT);
                                    Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                                    Notification notification = new NotificationCompat.Builder(getApplicationContext())
                                            .setSmallIcon(R.drawable.ic_menu_camera)
                                            .setContentIntent(contentintent)
                                            .setContentTitle("แจ้งเตือน !!!")
                                            .setSound(alarmSound)
                                            .setContentText(nowloc.getName() + " : ออกนอกพื้นที่ | เวลา : " + time)
                                            .setAutoCancel(false).build();
                                    Log.i("test", "onNotification: " + time);

//
                                    NotificationManager notificationManager = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
                                    notificationManager.notify(2000, notification);
                                }


                            }

                        }


                    }


                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }
}
