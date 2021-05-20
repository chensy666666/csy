package com.ruoyi.system.feign;

/*
 * tcp服务接口 指令处理 温度查询
 * */

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.DbOperationVo;
import com.ruoyi.system.domain.DbTcpClient;
import com.ruoyi.system.domain.DbTcpType;
import com.ruoyi.system.feign.factory.RemoteTcpFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = ServiceNameConstants.SYSTEM_FANGYUANTCP, fallbackFactory = RemoteTcpFallbackFactory.class)
public interface RemoteTcpService {

    /*
     * 指令发送  单个
     * */
    @PostMapping(value = "operateGeneral/operation")
    public R operation(@RequestBody DbOperationVo dbOperationVo);

    /*
     * 指令发送  多个operationList
     * */
    @PostMapping(value = "operateGeneral/operationList")
    public R operationList(@RequestBody List<DbOperationVo> dbOperationVo);

    /*
     *状态查询
     * */
    @PostMapping(value = "type/listonly")
    public List<DbTcpType> list(@RequestBody DbTcpType dbTcpType);


    @RequestMapping(method = RequestMethod.GET, value = "operateVentilate/operateTongFengType/{heartbeatText}/{equipmentNo}/{i}/{temp}")
    R operateTongFengType(@PathVariable("heartbeatText") String heartbeatText, @PathVariable("equipmentNo") String equipmentNo, @PathVariable("i") Integer i, @PathVariable("temp") String temp);

    @RequestMapping(method = RequestMethod.GET, value = "operateVentilate/operateTongFengHand/{heartbeatText}/{equipmentNo}/{i}")
    R operateTongFengHand(@PathVariable("heartbeatText") String heartbeatText, @PathVariable("equipmentNo") String equipmentNo, @PathVariable(name = "i") Integer i);


    //    @GetMapping("intervalState/{startTime}/{endTime}/{interval}/{hearName}")
    @GetMapping("type/intervalState/{startTime}/{endTime}/{interval}/{hearName}")
    R intervalState(@PathVariable("startTime") String s, @PathVariable("endTime") String s1, @PathVariable("interval") String intervalTime, @PathVariable("hearName") String hearName);

    @GetMapping(value = "client/listOnly")
    List<DbTcpClient> tcpClients();


    //    所有状态更新（通风，手自，状态，通风状态）
    @PostMapping(value = "type/stateAllQuery")
    public R stateAllQuery(@RequestBody List<DbOperationVo> dbOperationVo);


    /*
     * 装态定时更新系列
     * */
//通风温度查询
    @GetMapping("type/saveTongFengType")
    R saveTongFengType();

    //通风状态查询
    @GetMapping("type/timingTongFengHand")
    R timingTongFengHand();

    //手自动查询
    @GetMapping(value = "client/sinceOrHand")
    R sinceOrHand();

    //操作记录留根
    @GetMapping("order/curing")
    void curingTiming();

    //传感器状态
    @GetMapping(value = "type/timingType")
    R strtTiming();

    //传感器状态留根
    @GetMapping(value = "type/curingType")
    R startSaveTiming();
}
