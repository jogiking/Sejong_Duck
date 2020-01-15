package com.example.game0109

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.clustering.ClusterManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_map.*
//import kotlinx.android.synthetic.main.search_bar.view.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.location.LocationListener
import android.os.Debug
import android.os.IBinder


class Map : AppCompatActivity() , SensorEventListener {

    val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    val REQUEST_PERMISSION_CODE = 1
    val DEFAULT_ZOOM_LEVEL = 17f
    var googleMap: GoogleMap? = null

    var sensorManager: SensorManager? = null
    var stempDetectorSensor : Sensor? = null

    var mylat = 0.0
    var mylon = 0.0
    var cnt = 0 //보너스 포인트 저장용

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
                    //playerStatusTextView.text = myService?.getPlayerStatusTest() ?: "update fail."
                }
                else->{
                    Toast.makeText(applicationContext, "others", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    //세종대마크
    val bitmap by lazy {
        val drawable = resources.getDrawable(R.drawable.mark) as BitmapDrawable
        Bitmap.createScaledBitmap(drawable.bitmap , 64 , 64 , false)
    }

    //오리마크
    val duck by lazy{
        val drawable = resources.getDrawable(R.drawable.duck) as BitmapDrawable
        Bitmap.createScaledBitmap(drawable.bitmap, 64 , 64 , false)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        mView.onCreate(savedInstanceState)
        if(hasPermissions()){
            init()
        }
        else{
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_CODE)
        }

        //마크확인,갱신 //내위치로이동
        myLocationBt.setOnClickListener { addMarks()
            onMyLocationButtonClick()  }

        //걸음거리
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stempDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)



    }

    fun addMarks() {

        //내 위치 표시
        googleMap?.addMarker(
            MarkerOptions()
                .position(LatLng(getMyLocation().latitude,getMyLocation().longitude))
                .title("")
                .snippet("")
                .icon(BitmapDescriptorFactory.fromBitmap(duck))
        )

        //파이어베이스의 6개 포인트를 읽어옴
            var ref1 = FirebaseDatabase.getInstance().getReference("/bp/1").addValueEventListener( object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError?) {
                    Log.d("monster","읽기실패")
                }

                override fun onDataChange(p0: DataSnapshot?) {
                    p0?.let {
                        val  tmp = it.getValue(myLat::class.java)
                        val preference by lazy { getSharedPreferences("MainActivity", Context.MODE_PRIVATE)} // 위치 갱신용 쉐어드
                        val check = preference.getBoolean("p1",false) // 중복인지 체크
                        if(!check) {
                            val tf = isIn(mylat , mylon , tmp!!.latitude , tmp!!.longitude)
                            if (tf) {
                                preference.edit().putBoolean("p1", true).apply()
                                cnt++
                            }
                        }
                        googleMap?.addMarker(
                            MarkerOptions()
                                .position(LatLng(tmp!!.latitude , tmp!!.longitude))
                                .title("")
                                .snippet("")
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                        )

                    }
                }
            })

        var ref2 = FirebaseDatabase.getInstance().getReference("/bp/2").addValueEventListener( object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                Log.d("monster","읽기실패")
            }

            override fun onDataChange(p0: DataSnapshot?) {
                p0?.let {
                    val  tmp = it.getValue(myLat::class.java)
                    val preference by lazy { getSharedPreferences("MainActivity", Context.MODE_PRIVATE)} // 위치 갱신용 쉐어드
                    val check = preference.getBoolean("p2",false) // 중복인지 체크
                    if(!check) {
                        val tf = isIn(mylat , mylon , tmp!!.latitude , tmp!!.longitude)
                        if (tf) {
                            preference.edit().putBoolean("p2", true).apply()
                            cnt++
                        }
                    }
                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(LatLng(tmp!!.latitude , tmp!!.longitude))
                            .title("")
                            .snippet("")
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    )

                }
            }
        })

        var ref3 = FirebaseDatabase.getInstance().getReference("/bp/3").addValueEventListener( object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                Log.d("monster","읽기실패")
            }

            override fun onDataChange(p0: DataSnapshot?) {
                p0?.let {
                    val  tmp = it.getValue(myLat::class.java)
                    val preference by lazy { getSharedPreferences("MainActivity", Context.MODE_PRIVATE)} // 위치 갱신용 쉐어드
                    val check = preference.getBoolean("p3",false) // 중복인지 체크
                    if(!check) {
                        val tf = isIn(mylat , mylon , tmp!!.latitude , tmp!!.longitude)
                        if (tf) {
                            preference.edit().putBoolean("p3", true).apply()
                            cnt++
                        }
                    }

                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(LatLng(tmp!!.latitude , tmp!!.longitude))
                            .title("")
                            .snippet("")
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    )

                }
            }
        })

        var ref4 = FirebaseDatabase.getInstance().getReference("/bp/4").addValueEventListener( object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                Log.d("monster","읽기실패")
            }

            override fun onDataChange(p0: DataSnapshot?) {
                p0?.let {
                    val  tmp = it.getValue(myLat::class.java)
                    val preference by lazy { getSharedPreferences("MainActivity", Context.MODE_PRIVATE)} // 위치 갱신용 쉐어드
                    val check = preference.getBoolean("p4",false) // 중복인지 체크
                    if(!check) {
                        val tf = isIn(mylat , mylon , tmp!!.latitude , tmp!!.longitude)
                        if (tf) {
                            preference.edit().putBoolean("p4", true).apply()
                            cnt++
                        }
                    }

                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(LatLng(tmp!!.latitude , tmp!!.longitude))
                            .title("")
                            .snippet("")
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    )

                }
            }
        })


        var ref5 = FirebaseDatabase.getInstance().getReference("/bp/5").addValueEventListener( object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                Log.d("monster","읽기실패")
            }

            override fun onDataChange(p0: DataSnapshot?) {
                p0?.let {
                    val  tmp = it.getValue(myLat::class.java)
                    val preference by lazy { getSharedPreferences("MainActivity", Context.MODE_PRIVATE)} // 위치 갱신용 쉐어드
                    val check = preference.getBoolean("p5",false) // 중복인지 체크
                    if(!check) {
                        val tf = isIn(mylat , mylon , tmp!!.latitude , tmp!!.longitude)
                        if (tf) {
                            preference.edit().putBoolean("p5", true).apply()
                            cnt++
                        }
                    }

                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(LatLng(tmp!!.latitude , tmp!!.longitude))
                            .title("")
                            .snippet("")
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    )

                }
            }
        })

        var ref6 = FirebaseDatabase.getInstance().getReference("/bp/6").addValueEventListener( object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                Log.d("monster","읽기실패")
            }

            override fun onDataChange(p0: DataSnapshot?) {
                p0?.let {
                    val  tmp = it.getValue(myLat::class.java)
                    val preference by lazy { getSharedPreferences("MainActivity", Context.MODE_PRIVATE)} // 위치 갱신용 쉐어드
                    val check = preference.getBoolean("p6",false) // 중복인지 체크
                    if(!check) {
                        val tf = isIn(mylat , mylon , tmp!!.latitude , tmp!!.longitude)
                        if (tf) {
                            preference.edit().putBoolean("p6", true).apply()
                            cnt++
                        }
                    }

                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(LatLng(tmp!!.latitude , tmp!!.longitude))
                            .title("")
                            .snippet("")
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    )

                }
            }
        })

        if(cnt > 0){
            cnt = 0
            Toast.makeText(this , "근처에있음 보너스 획득${cnt}",Toast.LENGTH_LONG).show()
            myService?.gameManager?.increaseWeight()
            //!! 보너스 처리 필요 !!
        }
        else {
            Toast.makeText(this , "근처에없음",Toast.LENGTH_LONG).show()
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        init()
    }

    @SuppressLint("MissingPermission")
    fun init(){
        mView.getMapAsync{

            googleMap = it
            it.uiSettings.isMyLocationButtonEnabled =false

            when{
                hasPermissions() -> {
                    it.isMyLocationEnabled = true
                    it.moveCamera(CameraUpdateFactory.newLatLngZoom(getMyLocation(), DEFAULT_ZOOM_LEVEL))
                }
                else -> {
                   ;
                }
            }

        }
    }

    @SuppressLint("MissingPermission")
    fun getMyLocation(): LatLng{

        Log.d("monster","point 확인시작")
        val locationProvider: String = LocationManager.NETWORK_PROVIDER // GPS센서
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager // 위치 서비스 객체
        val gps = GPSListener()
            val mindist = 0.0
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER , 1000, mindist.toFloat() , gps)
        Log.d("monster", "{${locationManager.getLastKnownLocation(locationProvider)}")
        val lastLocation: Location = locationManager.getLastKnownLocation(locationProvider)
        Log.d("monster","point 확인완료")
        mylat = lastLocation.latitude
        mylon = lastLocation.longitude
        return LatLng(lastLocation.latitude , lastLocation.longitude)
    }

    class GPSListener : LocationListener{
        override fun onLocationChanged(location: Location?){
        }

        override fun onProviderDisabled(provider: String?) {

        }

        override fun onProviderEnabled(provider: String?) {

        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }
    }


    fun onMyLocationButtonClick(){
        when{
            hasPermissions() -> {
                var point = getMyLocation()
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(point, DEFAULT_ZOOM_LEVEL))
            }
            else -> Toast.makeText(applicationContext, "권한 없음 동의 필요", Toast.LENGTH_LONG).show()
        }
    }

    fun hasPermissions(): Boolean{
        for(permissions in PERMISSIONS){
            if(ActivityCompat.checkSelfPermission(this, permissions) != PackageManager.PERMISSION_GRANTED){
                return false
            }
        }
        return true
    }

    //맵뷰 라이프사이클
    override fun onResume() {
        super.onResume()
        mView.onResume()
        sensorManager?.registerListener(this, stempDetectorSensor , SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mView.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event!!.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            //!! 카운트 갱신처리 필요 !!
            if(event!!.values[0] > 20){
                //Toast.makeText(this , "${event!!.values[0]}점수처리필요",Toast.LENGTH_LONG).show()
                myService?.gameManager?.increaseHappy()
                myService?.gameManager?.increaseHappy()
                myService?.gameManager?.increaseHappy()
            }
            walkCountText.setText(" step  ${event!!.values[0].toString()}")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onDestroy() {
        super.onDestroy()
        mView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mView.onLowMemory()
    }

    //거리 구하는 함수
    fun deg2rad(deg : Double) : Double {
		return (deg * Math.PI / 180.0);
	}
    fun rad2deg(rad : Double) : Double {
        return (rad * 180 / Math.PI);
    }
    fun isIn(lat1 : Double , lon1 : Double , lat2 : Double , lon2 : Double) : Boolean{
        var theta = lon1 - lon2
        var dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        dist = dist * 1609.344;
        Log.d("monster" , "거리 ${dist}")

        //몇 미터 이하를 처리할지 dist < 범위
        if(dist < 100)
            return true
        else
            return false

    }


}
