package com.example.game0109


import android.animation.*
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_game01.*
import kotlinx.android.synthetic.main.activity_main.*



class game01 : AppCompatActivity() {

    var total = 0.0 //점수기록
    var isStart = false
    var startTime = 0L

    public var myService : MyService? = null
    public var isService = false

    //  서비스와 연결에 대한 정의
    var conn = object : ServiceConnection {
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


    val receiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            //Log.d("testTest", "MainActivity::Broadcast Received…" )
            var cnt = myService?.getCount() ?: -1
            when(intent?.getStringExtra("command")){
                "update" ->{
                    //Toast.makeText(applicationContext, "onRecieve", Toast.LENGTH_LONG).show()
                    //playerStatusTextView.text = myService?.getPlayerStatusTest() ?: "update fail."
                }
                else->{
                    Toast.makeText(applicationContext, "others", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    val sensorManager: SensorManager by lazy{
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    val eventListener: SensorEventListener = object : SensorEventListener{
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let{
                if(event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return@let
                val power = Math.pow(event.values[0].toDouble(), 2.0)
                + Math.pow(event.values[1].toDouble(), 2.0)
                + Math.pow(event.values[2].toDouble(), 2.0)

                //측정시작
                if (power > 20 && !isStart){
                    startTime = System.currentTimeMillis()
                    isStart = true
                    game01Result.setText("측정중")
                    AnimatorInflater.loadAnimator(this@game01,R.animator.game01).apply{
                        addListener(object :AnimatorListenerAdapter(){
                            override fun onAnimationEnd(animation: Animator?) {
                                start()
                            }
                        })
                        setTarget(gamePet)
                        start()
                    }
                }

                //측정중
                if (isStart){

                    total = total + power

                    if(System.currentTimeMillis() - startTime > 5000){ //5초후 종료
                        isStart = false
                        game01Result.setText("점수 = ${total}")
                    }
                }
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game01)

        gamePet.setImageResource(R.drawable.duck)

        button10.setOnClickListener {
            // !!total토탈처리 필요!!
            myService?.gameManager?.increaseHappy()
            myService?.gameManager?.increaseHappy()

            myService?.gameManager?.decreaseSatiety()


            myService?.gameManager?.decreaseStamina()

            //Toast.makeText(this , "{$total}점수처리필요",Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onStart(){
        super.onStart()
        init()
    }

    fun init(){
        total = 0.0
        isStart = false
        startTime = 0L

        sensorManager.registerListener(
            eventListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), // 중력제거
            SensorManager.SENSOR_DELAY_NORMAL
        )

    }


    override fun onStop() {
        super.onStop()
        try{
            sensorManager.unregisterListener(eventListener)
        } catch (e:Exception){}
    }




}