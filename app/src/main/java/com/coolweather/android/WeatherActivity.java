package com.coolweather.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.internal.Util;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;

    private TextView titleCity,titleUpdateTime,degreeText,
            weatherInfoText,aqiText,pm25Text,comfortText,
            carWashText,sportText,qltyText;

    private LinearLayout forecastLayout;

    private ImageView bingPicImg,weatherIcon;

    public SwipeRefreshLayout swipeRefresh;

    public DrawerLayout drawerLayout;

    private Button navButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21){
            getWindow().getDecorView().
                setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);
        //初始化各种控件
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        qltyText = findViewById(R.id.qlty_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        bingPicImg = findViewById(R.id.bing_pic_img);
        weatherIcon = findViewById(R.id.weather_icon);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        drawerLayout = findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        String bingPic = prefs.getString("bing_pic",null);
        final String weatherId;
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else {
            loadBingPic();
        }
        if (weatherString != null){
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        }else {
            //无缓存时去查询服务器数据
            weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                requestWeather(weatherId);
                loadBingPic();
            }
        });
    }

    //根据天气id请求城市天气信息
    public void requestWeather(final String weatherId){
//        String weatherUrl = "https://free-api.heweather.com/s6/weather/forecast?location=" +
//                weatherId + "&key=a011b5e3b0224bd98706acb6dd5a3e32";
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" +
                weatherId + "&key=a011b5e3b0224bd98706acb6dd5a3e32";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this)
                                    .edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            //Toast.makeText(WeatherActivity.this,weatherId+"/"+weather,Toast.LENGTH_SHORT).show();
                            showWeatherInfo(weather);

                        }else {
                            Toast.makeText(WeatherActivity.this,weatherId + "HeWeather获取天气信息失败",
                                    Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"onFailure获取天气信息失败",
                                Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

        });
        loadBingPic();
    }

    //加载必应每日一图
    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                swipeRefresh.setRefreshing(false);
            }

        });
    }

    //处理并展示Weather实体类中的数据
    private void showWeatherInfo(Weather weather){
        if (weather != null && "ok".equals(weather.status)) {
            String cityName = weather.basic.cityName;
            String updateTime = weather.basic.update.updateTime.split(" ")[1];
            String degree = weather.now.temperature + "℃";
            String weatherInfo = weather.now.more.info;
            titleCity.setText(cityName);
            titleUpdateTime.setText(updateTime);
            degreeText.setText(degree);
            weatherInfoText.setText(weatherInfo);
            //Glide.with(this).load(R.drawable.cloudy).into(weatherIcon);
            showWeatherIcon(weather);
            forecastLayout.removeAllViews();
            for (Forecast forecast : weather.forecastList) {
                //List<Forecast> forecast = weather.forecastList;
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                        forecastLayout, false);
                TextView dateText = view.findViewById(R.id.date_text);
                TextView infoText = view.findViewById(R.id.info_text);
                TextView maxText = view.findViewById(R.id.max_text);
                TextView minText = view.findViewById(R.id.min_text);
                dateText.setText(forecast.date);
                infoText.setText(forecast.more.info);
                maxText.setText(forecast.temperature.max + "℃");
                minText.setText(forecast.temperature.min + "℃");
                forecastLayout.addView(view);
            }
            if (weather.aqi != null) {
                aqiText.setText(weather.aqi.city.aqi);
                pm25Text.setText(weather.aqi.city.pm25);
                qltyText.setText(weather.aqi.city.qlty);
                //qltyText.setTextColor(Color.CYAN);
//                if (weather.aqi.city.aqi != "NA") {
//                    int i = 0;
//                    i = Integer.parseInt(weather.aqi.city.aqi);
//                    chooseColor(i);
//                }
                showAQIColor(weather);
               // Toast.makeText(this,weather.aqi.city.aqi,Toast.LENGTH_SHORT).show();
            }
            String comfort = "舒适度" + weather.suggestion.comfort.info;
            String carWash = "洗车指数" + weather.suggestion.carWash.info;
            String sport = "运动建议" + weather.suggestion.sport.info;
            comfortText.setText(comfort);
            carWashText.setText(carWash);
            sportText.setText(sport);
            weatherLayout.setVisibility(View.VISIBLE);
            Intent intent = new Intent(this, AutoUpdateService.class);
            startService(intent);
        }else {
            Toast.makeText(this,"AUS获取天气信息失败",Toast.LENGTH_SHORT).show();
        }
    }

    //
    private void showAQIColor(Weather weather){

        if ("NA".equals(weather.aqi.city.aqi)) {
            //Toast.makeText(this,weather.aqi.city.aqi,Toast.LENGTH_LONG).show();
            qltyText.setTextColor(Color.WHITE);
        }else {
            int i = 0;
            i = Integer.parseInt(weather.aqi.city.aqi);
            chooseColor(i);
        }
    }

    //显示天气图标信息
    private void showWeatherIcon(Weather weather){
        switch (weather.now.more.info){
            case "多云":
                Glide.with(this).load(R.drawable.cloudy).into(weatherIcon);
                break;
            case "阴":
                Glide.with(this).load(R.drawable.overcast).into(weatherIcon);
                break;
            case "晴":
                Glide.with(this).load(R.drawable.sunny).into(weatherIcon);
                break;
            case "小雨":
                Glide.with(this).load(R.drawable.light_rain).into(weatherIcon);
                break;
            case "阵雨":
                Glide.with(this).load(R.drawable.shower_rain).into(weatherIcon);
                break;
            case "雷阵雨":
                Glide.with(this).load(R.drawable.thunder_shower).into(weatherIcon);
                break;
            case "中雨":
                Glide.with(this).load(R.drawable.moderate_rain).into(weatherIcon);
                break;
            case "大雨":
                Glide.with(this).load(R.drawable.heavy_rain).into(weatherIcon);
                break;
            case "暴雨":
                Glide.with(this).load(R.drawable.storm_rain).into(weatherIcon);
                break;
            case "大暴雨":
                Glide.with(this).load(R.drawable.heavy_storm).into(weatherIcon);
                break;
            case "小到中雨":
                Glide.with(this).load(R.drawable.light_to_moderate_rain).into(weatherIcon);
                break;
            case "中到大雨":
                Glide.with(this).load(R.drawable.moderate_to_heavy_rain).into(weatherIcon);
                break;
            case "大到暴雨":
                Glide.with(this).load(R.drawable.heavy_rain_to_strom).into(weatherIcon);
                break;
            case "特大暴雨":
                Glide.with(this).load(R.drawable.severe_storm).into(weatherIcon);
                break;
            default:
                Glide.with(this).load(R.drawable.unknow).into(weatherIcon);
                break;
        }
    }
    private void chooseColor(int i){
        if (i > 100 && i <= 150){  //轻度污染
            qltyText.setTextColor(Color.YELLOW);
        }else if (0 <= i && i <= 50){ //优
            qltyText.setTextColor(Color.GREEN);
        }else if (i > 50 && i <= 100){ //良
            qltyText.setTextColor(Color.CYAN);
        }else if (i > 150 && i <= 200){ //中度污染
            qltyText.setTextColor(Color.parseColor("ffa500"));
        }else { //重度污染
            qltyText.setTextColor(Color.RED);
        }
    }
}
