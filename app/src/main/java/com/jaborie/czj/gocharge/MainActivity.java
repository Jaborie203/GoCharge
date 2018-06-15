package com.jaborie.czj.gocharge;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.PopupWindowCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.NavigateArrow;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.AmapNaviTheme;
import com.amap.api.navi.AmapNaviType;
import com.amap.api.navi.AmapPageType;
import com.amap.api.navi.INaviInfoCallback;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.Path;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.jaborie.czj.gocharge.adapter.DrivePathAdapter;
import com.jaborie.czj.gocharge.behavior.NoAnchorBottomSheetBehavior;
import com.jaborie.czj.gocharge.overlay.AMapServicesUtil;
import com.jaborie.czj.gocharge.overlay.AMapUtil;
import com.jaborie.czj.gocharge.overlay.DrivingRouteOverlay;
import com.jaborie.czj.gocharge.pickpoi.PoiItemEvent;
import com.jaborie.czj.gocharge.util.CheckPermissionsActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends CheckPermissionsActivity implements LocationSource, AMapLocationListener, RouteSearch.OnRouteSearchListener, INaviInfoCallback{
    MapView mMapView = null;
    AMap aMap;
    UiSettings uiSettings;
    //定位需要的数据
    LocationSource.OnLocationChangedListener mListener;
    AMapLocationClient mlocationClient;
    AMapLocationClientOption mLocationOption;
    //定位蓝点
    MyLocationStyle myLocationStyle;
    //SeekBar相关定义
    private SeekBar seekBar;
    private TextView txt_hint;
    private Context mContext;
    public static final String CITY_CODE="CityCode";
    public static MainActivity stopMain;
    //用于二次滑动确认返回
    private static boolean isExit = false;
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            isExit = false;
        }
    };
    @BindView(R.id.route_plan_loca_btn) ImageView mImageViewBtn;
    @BindView(R.id.coordinatorlayout) CoordinatorLayout mCoordinatorLayout;


    //@BindView(R.id.route_plan_float_btn)FloatingActionButton mFloatBtn;
    @BindView(R.id.sheet_head_layout)LinearLayout mSheetHeadLayout;
    @BindView(R.id.route_plan_poi_title) TextView mPoiTitleText;
    @BindView(R.id.bottom_sheet)
    NestedScrollView mNesteScrollView;
    @BindView(R.id.route_plan_poi_desc) TextView mPoiDescText;
    @BindView(R.id.route_plan_poi_detail_layout) LinearLayout mPoiDetailLayout;
    @BindView(R.id.path_detail_recyclerView)
    RecyclerView mPathDetailRecView;
    @BindView(R.id.path_detail_traffic_light_text) TextView mPathTipsText;
    @BindView(R.id.navi_start_btn_1) TextView mNaviText;
    @BindView(R.id.navi_start_btn)
    Button mNaviBtn;

    @BindView(R.id.path_layout)LinearLayout mPathLayout;
    @BindView(R.id.path_layout1)LinearLayout mPathLayout1;
    @BindView(R.id.path_layout2)LinearLayout mPathLayout2;
    @BindView(R.id.path_general_time)TextView mPathDurText;
    @BindView(R.id.path_general_time1)TextView mPathDurText1;
    @BindView(R.id.path_general_time2)TextView mPathDurText2;
    @BindView(R.id.path_general_distance)TextView mPathDisText;
    @BindView(R.id.path_general_distance1)TextView mPathDisText1;
    @BindView(R.id.path_general_distance2)TextView mPathDisText2;
    @BindView(R.id.result_holder) LinearLayout mResultholder;
    @BindView(R.id.filt) ImageView filtBtn;
    @BindView(R.id.percent) TextView mPercent;
    private LinearLayout avoidLinear;
    private LinearLayout freeLinear;
    private LinearLayout distanceLinear;

    private NoAnchorBottomSheetBehavior mBehavior;
    private DrivePathAdapter mDrivePathAdapter;
    private static final int MSG_MOVE_CAMERA = 0x01;
    private final int TYPE_DRIVE=100;
    private int mSelectedType =TYPE_DRIVE;

    private int mTopLayoutHeight=200;

    private float mDegree=0f;
    private SensorManager mSensorManager;

    private boolean FirstLocate =true;
    private Poi mEndPoi;
    private Poi mStartPoi;
    private DriveRouteResult mDriveRouteResult;
    TextToSpeech tts;

    PopupWindow mPop;
    PopupWindow mPop1;
    LatLng p1 = new LatLng(31.287648, 121.212623);//同济大学电信楼
    LatLng p2 = new LatLng(31.281183, 121.21144);//同嘉科技广场

    Poi start = new Poi("济大学电信楼", p1, "");//起点
    Poi end = new Poi("同嘉科技广场", p2, "");//终点

    int routeSearchStratesy = RouteSearch.DRIVING_MULTI_STRATEGY_FASTEST_SHORTEST;
    int progress1 = 100;
    private BatteryView horizontalBattery;
    boolean flag_toBreak = false;
    TextView mOk,mCancel;
    //RadioGroup radioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        horizontalBattery = (BatteryView) findViewById(R.id.horizontalBattery);
        //获取地图控件引用
        mMapView = (MapView)findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        initMap();
        /****全屏显示部分****/
        Window window = getWindow();
        //隐藏标题栏
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏状态栏
        //定义全屏参数
        int flag= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        //设置当前窗体为全屏显示
        window.setFlags(flag, flag);

        tts=new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if(status == TextToSpeech.SUCCESS){
                    int result=tts.setLanguage(Locale.CHINA);
                    if(result==TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("error", "This Language is not supported");
                    }
                    else{
                        Log.e("result of tts：", "The Language is Supported");
                    }
                }
                else
                    Log.e("error", "Initilization Failed!");
            }
        });

        bindViews();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Handler handler=new Handler(Looper.getMainLooper());//加上getMainlooper()，相当于handler绑定了主线程
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        initSensor();
                        initSheet();
                    }
                });
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                /*try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/

                try {
                    InputStream is = getAssets().open("simulator_battery");

                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(is);

                    Element element=doc.getDocumentElement();
                    element.normalize();

                    NodeList nList = doc.getElementsByTagName("battery");
                    Log.e("seekbar","test1");

                    Handler handler=new Handler(Looper.getMainLooper());//加上getMainlooper()，相当于handler绑定了主线程
                    for (int i=0; i<nList.getLength(); i++) {
                        Log.e("seekbar","test2");
                        Node node = nList.item(i);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            Element element2 = (Element) node;
                            String str1;
                            NodeList nodeList = element2.getElementsByTagName("num").item(0).getChildNodes();
                            Node node1 = nodeList.item(0);
                            str1 = node1.getNodeValue();
                            progress1=Integer.parseInt(str1);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                //seekBar.setProgress(progress);
                                horizontalBattery.setPower(progress1);
                                mPercent.setText(progress1 + "%");
                                if(progress1 <= 30) mPercent.setTextColor(Color.RED);
                                if(progress1 <= 20 && flag_toBreak == false){
                                    //showDialog();
                                    openBottomSheet();
                                    tts.speak("电池电量低", TextToSpeech.QUEUE_ADD, null);
                                    flag_toBreak = true;
                                    Log.e("seekbar","test3");
                                }
                            }
                        });
                        if(flag_toBreak == true) break;
                    }

                } catch (Exception e) {e.printStackTrace();}



            }
        }).start();
    }
    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
    }
    private void initMap()
    {
        if(aMap == null) {
            aMap = mMapView.getMap();
        }
        /****定位及UI相关****/
        uiSettings = aMap.getUiSettings();
        CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(31.285951,121.215242),15,0,0));
        aMap.animateCamera(mCameraUpdate);

        aMap.setTrafficEnabled(true);//设置是否显示拥堵状况
        //设置地图的放缩级别
        //aMap.moveCamera(CameraUpdateFactory.zoomTo(12));
        // 设置定位监听
        aMap.setLocationSource(this);
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);
        //aMap.showIndoorMap(true);//是否显示室内地图
        // 设置定位的类型为定位模式，有定位、跟随或地图根据面向方向旋转几种
        //aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
        //蓝点初始化
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）默认执行此种模式。
        myLocationStyle.showMyLocation(true);
        myLocationStyle.radiusFillColor(000000);//设置圆形区域（以定位位置为圆心，定位半径的圆形区域）的填充颜色
        myLocationStyle.strokeColor(000000);//设置圆形区域（以定位位置为圆心，定位半径的圆形区域）的边框颜色
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        uiSettings.setMyLocationButtonEnabled(false);//设置默认定位按钮是否显示，非必需设置。
        uiSettings.setCompassEnabled(false);//设置罗盘
        uiSettings.setZoomControlsEnabled(true);//设置缩放按钮是否可见
        //uiSettings.setScaleControlsEnabled(true);//设置比例尺
        uiSettings.setZoomPosition(AMapOptions.ZOOM_POSITION_RIGHT_CENTER);//设置缩放按钮位置
        uiSettings.setLogoBottomMargin(-40);//其实是为了隐藏高德地图logo
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。


        aMap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                //从location对象中获取经纬度信息，地址描述信息，建议拿到位置之后调用逆地理编码接口获取
                /*if (FirstLocate){
                    FirstLocate =false;
                    LocaBtnOnclick();
                    mStartPoi=new Poi(getString(R.string.poi_search_my_location),
                            new LatLng(location.getLatitude(),location.getLongitude()),"");
                }*/
                mStartPoi=new Poi(getString(R.string.poi_search_my_location),
                        new LatLng(location.getLatitude(),location.getLongitude()),"");
            }
        });
    }
    private void bindViews(){
        seekBar = (SeekBar)findViewById(R.id.seekBar);
        txt_hint = (TextView)findViewById(R.id.txt_hint);
        seekBar.setProgress(100);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt_hint.setText(progress+"/100 ");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(MainActivity.this, "开始模拟电量变化", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(MainActivity.this, "停止模拟电量变化", Toast.LENGTH_SHORT).show();
                if(seekBar.getProgress() <= 30) {
                    showDialog();
                    tts.speak("电池电量低", TextToSpeech.QUEUE_ADD, null);
                }

            }
        });
    }
    private void openBottomSheet(){
        View v3 = getLayoutInflater().inflate(R.layout.layout_bottom_sheet,null);
        mOk = (TextView)v3.findViewById(R.id.ok);
        mCancel = (TextView)v3.findViewById(R.id.cancel);
        mOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location=aMap.getMyLocation();
                PoiAroundSearchActivity.start(mContext, location.getLatitude(),location.getLongitude(),PoiAroundSearchActivity.FROM_TARGET);
                mPop1.dismiss();
            }
        });
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPop1.dismiss();
            }
        });
        mPop1 = new PopupWindow(v3, 1020,ViewGroup.LayoutParams.WRAP_CONTENT);
        mPop1.setAnimationStyle(R.style.Animation);
        //ColorDrawable dw = new ColorDrawable(0xb0000000);
        //设置SelectPicPopupWindow弹出窗体的背景
        //mPop1.setBackgroundDrawable(dw);
        //mPop1.setOutsideTouchable(false);
        //mPop1.setFocusable(false);
        mPop1.showAtLocation(mMapView, Gravity.BOTTOM,0,30);
    }
    /**
     * 这是兼容的 AlertDialog
     */
    private void showDialog() {
    /*
    这里使用了 android.support.v7.app.AlertDialog.Builder
    可以直接在头部写 import android.support.v7.app.AlertDialog
    那么下面就可以写成 AlertDialog.Builder
     */
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("请注意！！！");
        builder.setMessage("电池电量低\n请前往附近的充电站对车辆进行充电");
        builder.setIcon(R.drawable.emergency_charge);
        builder.setNegativeButton("取消", null);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Toast.makeText(MainActivity.this, "请选择要去的充电站", Toast.LENGTH_SHORT).show();
                Location location=aMap.getMyLocation();
                PoiAroundSearchActivity.start(mContext, location.getLatitude(),location.getLongitude(),PoiAroundSearchActivity.FROM_TARGET);
            }
        });
        builder.show();
    }
    private void initSheet(){
        mBehavior = NoAnchorBottomSheetBehavior.from(mNesteScrollView);
        mBehavior.setState(NoAnchorBottomSheetBehavior.STATE_COLLAPSED);
        mBehavior.setPeekHeight(getSheetHeadHeight());
        mBehavior.setBottomSheetCallback(new NoAnchorBottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (mPathDetailRecView.getVisibility()==View.VISIBLE){
                    if (slideOffset>0.5){
                        mNaviText.setVisibility(View.GONE);
                        mNaviBtn.setVisibility(View.VISIBLE);
                    }else {
                        mNaviText.setVisibility(View.VISIBLE);
                        mNaviBtn.setVisibility(View.GONE);
                    }
                }
            }
        });
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mPathDetailRecView.setLayoutManager(linearLayoutManager);
    }
    private SensorEventListener mSensorListner=new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float degree = event.values[0];
            mDegree=degree;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void initSensor(){
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListner,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);//http://www.cnblogs.com/plokmju/p/android_SensorManager.html（相关介绍）
    }

    @OnClick(R.id.filt)
    public void FiltBtnClick(){
        View v = getLayoutInflater().inflate(R.layout.layout_pop,null);
        avoidLinear = (LinearLayout)v.findViewById(R.id.strategy_avoid);
        avoidLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                routeSearchStratesy = RouteSearch.DRIVING_MULTI_CHOICE_AVOID_CONGESTION;
                routeSearch(mStartPoi,mEndPoi,mSelectedType);
                tts.speak("正在为您重新规划路线", TextToSpeech.QUEUE_ADD, null);
                mPop.dismiss();
            }
        });
        freeLinear = (LinearLayout)v.findViewById(R.id.strategy_free);
        freeLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                routeSearchStratesy = RouteSearch.DRIVING_MULTI_CHOICE_SAVE_MONEY;
                routeSearch(mStartPoi,mEndPoi,mSelectedType);
                tts.speak("正在为您重新规划路线", TextToSpeech.QUEUE_ADD, null);
                mPop.dismiss();
            }
        });
        distanceLinear = (LinearLayout)v.findViewById(R.id.strategy_distance);
        distanceLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                routeSearchStratesy = RouteSearch.DRIVING_MULTI_STRATEGY_FASTEST_SHORTEST;
                routeSearch(mStartPoi,mEndPoi,mSelectedType);
                tts.speak("正在为您重新规划路线", TextToSpeech.QUEUE_ADD, null);
                mPop.dismiss();
            }
        });
        mPop = new PopupWindow(v, ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        mPop.setOutsideTouchable(true);
        mPop.setFocusable(true);
        mPop.showAtLocation(filtBtn,0,30,30);
        //mPop.showAsDropDown(filtBtn);
    }
    private final int BTN_STATE_NOR=100;
    private final int BTN_STATE_LOCATE=101;
    private final int BTN_STATE_DIRE=102;

    private int mBtnState=BTN_STATE_NOR;
    @OnClick(R.id.route_plan_loca_btn)
    public void LocaBtnOnclick(){
        if (mBtnState==BTN_STATE_NOR){
            aMap.setMapType(AMap.MAP_TYPE_NORMAL);//普通地图模式常量
            changeMapLevelAndAngle(16,0);
            mBtnState=BTN_STATE_LOCATE;
            LocateBtnUIChagen();
        }else if (mBtnState==BTN_STATE_LOCATE){
            aMap.setMapType(AMap.MAP_TYPE_NORMAL);
            changeMapLevelAndAngle(18,60);
            mBtnState=BTN_STATE_DIRE;
            LocateBtnUIChagen();
        }else if (mBtnState==BTN_STATE_DIRE){
            aMap.setMapType(AMap.MAP_TYPE_NORMAL);
            changeMapLevelAndAngle(16,0);
            mBtnState=BTN_STATE_NOR;
            LocateBtnUIChagen();
        }
    }
    private void changeMapLevelAndAngle(final int lv, final int angle){
        CameraUpdate mCameraUpdate= CameraUpdateFactory.newLatLngZoom(
                new LatLng(aMap.getMyLocation().getLatitude(),aMap.getMyLocation().getLongitude())
                ,lv);
        aMap.animateCamera(mCameraUpdate, new AMap.CancelableCallback() {
            @Override
            public void onFinish() {
                aMap.animateCamera(CameraUpdateFactory.changeTilt(angle), new AMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        if (lv>17){
                            CameraUpdate cameraUpdate= CameraUpdateFactory.changeBearing(mDegree);
                            aMap.animateCamera(cameraUpdate);
                        }else{
                            CameraUpdate cameraUpdate= CameraUpdateFactory.changeBearing(0);
                            aMap.animateCamera(cameraUpdate);
                        }

                    }
                    @Override
                    public void onCancel() {}
                });
            }
            @Override
            public void onCancel() {}
        });
    }

    /**
     * 选点返回处理
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void selectPoiEvent(PoiItemEvent event){
        PoiItem item=event.getItem();
        if(item != null) Toast.makeText(MainActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
        LatLonPoint point=item.getLatLonPoint();
        LatLng latLng=new LatLng(point.getLatitude(),point.getLongitude());
        if (event.getFrom()== PoiAroundSearchActivity.FROM_START){
            mStartPoi=new Poi(item.getTitle(),latLng,item.getAdName());
        }else if (event.getFrom()==PoiAroundSearchActivity.FROM_TARGET){
            mEndPoi=new Poi(item.getTitle(),latLng,item.getAdName());
        }
//        goToPlaceAndMark(item);

        mPoiTitleText.setText(item.getTitle());
        mPoiDescText.setText(item.getAdName()+"    "+item.getSnippet());

        if (mStartPoi==null || mEndPoi==null){
            return;
        }

        routeSearch(mStartPoi,mEndPoi,mSelectedType);
    }
    private void LocateBtnUIChagen(){
        if (mBtnState==BTN_STATE_NOR){
            mImageViewBtn.setImageResource(R.drawable.icon_c34);
        }else if (mBtnState==BTN_STATE_LOCATE){
            mImageViewBtn.setImageResource(R.drawable.icon_c34_b);
        }else if (mBtnState==BTN_STATE_DIRE){
            mImageViewBtn.setImageResource(R.drawable.icon_c34_a);
        }
    }
    @OnClick({R.id.navi_start_btn,R.id.navi_start_btn_1, R.id.path_layout,R.id.path_layout1,
            R.id.path_layout2
    })
    public void onViewclik(View view){
        /*if (FirstLocate){
            return;
        }*/
        switch (view.getId()){
            case R.id.navi_start_btn:
            case R.id.navi_start_btn_1:
                if (mEndPoi==null){
                    return;
                }
                //new NaviDialog().showView(mContext,mStartPoi,mEndPoi,mSelectedType);
                startNavi();
                break;
            case R.id.path_layout:
                onPathClick(0);
                //tts.speak(mDriveRouteResult.getPaths().get(0).getStrategy(),TextToSpeech.QUEUE_ADD,null);
                break;
            case R.id.path_layout1:
                onPathClick(1);
                //tts.speak(mDriveRouteResult.getPaths().get(1).getStrategy(),TextToSpeech.QUEUE_ADD,null);
                break;
            case R.id.path_layout2:
                onPathClick(2);
                //tts.speak(mDriveRouteResult.getPaths().get(2).getStrategy(),TextToSpeech.QUEUE_ADD,null);
                break;
        }
    }
    private void onPathClick(int i){
        switch (mSelectedType){
            case TYPE_DRIVE:
                mPathTipsText.setText(getString(R.string.route_plan_path_traffic_lights,mDriveRouteResult.getPaths().get(i).getTotalTrafficlights()+""));
                mPathDetailRecView.setAdapter(new DrivePathAdapter(mContext,mDriveRouteResult.getPaths().get(i).getSteps()));
                drawDriveRoutes(mDriveRouteResult,mDriveRouteResult.getPaths().get(i));
                break;
            default:
                break;
        }
    }
    /*private void updateEditUI(){

    }*/

    private void updatePathGeneral(Path path, int i){
        String dur = AMapUtil.getFriendlyTime((int) path.getDuration());
        String dis = AMapUtil.getFriendlyLength((int) path.getDistance());
        if (i==0){
            mPathDurText.setText(dur);
            mPathDisText.setText(dis);
            mPathLayout.setVisibility(View.VISIBLE);
            mPathLayout1.setVisibility(View.GONE);
            mPathLayout2.setVisibility(View.GONE);
        }else if (i==1){
            mPathDurText1.setText(dur);
            mPathDisText1.setText(dis);
            mPathLayout.setVisibility(View.VISIBLE);
            mPathLayout1.setVisibility(View.VISIBLE);
            mPathLayout2.setVisibility(View.GONE);

        }else if (i==2){
            mPathDurText2.setText(dur);
            mPathDisText2.setText(dis);
            mPathLayout.setVisibility(View.VISIBLE);
            mPathLayout1.setVisibility(View.VISIBLE);
            mPathLayout2.setVisibility(View.VISIBLE);
        }
    }


    private int getSheetHeadHeight(){
        mSheetHeadLayout.measure(0,0);
        Log.d("czh",mSheetHeadLayout.getMeasuredHeight()+"height");
        return mSheetHeadLayout.getMeasuredHeight();
    }
    private void routeSearch(Poi startPoi, Poi targetPoi, int type){
        if(startPoi==null || targetPoi==null){
            return;
        }
        LatLng start=startPoi.getCoordinate();
        LatLng target=targetPoi.getCoordinate();

        RouteSearch routeSearch=new RouteSearch(this);
        routeSearch.setRouteSearchListener(this);
        RouteSearch.FromAndTo fromAndTo=new RouteSearch.FromAndTo(AMapServicesUtil.convertToLatLonPoint(start),AMapServicesUtil.convertToLatLonPoint(target));
        switch (type){
            case TYPE_DRIVE:
                RouteSearch.DriveRouteQuery dquery=new RouteSearch.DriveRouteQuery(fromAndTo, routeSearchStratesy,null,null,"");
                //DRIVING_MULTI_CHOICE_AVOID_CONGESTION(多备选，躲避拥堵), DRIVING_MULTI_CHOICE_SAVE_MONEY(多备选，费用优先)
                //DRIVING_MULTI_STRATEGY_FASTEST_SHORTEST(多备选，时间最短，距离最短),
                routeSearch.calculateDriveRouteAsyn(dquery);
                break;
            default:
                break;
        }
    }
    /*@Override
    public void onBackPressed() {
        super.onBackPressed();
    }*/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (PoiAroundSearchActivity.isStart == false) {
                finish();
            }
            else{
                Intent intent = new Intent(MainActivity.this,PoiAroundSearchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                mNesteScrollView.setVisibility(View.GONE);
                aMap.clear();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        mMapView.onDestroy();
        mSensorManager.unregisterListener(mSensorListner);
        if(null != mlocationClient){
            mlocationClient.onDestroy();
        }
        if (tts != null) {
            tts.shutdown();
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        mMapView.onResume();
    }
    @Override
    protected void onPause(){
        super.onPause();
        mMapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }
    /**
     * 激活定位*
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            //初始化定位
            mlocationClient = new AMapLocationClient(this);
            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();//启动定位
        }
    }
    /**
     * 停止定位*
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }
    /**
     * 定位回调，在回调方法中调用mListener.onLocationChanged(amapLocation);
     * 可以在地图中显示系统小蓝点
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null&&amapLocation != null) {
            if (amapLocation != null
                    &&amapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode()+ ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr",errText);
            }
        }
    }
    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int errorCode) {
        if (errorCode==1000){
            if (driveRouteResult != null && driveRouteResult.getPaths() != null){
                if (driveRouteResult.getPaths().size() > 0){
                    updateUiAfterRouted();
                    mDriveRouteResult=driveRouteResult;

                    DrivePath path=mDriveRouteResult.getPaths().get(0);
                    mDrivePathAdapter=new DrivePathAdapter(mContext,path.getSteps());
                    mPathDetailRecView.setAdapter(mDrivePathAdapter);
                    mPathDetailRecView.setVisibility(View.VISIBLE);

                    mPathTipsText.setText(getString(R.string.route_plan_path_traffic_lights,path.getTotalTrafficlights()+""));
                    mPathTipsText.setVisibility(View.VISIBLE);

                    for (int i=0;i<mDriveRouteResult.getPaths().size();i++){
                        updatePathGeneral(mDriveRouteResult.getPaths().get(i),i);
                    }

                    mBehavior.setPeekHeight(getSheetHeadHeight());
                    //停顿1秒
                    /*try {
                        Thread.sleep(1200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                    drawDriveRoutes(mDriveRouteResult,path);


                }else if (driveRouteResult != null && driveRouteResult.getPaths() == null) {
                    Toast.makeText(mContext,R.string.no_result,Toast.LENGTH_LONG).show();
                }
            }else {
                Toast.makeText(mContext,R.string.no_result,Toast.LENGTH_LONG).show();
            }
        }else {
            Toast.makeText(mContext,R.string.poi_search_error,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int errorCode) {

    }
    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int errorCode) {

    }

    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int errorCode) {

    }
    private void updateUiAfterRouted(){
        mNaviText.setVisibility(View.VISIBLE);
        mPoiDetailLayout.setVisibility(View.GONE);
        mPathTipsText.setVisibility(View.GONE);
        mNesteScrollView.setVisibility(View.VISIBLE);
        filtBtn.setVisibility(View.VISIBLE);
        horizontalBattery.setVisibility(View.GONE);
        mPercent.setVisibility(View.GONE);
    }
    private void drawDriveRoutes(DriveRouteResult driveRouteResult, DrivePath path){
        aMap.clear();
        final DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                mContext, aMap, path,
                driveRouteResult.getStartPos(),driveRouteResult.getTargetPos(),
                null);
        drivingRouteOverlay.setNodeIconVisibility(false);//设置节点marker是否显示
        drivingRouteOverlay.setIsColorfulline(true);//是否用颜色展示交通拥堵情况，默认true
        drivingRouteOverlay.removeFromMap();
        drivingRouteOverlay.addToMap();
        drivingRouteOverlay.zoomWithPadding(0,getSheetHeadHeight());
    }

    private void startNavi(){

        AmapNaviParams amapNaviParams = new AmapNaviParams(mStartPoi, null, mEndPoi, AmapNaviType.DRIVER, AmapPageType.NAVI);
        amapNaviParams.setUseInnerVoice(true);
        amapNaviParams.setTheme(AmapNaviTheme.WHITE);
        //amapNaviParams.setNeedCalculateRouteWhenPresent(false);
        AmapNaviPage.getInstance().showRouteActivity(mContext, amapNaviParams, MainActivity.this);
        Log.d("test","hhh4");
    }
    @Override
    public void onInitNaviFailure() {

    }

    @Override
    public void onGetNavigationText(String s) {

    }

    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

    }

    @Override
    public void onArriveDestination(boolean b) {

    }

    @Override
    public void onStartNavi(int i) {

    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {

    }

    @Override
    public void onCalculateRouteFailure(int i) {

    }

    @Override
    public void onStopSpeaking() {

    }

    @Override
    public void onReCalculateRoute(int i) {

    }

    @Override
    public void onExitPage(int i) {

    }

    /*@Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        if(checkedId == R.id.strategy_avoid) {
            routeSearchStratesy = RouteSearch.DRIVING_MULTI_CHOICE_AVOID_CONGESTION;
        }else if(checkedId == R.id.strategy_free){
            routeSearchStratesy = RouteSearch.DRIVING_MULTI_CHOICE_SAVE_MONEY;
        }else{
            routeSearchStratesy = RouteSearch.DRIVING_MULTI_STRATEGY_FASTEST_SHORTEST;
        }
        routeSearch(mStartPoi,mEndPoi,mSelectedType);
        tts.speak("正在为您重新规划路线", TextToSpeech.QUEUE_ADD, null);
        mPop.dismiss();
    }*/

}
