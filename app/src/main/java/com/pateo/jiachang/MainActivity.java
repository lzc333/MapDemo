package com.pateo.jiachang;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.AMap.InfoWindowAdapter;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiSearch.OnPoiSearchListener;

import android.view.View.OnClickListener;

import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.SuggestionCity;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.pateo.jiachang.overlay.PoiOverlay;
import com.pateo.jiachang.util.Constants;
import com.pateo.jiachang.util.GeoCoderUtil;
import com.pateo.jiachang.util.LatLngEntity;
import com.pateo.jiachang.util.ToastUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.List;

import io.reactivex.functions.Consumer;

public class MainActivity extends Activity implements
        OnMarkerClickListener, InfoWindowAdapter,
        OnPoiSearchListener, OnClickListener ,AMapLocationListener , AMap.OnCameraChangeListener {
    private static final String TAG = "MainActivity";
    private static final int DEFAULT_ZOOM = 14;
    private AMap mAMap;
    private String mKeyWords = "";// 要输入的poi搜索关键字
    private ProgressDialog progDialog = null;// 搜索时进度条

    private PoiResult poiResult; // poi返回的结果
    private int currentPage = 1;
    private PoiSearch.Query query;// Poi查询条件类
    private PoiSearch poiSearch;// POI搜索
    private TextView mKeywordsTextView;
    private Marker mPoiMarker;
    private ImageView mCleanKeyWords;
    //声明AMapLocationClient类对象
    private AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    public static final int REQUEST_CODE = 100;
    public static final int RESULT_CODE_INPUTTIPS = 101;
    public static final int RESULT_CODE_KEYWORDS = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate ");
        setContentView(R.layout.activity_main);
        mCleanKeyWords = (ImageView)findViewById(R.id.clean_keywords);
        mCleanKeyWords.setOnClickListener(this);
        mKeyWords = "";

        MapView mMapVIew = (MapView)findViewById(R.id.map);
        mMapVIew.onCreate(savedInstanceState);
        mAMap = mMapVIew.getMap();
        setUpMap();

        mKeywordsTextView = (TextView) findViewById(R.id.main_keywords);
        mKeywordsTextView.setOnClickListener(this);
        requestLocationPermission();
    }


    /**
     * 设置页面监听
     */
    private void setUpMap() {
        mAMap.setOnMarkerClickListener(this);// 添加点击marker监听事件
        mAMap.setInfoWindowAdapter(this);// 添加显示infowindow监听事件
        mAMap.getUiSettings().setRotateGesturesEnabled(false);
        mAMap.setOnCameraChangeListener(this);
    }



    private void requestLocationPermission() {
        RxPermissions rxPermissions=new RxPermissions(this);
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                Log.d(TAG,"requestLocationPermission  result = "+aBoolean);
                if(aBoolean){
                    setLocation();
                }
            }
        });

    }

    /**
     * 设置定位
     */
    private void setLocation(){
        //定位
        MyLocationStyle myLocationStyle;
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        //myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        mAMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        //aMap.getUiSettings().setMyLocationButtonEnabled(true);设置默认定位按钮是否显示，非必需设置。
        mAMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。


        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //初始化定位
        mLocationClient = new AMapLocationClient(this);
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //给定位客户端对象设置定位参数
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
        //获取一次定位结果：
        mLocationOption.setOnceLocation(true);
        //获取最近3s内精度最高的一次定位结果：
        // mLocationOption.setOnceLocationLatest(true);
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        Log.d(TAG,"showProgressDialog ");
        if (progDialog == null)
            progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(false);
        progDialog.setMessage("正在搜索:\n" + mKeyWords);
        progDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        Log.d(TAG,"dissmissProgressDialog ");
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }

    /**
     * 开始进行poi搜索
     */
    protected void doSearchQuery(String keywords) {
        Log.d(TAG,"doSearchQuery keywords = "+keywords);
        showProgressDialog();// 显示进度框
        currentPage = 1;
        // 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        query = new PoiSearch.Query(keywords, "", "");
        // 设置每页最多返回多少条poiitem
        query.setPageSize(10);
        // 设置查第一页
        query.setPageNum(currentPage);

        poiSearch = new PoiSearch(this, query);
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.searchPOIAsyn();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return false;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @Override
    public View getInfoWindow(final Marker marker) {
        View view = getLayoutInflater().inflate(R.layout.poikeywordsearch_uri,
                null);
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(marker.getTitle());

        TextView snippet = (TextView) view.findViewById(R.id.snippet);
        snippet.setText(marker.getSnippet());
        return view;
    }

    /**
     * poi没有搜索到数据，返回一些推荐城市的信息
     */
    private void showSuggestCity(List<SuggestionCity> cities) {
        Log.d(TAG,"showSuggestCity");
        String infomation = "推荐城市\n";
        for (int i = 0; i < cities.size(); i++) {
            infomation += "城市名称:" + cities.get(i).getCityName() + "城市区号:"
                    + cities.get(i).getCityCode() + "城市编码:"
                    + cities.get(i).getAdCode() + "\n";
        }
        Log.d(TAG,"showSuggestCity infomation = "+infomation);
        ToastUtil.show(this, infomation);
    }


    /**
     * POI信息查询回调方法
     */
    @Override
    public void onPoiSearched(PoiResult result, int rCode) {
        Log.d(TAG,"onPoiSearched");
        dissmissProgressDialog();// 隐藏对话框
        if (rCode == 1000) {
            if (result != null && result.getQuery() != null) {// 搜索poi的结果
                if (result.getQuery().equals(query)) {// 是否是同一条
                    poiResult = result;
                    // 取得搜索到的poiitems有多少页
                    List<PoiItem> poiItems = poiResult.getPois();// 取得第一页的poiitem数据，页数从数字0开始
                    List<SuggestionCity> suggestionCities = poiResult
                            .getSearchSuggestionCitys();// 当搜索不到poiitem数据时，会返回含有搜索关键字的城市信息

                    if (poiItems != null && poiItems.size() > 0) {
                        mAMap.clear();// 清理之前的图标
                        PoiOverlay poiOverlay = new PoiOverlay(mAMap, poiItems);
                        poiOverlay.removeFromMap();
                        poiOverlay.addToMap();
                        poiOverlay.zoomToSpan();
                    } else if (suggestionCities != null
                            && suggestionCities.size() > 0) {
                        showSuggestCity(suggestionCities);
                    } else {
                        Log.d(TAG,"onPoiSearched no_result 1");
                        ToastUtil.show(this, R.string.no_result);
                    }
                }
            } else {
                Log.d(TAG,"onPoiSearched no_result 2");
                ToastUtil.show(this, R.string.no_result);
            }
        } else {
            Log.d(TAG,"onPoiSearched rCode = "+rCode);
            ToastUtil.showerror(this, rCode);
        }

    }

    @Override
    public void onPoiItemSearched(PoiItem item, int rCode) {
        // TODO Auto-generated method stub

    }

    /**
     * 输入提示activity选择结果后的处理逻辑
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CODE_INPUTTIPS && data
                != null) {
            Log.d(TAG,"onActivityResult resultCode == RESULT_CODE_INPUTTIPS ");
            mAMap.clear();
            Tip tip = data.getParcelableExtra(Constants.EXTRA_TIP);
            if (tip.getPoiID() == null || tip.getPoiID().equals("")) {
                doSearchQuery(tip.getName());
            } else {
                addTipMarker(tip);
            }
            mKeywordsTextView.setText(tip.getName());
            if(!tip.getName().equals("")){
                mCleanKeyWords.setVisibility(View.VISIBLE);
            }
        } else if (resultCode == RESULT_CODE_KEYWORDS && data != null) {
            Log.d(TAG,"onActivityResult resultCode == RESULT_CODE_KEYWORDS ");
            mAMap.clear();
            String keywords = data.getStringExtra(Constants.KEY_WORDS_NAME);
            if(keywords != null && !keywords.equals("")){
                doSearchQuery(keywords);
            }
            mKeywordsTextView.setText(keywords);
            if(!keywords.equals("")){
                mCleanKeyWords.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 用marker展示输入提示list选中数据
     *
     * @param tip
     */
    private void addTipMarker(Tip tip) {
        Log.d(TAG,"addTipMarker ");
        if (tip == null) {
            return;
        }
        //tip.getAdcode = 320114 , tip.getAddress = 玉兰路98号 , tip.getDistrict = 江苏省南京市雨花台区 , tip.getName = 南京南站 , tip.getPoiID = B00190YPLY
        Log.d(TAG,"addTipMarker tip.getAdcode = "+tip.getAdcode()+" , tip.getAddress = "+tip.getAddress() +" , tip.getDistrict = "+tip.getDistrict()
        + " , tip.getName = "+tip.getName()+ " , tip.getPoiID = "+tip.getPoiID());
        mPoiMarker = mAMap.addMarker(new MarkerOptions());
        LatLonPoint point = tip.getPoint();
        if (point != null) {
            LatLng markerPosition = new LatLng(point.getLatitude(), point.getLongitude());
            mPoiMarker.setPosition(markerPosition);
            mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerPosition, DEFAULT_ZOOM));
        }
        mPoiMarker.setTitle(tip.getName());
        mPoiMarker.setSnippet(tip.getAddress());
    }

    /**
     * 点击事件回调方法
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_keywords:
                Intent intent = new Intent(this, InputTipsActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
                break;
            case R.id.clean_keywords:
                mKeywordsTextView.setText("");
                mAMap.clear();
                mCleanKeyWords.setVisibility(View.GONE);
            default:
                break;
        }
    }


    /**
     * amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
     * amapLocation.getLatitude();//获取纬度
     * amapLocation.getLongitude();//获取经度
     * amapLocation.getAccuracy();//获取精度信息
     * amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
     * amapLocation.getCountry();//国家信息
     * amapLocation.getProvince();//省信息
     * amapLocation.getCity();//城市信息
     * amapLocation.getDistrict();//城区信息
     * amapLocation.getStreet();//街道信息
     * amapLocation.getStreetNum();//街道门牌号信息
     * amapLocation.getCityCode();//城市编码
     * amapLocation.getAdCode();//地区编码
     * amapLocation.getAoiName();//获取当前定位点的AOI信息
     * amapLocation.getBuildingId();//获取当前室内定位的建筑物Id
     * amapLocation.getFloor();//获取当前室内定位的楼层
     * amapLocation.getGpsAccuracyStatus();//获取GPS的当前状态
     * //获取定位时间
     * SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
     * Date date = new Date(amapLocation.getTime());
     * df.format(date);
     * */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        Log.d(TAG,"onLocationChanged aMapLocation = "+aMapLocation.toString());
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                LatLng markerPosition = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerPosition, DEFAULT_ZOOM));
                //Constants.DEFAULT_CITY = aMapLocation.getCity();
            }else {
                Log.e("onLocationChanged"," Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }


    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
        if(mLocationClient!=null){
            mLocationClient.onDestroy();//销毁定位客户端，同时销毁本地定位服务。
        }
        super.onDestroy();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        //Log.d(TAG,"onCameraChange");
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        Log.d(TAG,"onCameraChangeFinish cameraPosition = "+cameraPosition.toString());
        LatLngEntity latLngEntity = new LatLngEntity(cameraPosition.target.latitude, cameraPosition.target.longitude);
        //地理反编码工具类，代码在后面
        GeoCoderUtil.getInstance(this).geoAddress(latLngEntity, new GeoCoderUtil.GeoCoderAddressListener() {
            @Override
            public void onAddressResult(String result) {
                Log.d(TAG,"onCameraChangeFinish result = "+result);
                /*if(!autotext.getText().toString().trim().equals("")){
                    //输入地址后的点击搜索
                    tvAddressDesc.setText(autotext.getText().toString().trim());
                    currentLoc = new LocationBean(cameraPosition.target.longitude,cameraPosition.target.latitude,autotext.getText().toString().trim(),"");
                }else{
                    //拖动地图
                    tvAddressDesc.setText(result);
                    currentLoc = new LocationBean(cameraPosition.target.longitude,cameraPosition.target.latitude,result,"");
                }*/
            }
        });
    }
}
