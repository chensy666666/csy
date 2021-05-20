package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.domain.TreeEntity;

/**
 * 土地对象 db_land
 *
 * @author zheng
 * @date 2020-09-25
 */
@ApiModel
public class DbLand extends TreeEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long landId;

    /** 地区（省市县三级逗号分隔） */
    @Excel(name = "地区", readConverterExp = "省=市县三级逗号分隔")
    @ApiModelProperty(value = "地区")
    private String region;

    /** 经度 */
    @Excel(name = "经度")
    @ApiModelProperty(value = "经度")
    private String longitude;

    /** 纬度 */
    @Excel(name = "纬度")
    @ApiModelProperty(value = "纬度")
    private String latitude;

    /** 产品类别 */
    @Excel(name = "产品类别")
    @ApiModelProperty(value = "产品类别")
    private String productCategory;

    /** 产品名称 */
    @Excel(name = "产品名称")
    @ApiModelProperty(value = "产品名称")
    private String productName;

    /** 备注信息 */
    @Excel(name = "备注信息")
    @ApiModelProperty(value = "备注信息")
    private String noteText;

    /** 关联用户id */
    @Excel(name = "关联用户id")
    @ApiModelProperty(value = "关联用户id")
    private Long dbUserId;

    /** 详细地址 */
    @Excel(name = "详细地址")
    @ApiModelProperty(value = "详细地址")
    private String address;

    @Override
    public String toString() {
        return "DbLand{" +
                "landId=" + landId +
                ", region='" + region + '\'' +
                ", longitude='" + longitude + '\'' +
                ", latitude='" + latitude + '\'' +
                ", productCategory='" + productCategory + '\'' +
                ", productName='" + productName + '\'' +
                ", noteText='" + noteText + '\'' +
                ", dbUserId=" + dbUserId +
                ", address='" + address + '\'' +
                ", nickName='" + nickName + '\'' +
                ", siteId=" + siteId +
                ", equipmentIds='" + equipmentIds + '\'' +
                '}';
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /** 别称自定义 */
    @Excel(name = "别称自定义")
    @ApiModelProperty(value = "别称自定义")
    private String nickName;

    /** 地区id */
    @Excel(name = "地区id")
    @ApiModelProperty(value = "地区id")
    private Long siteId;

    /** 设备集 */
    @Excel(name = "设备集")
    @ApiModelProperty(value = "设备集")
    private String equipmentIds;

    public void setLandId(Long landId)
    {
        this.landId = landId;
    }

    public Long getLandId()
    {
        return landId;
    }
    public void setRegion(String region)
    {
        this.region = region;
    }

    public String getRegion()
    {
        return region;
    }
    public void setLongitude(String longitude)
    {
        this.longitude = longitude;
    }

    public String getLongitude()
    {
        return longitude;
    }
    public void setLatitude(String latitude)
    {
        this.latitude = latitude;
    }

    public String getLatitude()
    {
        return latitude;
    }
    public void setProductCategory(String productCategory)
    {
        this.productCategory = productCategory;
    }

    public String getProductCategory()
    {
        return productCategory;
    }
    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getProductName()
    {
        return productName;
    }
    public void setNoteText(String noteText)
    {
        this.noteText = noteText;
    }

    public String getNoteText()
    {
        return noteText;
    }
    public void setDbUserId(Long dbUserId)
    {
        this.dbUserId = dbUserId;
    }

    public Long getDbUserId()
    {
        return dbUserId;
    }
    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getNickName()
    {
        return nickName;
    }
    public void setSiteId(Long siteId)
    {
        this.siteId = siteId;
    }

    public Long getSiteId()
    {
        return siteId;
    }
    public void setEquipmentIds(String equipmentIds)
    {
        this.equipmentIds = equipmentIds;
    }

    public String getEquipmentIds()
    {
        return equipmentIds;
    }

}