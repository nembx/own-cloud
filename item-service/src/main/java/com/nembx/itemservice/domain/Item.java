package com.nembx.itemservice.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author Lian
 */
@Data
@TableName("item")
public class Item {
    private String itemName;
    private int itemPrice;
    private String itemImage;
    private String itemCity;
    private String itemState;
    private String itemAddress;
    private double itemLongitude;
    private double itemLatitude;
    private int itemAmount;
}
