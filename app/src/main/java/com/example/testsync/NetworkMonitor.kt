package com.example.testsync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

import android.os.strictmode.SqliteObjectLeakedViolation
import androidx.lifecycle.Observer
import androidx.lifecycle.observe

import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.example.testsync.roomDb.entity.Contact
import com.example.testsync.roomDb.repository.ContactRepo
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

import org.json.JSONException
import org.json.JSONObject

import java.util.HashMap

class NetworkMonitor : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (isNetworkAvailable(context)) {

            val all = ContactRepo.getInstance(context).getall()
                for(contact in all){
                    val sync_status = contact.syncstatus
                    if (sync_status == DbContract.SYNC_STATUS_FAILED) {

                        val name = contact.name

                        val jsonObject1 = JSONObject()
                        jsonObject1.put("name",name)
                        val request = JsonObjectRequest(Request.Method.POST, DbContract.SERVER_URL, jsonObject1,
                                Response.Listener { response ->

                                    val moshi: Moshi = Moshi.Builder().build()
                                    val adapter: JsonAdapter<Contact> = moshi.adapter(Contact::class.java)
                                    val contact = adapter.fromJson(response.toString())

                                    try {
                                        if (contact!!.name.isNotEmpty()) {
                                            ContactRepo.getInstance(context).update_Sync_status(name,DbContract.SYNC_STATUS_OK)
                                            context.sendBroadcast(Intent(DbContract.UI_UPDATE_BROADCAST))
                                        } else {

                                        }
                                    } catch (e: JSONException) {
                                        e.printStackTrace()
                                    }
                                },
                                Response.ErrorListener { error ->

                                })

                        MySingleton.getInstance(context).addToRequestQueue(request)
                    }
                }


        }

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
}
