package com.ruoyi.fangyuantcp.controller;


import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.fangyuantcp.service.OperateVentilateService;
import com.ruoyi.fangyuantcp.processingCode.SendCodeUtils;
import com.ruoyi.fangyuantcp.processingCode.TcpOrderTextConf;
import com.ruoyi.system.domain.DbOperationVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/*
 * 通风相关操作
 * */
@RestController
@Api("operateVentilate")
@RequestMapping("operateVentilate")
public class OperateVentilateController extends BaseController {

    @Autowired
    private OperateVentilateService OperateVentilateService;


    private SendCodeUtils sendCodeUtils = new SendCodeUtils();

    /*
     * 通风 自动手动状态更改
     * */
    @GetMapping("operateTongFengHand/{heartbeatText}/{equipmentNo}/{i}")
    public R operateTongFengHand(@ApiParam(name = "heartbeatText", value = "string") @PathVariable String heartbeatText,
                                 @ApiParam(name = "equipmentNo", value = "string", required = true) @PathVariable("equipmentNo") String equipmentNo,
                                 @ApiParam(name = "i", value = "inter", required = true) @PathVariable("i") Integer i) throws InterruptedException {
        return OperateVentilateService.operateTongFengHand(heartbeatText, equipmentNo, i);



    }

    /*
     * 自动通风  开启关闭温度修改
     * */
    @GetMapping("operateTongFengType/{heartbeatText}/{equipmentNo}/{i}/{temp}")
    public R operateTongFengType(@ApiParam(name = "heartbeatText", value = "string") @PathVariable("heartbeatText") String heartbeatText,
                                 @ApiParam(name = "equipmentNo", value = "string", required = true) @PathVariable("equipmentNo") String equipmentNo,
                                 @ApiParam(name = "i", value = "inter", required = true) @PathVariable("i") Integer i,
                                 @ApiParam(name = "temp", value = "温度") @PathVariable("temp") String temp) throws InterruptedException {
        int i1 = Integer.parseInt(temp);
        int i2=i1/256;
        int i3=i1%256;
        String hex = Integer.toHexString(i2);
        String hex2 = Integer.toHexString(i3);
        String hex1="";
        if (i2<10){

            hex1 ="0"+i2+","+i3+"";
        }else {
            hex1 =i2+","+i3+"";
        }
        return OperateVentilateService.operateTongFengType(heartbeatText, equipmentNo, i, hex1);


    }


}
