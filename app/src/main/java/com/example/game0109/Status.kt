package com.example.game0109

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_status.*

class Status : AppCompatActivity() {

    var myService : MyService? = null
    var isService = false

    //  서비스와 연결에 대한 정의
    var conn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            isService = false
            myService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            var binder = service as MyService.LocalBinder
            myService = binder.getService()
            isService = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        val preference by lazy { getSharedPreferences("MainActivity", Context.MODE_PRIVATE)} //메인과 쉐어드


        val str : String  by lazy { myService?.getPlayerStatusTest() ?:  "update fail." }

        val str1 = intent.getStringExtra("status")
        textView.text = str1



        button9.setOnClickListener {  finish() }
    }

    override fun onStart() {
        super.onStart()
        val intentToService = Intent(applicationContext, MyService::class.java)
        bindService(intentToService, conn, Context.BIND_AUTO_CREATE)
    }



    override fun onStop() {
        super.onStop()
        unbindService(conn)
    }
}
