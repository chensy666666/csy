package com.ruoyi.fangyuanapi.controller;

/*
 * 通风操作，以及状态回写
 * */


import com.ruoyi.common.core.domain.R;
import com.ruoyi.fangyuanapi.aspect.OperationLog;
import com.ruoyi.fangyuanapi.aspect.OperationLogType;
import com.ruoyi.fangyuanapi.service.IDbEquipmentService;
import com.ruoyi.system.domain.DbEquipment;
import com.ruoyi.system.feign.RemoteTcpService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Api("OperateVentilate")
@RequestMapping("OperateVentilate")
public class OperateVentilate {


    @Autowired
    private RemoteTcpService remoteTcpService;

    @Autowired
    private IDbEquipmentService equipmentService;

    /*
     * 自动通风操作  更改开始关闭的温度
     * */
    @GetMapping("operateTongFengType")
    @ApiOperation(value = "操作自动通风自动开启关闭的温度", notes = "操作自动通风自动开启关闭的温度")
    public R operateTongFengType(@ApiParam(name = "dbEquipmentId", value = "dbEquipmentID") Long dbEquipmentId,
                                 @ApiParam(name = "i", value = "是否开启0,1") Integer i,
                                 @ApiParam(name = "temp", value = "温度") Integer temp) {

        DbEquipment dbEquipment = equipmentService.selectDbEquipmentById(dbEquipmentId);
        return remoteTcpService.operateTongFengType(dbEquipment.getHeartbeatText(), dbEquipment.getEquipmentNoString(), i, temp+"");

    }

    /*
     * 自动通风操作  更改开始关闭装态
     * */
    @GetMapping("operateTongFengHand")
    @ApiOperation(value = "操作自动通风是否开启自动", notes = "操作自动通风是否开启自动")
    public R operateTongFengHand(@ApiParam(name = "dbEquipmentId", value = "dbEquipmentID") Long dbEquipmentId, @ApiParam(name = "i", value = "是否开启0,1") Integer i) {
        DbEquipment equipment = equipmentService.selectDbEquipmentById(dbEquipmentId);
        return remoteTcpService.operateTongFengHand(equipment.getHeartbeatText(), equipment.getEquipmentNoString(), i);

    }

}
