package com.example.romeo.gpstracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.ArraySet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.util.Attributes;
import com.example.romeo.gpstracker.utils.Item;
import com.example.romeo.gpstracker.utils.ListviewAdapter;
import com.example.romeo.gpstracker.utils.Location;
import com.example.romeo.gpstracker.utils.MapLocation;
import com.example.romeo.gpstracker.utils.ToTimeStamp;
import com.example.romeo.gpstracker.utils.Utils;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.data.remote.EmailSignInHandler;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_QR_SCAN = 500;
    private static final int REQUEST_CAMERA = 100;

    public  SharedPreferences prefs;
    public  SharedPreferences.Editor edit;

    MapFragment mapFragment;

    private FirebaseAuth mAuth;
    FirebaseUser user;

     Set<String> uidset;
     Set<String> nameset;

    EditText input;
    // User view
    ImageView profile;
    TextView txtName;
    TextView txtEmail;
    MyAdapter myAdapter;
    ListView listView;
    private  List<Item> itemList;
    ListviewAdapter mAdapter;
//    MapFragment mapFragment;

    DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)){

            }else {
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.VIBRATE,Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CAMERA);
            }
        }else {
            mapFragment = new MapFragment();
            mapFragment.setMain(this);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.content,mapFragment );
            transaction.commit();
        }

        mAuth = FirebaseAuth.getInstance();
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        profile = (ImageView) findViewById(R.id.imageprofile);

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mAuth.getCurrentUser() == null)
                    signin();
            }
        });

        if(mAuth.getCurrentUser() == null)
            updateUI(null);
        else
            updateUI(mAuth.getCurrentUser());

        prefs = getSharedPreferences("locationPref",Context.MODE_PRIVATE);
        edit = prefs.edit();

        uidset = prefs.getStringSet("uid",new HashSet<String>());
        nameset = prefs.getStringSet("name",new HashSet<String>());


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mAuth.getCurrentUser() == null){
                    signin();
                }else {
                    Intent i = new Intent(MainActivity.this, QrCodeActivity.class);
                    startActivityForResult(i, REQUEST_CODE_QR_SCAN);
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference("items");

        itemList = new ArrayList<>();
        listView = findViewById(R.id.listView);
        fetchdata();

        myAdapter = new MyAdapter(MainActivity.this,itemList);
        mAdapter = new ListviewAdapter(this,itemList,this);
        listView.setAdapter(mAdapter);
        mAdapter.setMode(Attributes.Mode.Single);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this,DetailsActivity.class);
                intent.putExtra("uid",itemList.get(position).getUid());
                intent.putExtra("name",itemList.get(position).getName());
                startActivity(intent);

            }
        });

        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.e("ListView", "OnTouch");
                return false;
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(get, "OnItemLongClickListener", Toast.LENGTH_SHORT).show();
                ((SwipeLayout)(listView.getChildAt(position - listView.getFirstVisiblePosition()))).open(true);
                return true;
            }
        });

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                Log.e("ListView", "onScrollStateChanged");
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });

        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.e("ListView", "onItemSelected:" + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.e("ListView", "onNothingSelected:");
            }
        });

    }


    public void delete(int position){
        final Gson gson = new Gson();
        ArrayList<Location> locationArrayList1 = new ArrayList<>();
        String locatetemp = prefs.getString("locations","");
        Type type = new TypeToken<ArrayList<Location>>(){}.getType();

        if(!locatetemp.equals("")){
            locationArrayList1 = (ArrayList<Location>) gson.fromJson(locatetemp,type);
            Location search = new Location();
            search.setName(itemList.get(position).getName());
            locationArrayList1.remove(search);
            edit.putString("locations",gson.toJson(locationArrayList1));
            edit.apply();
            itemList.remove(position);
        }


        mapFragment.update();
        myAdapter.setList(itemList);
        listView.setAdapter(myAdapter);
        Toast.makeText(MainActivity.this,"ลบแล้ว",Toast.LENGTH_SHORT).show();
    }


    public void fetchdata(){
        itemList.clear();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mLocationRef = database.getReference("items");
        final Gson gson = new Gson();

        String temp = prefs.getString("locations","");
        Type type = new TypeToken<ArrayList<Location>>(){}.getType();
        final ArrayList<Location> arrayList;
        if(!temp.equals(""))
            arrayList = (ArrayList<Location>) gson.fromJson(temp,type);
        else
            arrayList = new ArrayList<>();

        Log.i(TAG, "fetchdata: "+arrayList.size());

        if(arrayList.size() == 0){
            itemList.clear();
//            mAdapter.setList((List<Item>) new ArrayList<Item>());
            listView.setAdapter(mAdapter);
        }

        for (final Location a:arrayList) {
            mLocationRef.child(a.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String uid = dataSnapshot.getKey();
                    String name = a.getName();
                    Item item = new Item(uid, name);
                    if(!itemList.contains(item))
                        itemList.add(item);
                    if(arrayList.size() == 0){
                        itemList.clear();
                    }
                    mAdapter.setList(itemList);
                    listView.setAdapter(mAdapter);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

    }


    private void signin(){
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );
        startActivityForResult(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build(),123
        );
    }

    private void signout(){
        mAuth.signOut();
        updateUI(null);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CAMERA :
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                    mapFragment = new MapFragment();
                    mapFragment.setMain(this);
                    FragmentManager manager = getSupportFragmentManager();
                    FragmentTransaction transaction = manager.beginTransaction();
                    transaction.replace(R.id.content,mapFragment );
                    transaction.commit();
                }else {

                }
                return;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode != Activity.RESULT_OK)
        {
            Log.d(TAG,"COULD NOT GET A GOOD RESULT.");
            if(data==null)
                return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");
            if( result!=null)
            {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Scan Error");
                alertDialog.setMessage("QR Code could not be scanned");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            return;

        }

        if(requestCode == REQUEST_CODE_QR_SCAN)
        {
            if(data==null)
                return;
            //Getting the passed result
            final String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
//            Log.d(TAG,"Have scan result in your app activity :"+ result);
            input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setGravity(Gravity.CENTER);
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
            alertDialog.setTitle("ใส่ชื่ออ้างอิง");
            alertDialog.setMessage(result);
            alertDialog.setView(input);
            alertDialog.setPositiveButton("บันทึก",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (!input.getText().toString().equals("")) {
                                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            if (dataSnapshot.hasChild(result)) {
                                                selectmlocation(result, input.getText().toString());
                                            }
                                            else
                                                Toast.makeText(MainActivity.this,"ไม่พบอุปกรณ์",Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                    }
                                });
                            }

                            dialog.dismiss();
                        }
                    });
            alertDialog.setNegativeButton("ยกเลิก", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            AlertDialog alertDialog1 = alertDialog.create();
            alertDialog1.show();

        }

        if(requestCode == 123){
            updateUI(mAuth.getCurrentUser());
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            Log.i(TAG, "onActivityResult: "+data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            }catch (ApiException e){
                e.printStackTrace();
            }
        }
    }

    private void selectmlocation(final String result, final String input){
        final ArrayList<Location> locationArrayList = new ArrayList<>();
        AlertDialog.Builder alertDialog1 = new AlertDialog.Builder(MainActivity.this);
        alertDialog1.setTitle("เลือกสถานที่อ้างอิง");
        View view = getLayoutInflater().inflate(R.layout.spinner_layout,null);
        final Spinner spinner = (Spinner) view.findViewById(R.id.spinner);
        final ArrayList<String> items = new ArrayList<>();
        final Gson gson = new Gson();
        ArrayList<Location> arrayList = new ArrayList<>();
        String temp = prefs.getString("mLocationmap","");
        Type type = new TypeToken<ArrayList<Location>>(){}.getType();
        if(!temp.equals("")){
            arrayList = (ArrayList<Location>)gson.fromJson(temp,type);
        }
        for (Location a:arrayList) {
            items.add(a.getName());
            locationArrayList.add(a);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        alertDialog1.setPositiveButton("บันทึก", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(!spinner.getSelectedItem().toString().equalsIgnoreCase("")){
                    Toast.makeText(MainActivity.this,""+spinner.getSelectedItem().toString(),Toast.LENGTH_SHORT).show();
                    HashMap<String,Location> hashMap = new HashMap<>();
                    String temp = prefs.getString("maplocation","");
                    Type type = new TypeToken<HashMap<String,Location>>(){}.getType();
                    if(!temp.equals(""))
                        hashMap = (HashMap<String,Location>)gson.fromJson(temp,type);
                    Log.i(TAG, "maplocation size: "+hashMap.size());
                    String key = spinner.getSelectedItem().toString();
                    Location mlocation = locationArrayList.get(items.indexOf(key));
                    hashMap.put(result,mlocation);
                    edit.putString("maplocation",gson.toJson(hashMap));
                    edit.apply();
                    Log.i(TAG, "after maplocation size: "+hashMap.size());

                    ArrayList<Location> locationArrayList1 = new ArrayList<>();
                    Type type2 = new TypeToken<ArrayList<Location>>(){}.getType();
                    String locatetemp = prefs.getString("locations","");
                    if(!locatetemp.equals(""))
                        locationArrayList1 = (ArrayList<Location>) gson.fromJson(locatetemp,type2);
                    Location locattemp = new Location();
                    locattemp.setKey(result);
                    locattemp.setName(input);
                    if(!locationArrayList1.contains(locattemp)){
                        locationArrayList1.add(locattemp);
                    }

                    edit.putString("locations",gson.toJson(locationArrayList1));
                    edit.apply();


                    // save map of location and pin
                    HashMap<String,String> locationmap = new HashMap<>();
                    String json = prefs.getString("locationmap","");
                    Type token = new TypeToken<HashMap<String,String>>(){}.getType();
                    if(!json.equals("")){
                        locationmap = (HashMap<String,String>) gson.fromJson(json,token);
                    }
                    locationmap.put(result,key);
                    edit.putString("locationmap",gson.toJson(locationmap));
                    edit.apply();


                    fetchdata();
                    mapFragment.update();




                }
                dialogInterface.dismiss();
            }
        });

        alertDialog1.setNegativeButton("ยกเลิก", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        alertDialog1.setView(view);
        AlertDialog dialog1 = alertDialog1.create();
        if(items.size() == 0){
            Toast.makeText(MainActivity.this,"กรุณาเพิ่มสถานที่อ้างอิงก่อน",Toast.LENGTH_SHORT).show();
        }
        else
            dialog1.show();
    }


    private void firebaseAuthWithGoogle(GoogleSignInAccount account){
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(),null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    user = mAuth.getCurrentUser();
                    updateUI(user);
                    Toast.makeText(getApplicationContext(),"เข้าสู่ระบบ!!!",Toast.LENGTH_SHORT).show();
                }else {
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                }
            }
        });
    }


    private void updateUI(FirebaseUser user){
        Log.i(TAG, "updateUI: "+(profile == null));
        if(user != null){
            // User profile
            txtName.setText(user.getDisplayName());
            txtEmail.setText(user.getEmail());
            Picasso.get().load("https://dummyimage.com/150x150/fff/000.png&text="+user.getDisplayName().charAt(0)).into(profile);
        }else {
            txtName.setText("Guess");
            txtEmail.setText("");
            Picasso.get().load("https://dummyimage.com/150x150/fff/000.png&text=G").into(profile);
        }



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            signout();
            Toast.makeText(MainActivity.this,"Logout!!!",Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public class MyAdapter extends BaseAdapter {
        List<Item> itemList;
        Context mcontext;

        MyAdapter(Context mcontext,List<Item> itemList){
            this.mcontext = mcontext;
            this.itemList = itemList;
        }

        public void setList(List<Item> itemList){
            this.itemList = itemList;
        }

        public void addItem(Item item){
            itemList.add(item);
        }

        @Override
        public int getCount() {
            return itemList.size();
//            return 10;
        }

        @Override
        public Object getItem(int i) {
            return itemList.get(i);
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
            row = inflater.inflate(R.layout.custom_row, viewGroup,false);
            TextView title = row.findViewById(R.id.textView2);
            title.setText(itemList.get(i).getName());
            return row;
        }
    }
}
