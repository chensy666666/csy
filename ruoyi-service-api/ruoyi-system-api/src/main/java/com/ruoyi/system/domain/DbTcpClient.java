package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * tcp在线设备对象 db_tcp_client
 *
 * @author fangyuan
 * @date 2020-09-07
 */
@ApiModel
public class DbTcpClient extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long tcpClientId;

    /** 心跳名称 */
    @Excel(name = "心跳名称")
    @ApiModelProperty(value = "心跳名称")
    private String heartName;


    /** 心跳时间 */
    @Excel(name = "心跳时间", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "心跳时间")
    private Date heartbeatTime;

    /** ip */
    @Excel(name = "ip")
    @ApiModelProperty(value = "ip")
    private String ip;

    /** 端口 */
    @Excel(name = "端口")
    @ApiModelProperty(value = "端口")
    private String port;

    /** 是否在线（0在线，1不在线） */
    @Excel(name = "是否在线", readConverterExp = "0=在线，1不在线")
    @ApiModelProperty(value = "是否在线")
    private Integer isOnline;

    public void setTcpClientId(Long tcpClientId)
    {
        this.tcpClientId = tcpClientId;
    }

    public Long getTcpClientId()
    {
        return tcpClientId;
    }
    public void setHeartName(String heartName)
    {
        this.heartName = heartName;
    }

    public String getHeartName()
    {
        return heartName;
    }
    public void setHeartbeatTime(Date heartbeatTime)
    {
        this.heartbeatTime = heartbeatTime;
    }

    public Date getHeartbeatTime()
    {
        return heartbeatTime;
    }
    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public String getIp()
    {
        return ip;
    }
    public void setPort(String port)
    {
        this.port = port;
    }

    public String getPort()
    {
        return port;
    }
    public void setIsOnline(Integer isOnline)
    {
        this.isOnline = isOnline;
    }

    public Integer getIsOnline()
    {
        return isOnline;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("tcpClientId", getTcpClientId())
                .append("heartName", getHeartName())
                .append("heartbeatTime", getHeartbeatTime())
                .append("ip", getIp())
                .append("port", getPort())
                .append("isOnline", getIsOnline())
                .toString();
    }
}