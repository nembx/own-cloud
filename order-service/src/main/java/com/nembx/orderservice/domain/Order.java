package com.nembx.orderservice.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nembx.feign.domain.Item;
import lombok.Data;


/**
 * @author Lian
 */
@Data
@TableName("show_order")
public class Order {
    private String orderDate;
    private String orderName;
    private String orderNumber;
    private String orderPurchaser;
    private String orderCity;
    private int orderPrice;
    private String orderState;
    private String orderAddress;
    private Item item;
}
