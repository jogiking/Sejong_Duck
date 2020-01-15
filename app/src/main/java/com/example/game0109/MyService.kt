package com.example.game0109

import android.app.*
import android.content.*
import android.graphics.Color
import android.media.RingtoneManager
import android.os.*
import android.os.Looper.myLooper
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.Toast
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*


class MyService : Service, Serializable {
    constructor() : super()

    var serviceIntent: Intent? = null
    var preference: SharedPreferences? = null
    var cnt = 0

    val receiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.getStringExtra("command")){
                "start" ->{
                    Toast.makeText(applicationContext, "onReceive in Service, start", Toast.LENGTH_LONG).show()
                }
                "remove" ->{
                    Toast.makeText(applicationContext, "Game Data Clear!", Toast.LENGTH_LONG).show()
                }
                "sleep" ->{
                   gameManager.setSleepMode()
                }
                "awake"->{
                    gameManager.setAwakeMode()
                }
                else ->{
                    Toast.makeText(applicationContext, "onReceive in Service, else", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MyService = this@MyService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        preference = getSharedPreferences("Backup", Context.MODE_PRIVATE)

        gameManager.isGameOver = preference!!.getBoolean("isGameOver", false)
        gameManager.age = preference!!.getInt("age", 1)
        gameManager.name = preference!!.getString("name", "귀여운 오리")
        gameManager.weight = preference!!.getFloat("weight", 1.0f)
        gameManager.level = preference!!.getFloat("level", 1.0f)
        gameManager.speed = preference!!.getInt("speed", 1)
        gameManager.power = preference!!.getInt("power", 1)
        gameManager.player.병 = preference!!.getBoolean("isDisease", false)
        gameManager.player.포만감 = preference!!.getFloat("full", 1.0f)
        gameManager.player.스테미너 = preference!!.getFloat("stamina", 1.0f)
        gameManager.player.행복 = preference!!.getFloat("happy", 1.0f)
        cnt = preference!!.getInt("cnt", -1)

        registerReceiver(receiver, IntentFilter("com.example.TO_SERVICE"))
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceIntent = intent

        thread = ProcessThread()
        thread?.start()

        return Service.START_NOT_STICKY
    }


    override fun onDestroy() {

        val editor = preference?.edit()
        // set Key values
        editor?.putInt("cnt", cnt)
        editor?.putBoolean("isDisease", gameManager.player.병)
        editor?.putFloat("full", gameManager.player.포만감)
        editor?.putFloat("stamina", gameManager.player.스테미너)
        editor?.putBoolean("isGameOver", gameManager.isGameOver)
        editor?.putFloat("happy", gameManager.player.행복)
        editor?.putInt("age", gameManager.age)
        editor?.putFloat("weight", gameManager.weight)
        editor?.putString("name", gameManager.name)
        editor?.putFloat("level", gameManager.level)
        editor?.putInt("speed", gameManager.speed)
        editor?.putInt("power", gameManager.power)
        editor?.apply()

        serviceIntent = null
        if(gameManager.isGameOver == false) setAlarmTimer()
        Thread.currentThread().interrupt()

        if (thread != null) {
            thread?.interrupt()
            thread = null
        }

        super.onDestroy()
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    @Synchronized fun getCount() : Int{
        return cnt
    }

    @Synchronized fun getPlayerStatusTest() : String{
        val str = "name : ${gameManager.name}\n" +
                "isGameOver : ${gameManager.isGameOver}\n" +
                "age : ${gameManager.age}\n" +
                "level : ${gameManager.level}\n" +
                "weight : ${gameManager.weight}\n" +
                "병 : ${gameManager.player.병}\n" +
                "스테미너 : ${gameManager.player.스테미너}\n"+
                "포만감 : ${gameManager.player.포만감}\n"+
                "행복 : ${gameManager.player.행복}"
        return str
    }

    val gameManager = GameManager()

    var thread : ProcessThread? = null

    inner class ProcessThread : Thread(){
        override fun run() {
            cnt = cnt + preference!!.getInt("Test", 0)

            //30 * 60 * 1000
            loop@while(true){
                sleep(900000)
                /*--------------------------------------------*/
                //기본 로그 출력 영역
                Log.d("testTest", "MyService::Thread::${cnt}초, "+
                        "${gameManager.age}살," +
                        "${gameManager.isGameOver}," +
                        "${gameManager.weight}kg," +
                        "${gameManager.player.병}" +
                        "${gameManager.player.행복}happy" +
                        "${gameManager.player.스테미너}stat" +
                        "${gameManager.player.포만감}poman")

                Log.d("ServiceOnStatus", "MyService::Thread : ${cnt} 초, "+
                        "${gameManager.age} + " +
                        "${gameManager.isGameOver} + " +
                        "${gameManager.speed}")
                /*--------------------------------------------*/
                //게임 종료 여부 확인
                if(gameManager.isGameOver){
                    //stopSelf()
                    preference?.edit()?.clear()?.apply()
                    continue@loop
                }
                /*--------------------------------------------*/
                // 푸시 알림 처리
                // 병에 걸렸을 때 푸시 알림을 준다
                when{
                    (gameManager.player.병 == true) ->{
                        val sdf = SimpleDateFormat("aa hh:mm")
                        sendNotification(sdf.format(Date())+" : 오리 : 주인님, 병에 걸렸어요... ")
                    }
                    (gameManager.player.포만감 < 0.5f) ->{
                        val sdf = SimpleDateFormat("aa hh:mm")
                        sendNotification(sdf.format(Date())+" : 오리 : 주인님, 배고파요... ")
                    }
                    (gameManager.isGameOver == true) ->{
                        val sdf = SimpleDateFormat("aa hh:mm")
                        sendNotification(sdf.format(Date())+" : 오리 : 주인님... ")
                    }
                }
                /*--------------------------------------------*/
                //게임 업데이트
                //if()

                gameManager.update()

                /*--------------------------------------------*/
                //업데이트 정보를 담은 인텐트로 브로드캐스트를 사용하여 액티비티로 전송
                val intent = Intent("com.example.TO_ACTIVITY")
                intent.putExtra("command", "update")
                sendBroadcast(intent)
                cnt++
            }
        }
    }

    protected fun setAlarmTimer() {
        val c = Calendar.getInstance()
        c.timeInMillis = System.currentTimeMillis()
        c.add(Calendar.SECOND, 1)
        val intent = Intent(this, AlarmReceiver::class.java)

        val sender = PendingIntent.getBroadcast(this, 0, intent, 0)

        val mAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, c.timeInMillis, sender)
    }



    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)//drawable.splash)
            .setContentTitle("오리 키우기")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0 , notificationBuilder.build())
    }
}