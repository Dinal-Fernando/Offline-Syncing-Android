package com.example.testsync

import android.app.DownloadManager
import android.content.Context
import android.view.textclassifier.TextLinks

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class MySingleton private constructor(context: Context) {
    private var requestQueue: RequestQueue? = null

    init {
        mCtx = context
        requestQueue = getRequestQueue()
    }

    private fun getRequestQueue(): RequestQueue? {
        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(mCtx.applicationContext)
        return requestQueue
    }

    fun <T> addToRequestQueue(request: Request<T>) {
        getRequestQueue()!!.add(request)
    }

    companion object {
        private var mInstance: MySingleton? = null
        private lateinit var mCtx: Context

        @Synchronized
        fun getInstance(context: Context): MySingleton {
            if (mInstance == null) {
                mInstance = MySingleton(context)
            }
            return mInstance as MySingleton
        }
    }

}
