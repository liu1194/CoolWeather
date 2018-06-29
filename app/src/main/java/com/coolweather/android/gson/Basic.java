package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {
    //由于JSON中的一些字段可能不太适合直接作为JAVA字段来命名
    //因此使用了@SerializedName注解来和JAVA字段之间建立联系
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
