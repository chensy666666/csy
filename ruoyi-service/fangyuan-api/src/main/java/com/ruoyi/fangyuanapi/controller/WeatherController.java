package com.ruoyi.fangyuanapi.controller;


import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.HttpUtil;
import com.ruoyi.system.domain.WeatherVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
/*
 *天气调用服务
 * */
@RestController
@Api("weather")
@RequestMapping("weather")
public class WeatherController {

//    private HttpUtil httpUtils;

    /**
     * 查询${tableComment}
     */
    @GetMapping("getWeather")
    @ApiOperation(value = "根据经纬度查询天气情况", notes = "根据经纬度查询天气情况")
    public AjaxResult getWeather(@ApiParam(name = "lng", value = "经度", required = true) String lng, @ApiParam(name = "lat", value = "纬度", required = true) String lat, @ApiParam(name = "type", value = "\t输入的坐标类型： 1：GPS设备获取的角度坐标; 2：GPS获取的米制坐标、sogou地图所用坐标; 3：google地图、soso地图、aliyun地图、mapabc地图和amap地图所用坐标 4：3中列表地图坐标对应的米制坐标 5：百度地图采用的经纬度坐标 6：百度地图采用的米制坐标 7：mapbar地图坐标; 8：51地图坐标", required = true) String type) {

        WeatherVo toget = toget(lng, lat, type,1);
        if (toget==null){
            return AjaxResult.error("坐标转换失败");
        }
        return AjaxResult.success(toget);
    }
    @GetMapping("getWeatherWeChat")
    @ApiOperation(value = "根据经纬度查询天气情况", notes = "根据经纬度查询天气情况")
    public AjaxResult getWeatherWeChat(@ApiParam(name = "lat", value = "经度", required = true) String lat, @ApiParam(name = "lng", value = "纬度", required = true) String lng, @ApiParam(name = "type", value = "\t输入的坐标类型： 1：GPS设备获取的角度坐标; 2：GPS获取的米制坐标、sogou地图所用坐标; 3：google地图、soso地图、aliyun地图、mapabc地图和amap地图所用坐标 4：3中列表地图坐标对应的米制坐标 5：百度地图采用的经纬度坐标 6：百度地图采用的米制坐标 7：mapbar地图坐标; 8：51地图坐标", required = true) String type) {

        WeatherVo toget = toget(lng, lat, type,2);
        if (toget==null){
            return AjaxResult.error("坐标转换失败");
        }
        return AjaxResult.success(toget);
    }

    @Value("${com.fangyuan.weather.host}")
    private String host;

    @Value("${com.fangyuan.weather.path}")
    private String path;

    @Value("${com.fangyuan.weather.appcode}")
    private String appcode;

    public WeatherVo toget(String lng, String lat, String type,int code) {
        String method = "GET";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("from", type);
        querys.put("lat", lat);
        querys.put("lng", lng);
        querys.put("need3HourForcast", "0");
        querys.put("needAlarm", "1");
        querys.put("needHourData", "0");
        querys.put("needIndex", "0");
        querys.put("needMoreDay", "0");

        try {
            HttpResponse httpResponse = HttpUtil.doGet(host, path, method, headers, querys);
            //获取response的body
            String s = EntityUtils.toString(httpResponse.getEntity());
            Map<String, Object> parse = (Map<String, Object>) JSONObject.parse(s);
            Map<String, Object> showapi_res_body = (Map<String, Object>) parse.get("showapi_res_body");

            Object now = showapi_res_body.get("now");
            String s1 = com.alibaba.fastjson.JSON.toJSONString(now);
            WeatherVo parse1 = com.alibaba.fastjson.JSON.parseObject(s1, WeatherVo.class);
            /*
             * 替换指定图片
             * */
            if (code==1){

            WeatherVo parse2 = replacePic(parse1);

                return parse2;
            }else {
                WeatherVo parse2 = replacePic2(parse1);

                return parse2;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private WeatherVo replacePic(WeatherVo parse1) {
        String weather = parse1.getWeather();
        if (weather.contains("雨")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/%E9%9B%A8.png");
        } else if (weather.contains("冰雹")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/%E5%86%B0%E9%9B%B9.png");
        } else if (weather.contains("晴")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/%E6%99%B4%E5%A4%A9.png");
        } else if (weather.contains("多云")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/%E5%A4%9A%E4%BA%91.png?e=1601279187&token=-rrLxBpbfyQjssy5kU0GGZdYUFLR7p_T225rvz5I:XyurSeO6QuWT5IbwZYTwqqg-Peo=");
        } else if (weather.contains("雪")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/%E9%9B%AA.png");
        } else if (weather.contains("雷")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/%E9%9B%B7%E9%98%B5%E9%9B%A8.png");
        } else if (weather.contains("风")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/%E9%A3%8E.png");
        }
        return parse1;
    }

    private WeatherVo replacePic2(WeatherVo parse1) {
        String weather = parse1.getWeather();
        if (weather.contains("雨")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/fangyuan/20201116/98b238bad46b4dd1801299d708cd7729.png");
        } else if (weather.contains("冰雹")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/fangyuan/20201116/035f9fc4670b44b992e7e172504df53f.png");
        } else if (weather.contains("晴")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/fangyuan/20201116/f5f2d96440bb4e1ea00fcaa057ecd9d0.png");
        } else if (weather.contains("多云")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/fangyuan/20201116/6c01e705a9804e84b91ba4440d18bb0e.png");
        } else if (weather.contains("雪")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/fangyuan/20201116/d859091bb942481d887330cdc5f8db58.png");
        } else if (weather.contains("雷")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/fangyuan/20201116/8ba7aa45e1444485ab993d4554f3de04.png");
        } else if (weather.contains("风")) {
            parse1.setWeather_pic("http://cdn.fangyuancun.cn/fangyuan/20201116/d1cdd56d6d15452d9785fe0ec77d8a71.png");
        }
        return parse1;
    }




}
