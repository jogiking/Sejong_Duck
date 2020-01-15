package com.example.game0109

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.content.*
import android.net.Uri
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Button
import android.provider.Settings
import android.view.View

import android.widget.Toast
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_status.*

import java.util.Date
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    var state = myStatus() // 클래스에 스테이터스 저장
    var zzz = false // 자는 상태인지
    var ispoop = true // 똥이 있는지

    var serviceIntent : Intent? = null

    //  서비스와 연결에 대한 정의
    public var myService : MyService? = null
    public var isService = false

    //  서비스와 연결에 대한 정의
    var conn = object : ServiceConnection{
        override fun onServiceDisconnected(name: ComponentName?) {
            isService = false
            //myService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            var binder = service as MyService.LocalBinder
            myService = binder.getService()
            isService = true
        }
    }


    val receiver = object :BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            //Log.d("testTest", "MainActivity::Broadcast Received…" )
            var cnt = myService?.getCount() ?: -1
            when(intent?.getStringExtra("command")){
                "update" ->{
                    //Toast.makeText(applicationContext, "onRecieve", Toast.LENGTH_LONG).show()
                }
                else->{
                    Toast.makeText(applicationContext, "others", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        /*———————————————————————————————————————*/
        //  서비스, 스레드 생성
        registerReceiver(receiver, IntentFilter("com.example.TO_ACTIVITY"))
        serviceIntent = Intent(applicationContext, MyService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, conn, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        unbindService(conn)
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)

        if (serviceIntent != null) {
            stopService(serviceIntent)
            serviceIntent = null
        }


        //메인에서 나갈경우 현재 상태 데이터베이스에 저장 (복구용?)
        val preference by lazy { getSharedPreferences("MainActivity", Context.MODE_PRIVATE)}
        val id = preference.getString("m_monsterId","")
        var ref = FirebaseDatabase.getInstance().getReference("/monster/$id")
        ref.setValue(state)

        super.onDestroy()
    }

    //시간 (한국시간 아닌듯)
    fun isSun() : Boolean{
        val cd : Calendar? = Calendar.getInstance()
        val ampm = cd?.get(Calendar.AM_PM)
        val hour:Int? = cd?.get(Calendar.HOUR)

        if (ampm == Calendar.AM &&  8 <= hour!!)
            return true
        else if (ampm == Calendar.PM && hour!! <= 8)
            return true
        else
            return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val preference by lazy { getSharedPreferences("MainActivity", Context.MODE_PRIVATE)} //쉐어드


        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        var isWhiteListing = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            isWhiteListing = powerManager.isIgnoringBatteryOptimizations(applicationContext.packageName)
        }
        if (!isWhiteListing) {
            val intent = Intent()
            intent.action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:" + applicationContext.packageName)
            startActivity(intent)
        }




        //그림 등록
        pet.setImageResource(R.drawable.duck)
        sleep.setImageResource(R.drawable.zzz)
        poop.setImageResource(R.drawable.poop)

        //시간에따라 밤낮 배경 설정
        if (!isSun()) {
            background.setImageResource(R.drawable.bg_sun)
        }
        else {
            background.setImageResource(R.drawable.bg_moon)
        }

        //오리 움직임
        AnimatorInflater.loadAnimator(this@MainActivity,R.animator.main).apply{
            addListener(object : AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator?) {
                    start()
                }
            })
            setTarget(pet)
            start()
        }

        button.setOnClickListener {
            myService?.gameManager?.increaseSatiety()
            myService?.gameManager?.increaseSatiety()
            myService?.gameManager?.increaseSatiety()
            myService?.gameManager?.increaseSatiety()
            myService?.gameManager?.increaseSatiety()
            Toast.makeText(this,"밥주기 성공" + myService?.getPlayerStatusTest(), Toast.LENGTH_SHORT).show()
        }

        //똥치우기
        button2.setOnClickListener {
            if(ispoop){
                ispoop = false
                poop.visibility = View.INVISIBLE
            }
            Toast.makeText(this,"똥치우기 성공", Toast.LENGTH_SHORT).show()
    }

        button3.setOnClickListener {
            if(myService?.gameManager?.player?.병 == false)
            {
                Toast.makeText(this,"이미 건강함", Toast.LENGTH_SHORT).show()
            }
            else {
                myService?.gameManager?.healing()
                if (myService?.gameManager?.player?.병 == false) {
                    Toast.makeText(this, "치료 성공", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "치료 실패 사망", Toast.LENGTH_SHORT).show()
                }
            }
        }

        //불끄기
        button4.setOnClickListener {
            //!! 불끈 여부에 따라서(zzz)에 따른 상태변화? 스태미너업?
            if (!zzz) {
                zzz = true
                background.visibility = View.INVISIBLE
                pet.visibility = View.INVISIBLE
                sleep.visibility = View.VISIBLE
                val intent = Intent("com.example.TO_SERVICE")
                intent.putExtra("command", "sleep")
                sendBroadcast(intent)
            }
            else {
                zzz = false
                background.visibility = View.VISIBLE
                pet.visibility = View.VISIBLE
                sleep.visibility = View.INVISIBLE
                val intent = Intent("com.example.TO_SERVICE")
                intent.putExtra("command", "awake")
                sendBroadcast(intent)
            }
        }

        //게임처음시작이면 새로 객체 생성
        var isNew = preference.getBoolean("isStart", true)
        Log.d("monster", "{$isNew}")
        if(isNew){
            preference.edit().putBoolean("isStart",false).apply()
            val newRef = FirebaseDatabase.getInstance().getReference("monster").push()

            //상태 초기화
            state = myStatus()
            state.name ="default" // 이름
            state.weight = 1
            state.level = 1
            state.speed = 1
            state.strong = 1
            state.exp = 0
            state.isDeath = false
            state.hungry = 100
            state.stamina = 100
            state.isSeek = false
            state.walk = 0
            state.id = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID) // 디바이스 아이디
            state.monsterId = newRef.key

            newRef.setValue(state) // 데이터베이스에 저장
            Log.d("monster","make")
            preference.edit().putString("id",state.monsterId).apply() // 데이터베이스에 읽어오거나 저장할때 필요
        }

        //놀기
        button5.setOnClickListener {
            Log.d("monster", "{${state.exp} ${state.weight}")
            val intent = Intent(this@MainActivity, game01::class.java)
            startActivity(intent)
        }

        //지도
        button6.setOnClickListener {
            val intent = Intent(this@MainActivity, Map::class.java)
            startActivity(intent)
        }

        //상태
        button7.setOnClickListener {
            val intent = Intent(this@MainActivity, Status::class.java)
            intent.putExtra("status" , myService?.getPlayerStatusTest())
            startActivity(intent)
        }

        //랭킹
        button8.setOnClickListener {
            val intent = Intent(this@MainActivity, RankingActivity::class.java)
            val po : Double = myService?.gameManager?.level?.toDouble() ?: 1.0
            intent.putExtra("point", po)

            startActivity(intent)
        }

        /*———————————————————————————*/
        //registerReceiver(receiver, IntentFilter("com.example.TO_ACTIVITY"))
        //val intentToService = Intent(applicationContext, MyService::class.java)
        //startService(intentToService)
        //bindService(intentToService, conn, Context.BIND_AUTO_CREATE)
    }
}
