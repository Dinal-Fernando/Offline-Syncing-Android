package com.example.testsync;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    EditText Name;

    RecyclerView.LayoutManager layoutManager;
    RecyclerAdapter adapter;
    ArrayList<Contact> arrayList=new ArrayList<>();
    BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView=(RecyclerView)findViewById(R.id.recyclerView);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(new NetworkMonitor(), filter);

        Name=(EditText)findViewById(R.id.name);

        layoutManager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        adapter=new RecyclerAdapter((arrayList));
        recyclerView.setAdapter(adapter);

        readFromLocalStorage();

        broadcastReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                readFromLocalStorage();
            }
        };

    }

    public void submitName(View view) {
        String name=Name.getText().toString();
        saveToAppServer(name);
        Name.setText("");
    }

    private void saveToAppServer(final String name){
        if(checkNetworkConnection()){
            StringRequest stringRequest=new StringRequest(Request.Method.POST, DbContract.SERVER_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonObject=new JSONObject(response);
                                String Response=jsonObject.getString("response");
                                if(Response.equals("OK"))
                                {
                                    saveToLocalStorage(name,DbContract.SYNC_STATUS_OK);
                                }
                                else
                                {
                                    saveToLocalStorage(name,DbContract.SYNC_STATUS_FAILED);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },

                    new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    saveToLocalStorage(name,DbContract.SYNC_STATUS_FAILED);
                }
            })
            {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String,String> params=new HashMap<>();
                    params.put("name",name);
                    return params;
                }
            };

            MySingleton.getInstance(MainActivity.this).addToRequestQueue(stringRequest);

        }
        else {
            saveToLocalStorage(name,DbContract.SYNC_STATUS_FAILED);
        }

    }

    private void saveToLocalStorage(String name,int sync){
        DbHelper dbHelper=new DbHelper(this);
        SQLiteDatabase database=dbHelper.getWritableDatabase();
        dbHelper.saveToLocalDatabase(name,sync,database);
        readFromLocalStorage();
        dbHelper.close();
    }

    private void readFromLocalStorage(){
        arrayList.clear();
        DbHelper dbHelper=new DbHelper(this);
        SQLiteDatabase database=dbHelper.getReadableDatabase();

        Cursor cursor =dbHelper.readFromLocalDatabase(database);

        while (cursor.moveToNext()){
            String name=cursor.getString(cursor.getColumnIndex(DbContract.NAME));
            int sync_status=cursor.getInt(cursor.getColumnIndex(DbContract.SYNC_STATUS));
            arrayList.add(new Contact(name,sync_status));
        }

        adapter.notifyDataSetChanged();
        cursor.close();
        dbHelper.close();
    }

    public boolean checkNetworkConnection(){
        ConnectivityManager connectivityManager=(ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=connectivityManager.getActiveNetworkInfo();
        return (networkInfo!=null && networkInfo.isConnected());
    }

    @Override
    protected void onStart() {

        super.onStart();
        registerReceiver(broadcastReceiver,new IntentFilter(DbContract.UI_UPDATE_BROADCAST));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }
}
