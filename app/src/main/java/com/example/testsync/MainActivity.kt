package com.example.testsync

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.lifecycle.Observer

import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Request.Method.GET
import com.android.volley.Request.Method.POST
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.example.testsync.roomDb.entity.Contact
import com.example.testsync.roomDb.repository.ContactRepo
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList
import java.util.HashMap


class MainActivity : AppCompatActivity() {
     lateinit var recyclerView: RecyclerView
     lateinit var Name: EditText

     lateinit var layoutManager: RecyclerView.LayoutManager
     lateinit var adapter: RecyclerAdapter
     var arrayList = ArrayList<Contact>()
     lateinit var broadcastReceiver: BroadcastReceiver

    lateinit var repo:ContactRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById<View>(R.id.recyclerView) as RecyclerView

        repo=ContactRepo.getInstance(this)



        Name = findViewById<View>(R.id.name) as EditText

        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)
        adapter = RecyclerAdapter(arrayList)
        recyclerView.adapter = adapter

        readFromLocalStorage()

        val filter = IntentFilter()
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(NetworkMonitor(), filter)

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                readFromLocalStorage()
            }
        }

    }

    fun submitName(view: View) {
        val name = Name.text.toString()
        saveToAppServer(name)
        Name.setText("")
    }

    private fun saveToAppServer(name: String) {
        if (isNetworkAvailable(this)) {


            val jsonObject1 = JSONObject()
            jsonObject1.put("name",name)
            val request = JsonObjectRequest(POST, DbContract.SERVER_URL, jsonObject1,
                    Response.Listener { response ->

                        val moshi: Moshi = Moshi.Builder().build()
                        val adapter: JsonAdapter<Contact> = moshi.adapter(Contact::class.java)
                        val contact = adapter.fromJson(response.toString())

                        try {
                            if (contact!!.name.isNotEmpty()) {
                                saveToLocalStorage(name, DbContract.SYNC_STATUS_OK)
                            } else {
                                saveToLocalStorage(name, DbContract.SYNC_STATUS_FAILED)
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    },
                    Response.ErrorListener { error ->
                        saveToLocalStorage(name, DbContract.SYNC_STATUS_FAILED)
                    })

            MySingleton.getInstance(this@MainActivity).addToRequestQueue(request)

        } else {
            saveToLocalStorage(name, DbContract.SYNC_STATUS_FAILED)
        }

    }

    private fun saveToLocalStorage(name: String, sync: Int) {
        val contact=Contact(
                id=null,
                name = name,
                syncstatus = sync
        )
        repo.insert(contact)
        readFromLocalStorage()

    }

    private fun readFromLocalStorage() {
        arrayList.clear()

        val all = repo.getall()
            for(contact in all){
                val id=contact.id
                val name = contact.name
                val syncStatus = contact.syncstatus
                arrayList.add(Contact(id,name,syncStatus))
            }
        adapter.notifyDataSetChanged()

    }

    private fun isNetworkAvailable(context: Context): Boolean {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).apply {
            return getNetworkCapabilities(activeNetwork)?.run {
                when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } ?: false
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(broadcastReceiver, IntentFilter(DbContract.UI_UPDATE_BROADCAST))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    fun  checkInContacts(name:String):Boolean{
        val all = ContactRepo.getInstance(this).getall()
        var state=false
        for(contact in all){
            if (name.equals(contact.name)){
                state= true
            }
        }
        return state
    }

    fun syncContacts(view: View) {

        val jsonObjectRequest=JsonArrayRequest(GET,DbContract.SERVER_URL,null,
                Response.Listener {response ->
                    val moshi: Moshi = Moshi.Builder().build()
                    val type = Types.newParameterizedType(List::class.java, Contact::class.java)
                    val adapter: JsonAdapter<List<Contact>> = moshi.adapter(type)
                    val contact = adapter.fromJson(response.toString())
                    println(contact)

                    if (contact != null) {
                        for(cont in contact){
                            val exist=checkInContacts(cont.name)
                            println(exist)
                        }
                    }
                },
                Response.ErrorListener {error ->
                    println(error)
                }
                )
//        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
//                10000,
//                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
//        )


//
//
//        if (sync_status == DbContract.SYNC_STATUS_FAILED) {
//
//            val name = contact.name
//
//            val jsonObject1 = JSONObject()
//            jsonObject1.put("name",name)
//            val request = JsonObjectRequest(Request.Method.POST, DbContract.SERVER_URL, jsonObject1,
//                    Response.Listener { response ->
//
//                        val moshi: Moshi = Moshi.Builder().build()
//                        val adapter: JsonAdapter<Contact> = moshi.adapter(Contact::class.java)
//                        val contact = adapter.fromJson(response.toString())
//
//                        try {
//                            if (contact!!.name.isNotEmpty()) {
//                                ContactRepo.getInstance(this).update_Sync_status(name,DbContract.SYNC_STATUS_OK)
//                                this.sendBroadcast(Intent(DbContract.UI_UPDATE_BROADCAST))
//                            } else {
//
//                            }
//                        } catch (e: JSONException) {
//                            e.printStackTrace()
//                        }
//                    },
//                    Response.ErrorListener { error ->
//
//                    })

            MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
        }

    }

