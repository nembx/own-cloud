package com.nembx.orderservice.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nembx.feign.client.ItemClient;
import com.nembx.orderservice.domain.City;
import com.nembx.orderservice.domain.Order;
import com.nembx.orderservice.result.CommonResult;
import com.nembx.orderservice.service.CityService;
import com.nembx.orderservice.service.OrderService;
import io.swagger.annotations.ApiModelProperty;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Lian
 */
@RestController
@RequestMapping("/order")
public class OrderController {
    @Resource
    private OrderService orderService;

    @Resource
    private CityService cityService;

    @Resource
    private ItemClient itemClient;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @ApiModelProperty("添加订单接口")
    @RequestMapping("/addOrder")
    private CommonResult<String> addOrder(@RequestBody Order order){
        rabbitTemplate.convertAndSend("item-exchange","decrease",order.getOrderName());
        int count = itemClient.getItemCount(order.getOrderName());
        if (order == null || count <= 0){
            return new CommonResult<>(400, "库存不足，添加失败");
        }
        order.setOrderNumber(IdUtil.randomUUID());
        orderService.save(order);
        if(null == cityService.getOne(new QueryWrapper<City>().eq("city_name", order.getOrderCity()))){
            City city = new City();
            city.setCityName(order.getOrderCity());
            cityService.save(city);
        }
        City city = new City();
        city.setOrderCount(cityService.getOne(new QueryWrapper<City>().eq("city_name", order.getOrderCity())).getOrderCount()+1);
        cityService.update(city,new QueryWrapper<City>().eq("city_name", order.getOrderCity()));
        return new CommonResult<>(201, "添加成功");
    }

    @ApiModelProperty("更新订单接口")
    @RequestMapping("/updateOrder")
    private CommonResult<Order> updateOrder(@RequestBody Order order){
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_date", order.getOrderDate());
        queryWrapper.eq("order_name", order.getOrderName());
        queryWrapper.eq("order_state", order.getOrderState());
        queryWrapper.eq("order_city", order.getOrderCity());
        queryWrapper.eq("order_address", order.getOrderAddress());
        orderService.update(order, queryWrapper);
        return new CommonResult<>(200, "更新成功", order);
}

    @ApiModelProperty("删除订单接口")
    @RequestMapping("/deleteOrder")
    private CommonResult<String> deleteOrder(@RequestBody Order order){
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_date", order.getOrderDate());
        queryWrapper.eq("order_name", order.getOrderName());
        orderService.remove(queryWrapper);
        String cityName = orderService.getOne(new QueryWrapper<Order>().eq("order_name", order.getOrderName())).getOrderCity();
        City city = new City();
        city.setOrderCount(cityService.getOne(new QueryWrapper<City>().eq("city_name", cityName)).getOrderCount()-1);
        cityService.update(city, new QueryWrapper<City>().eq("city_name", cityName));
        return new CommonResult<>(200, "删除成功");
    }

    @ApiModelProperty("查询订单接口")
    @RequestMapping("/findOrder")
    private CommonResult<Order> findOrder(@RequestBody Order order){
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("order_name", order.getOrderName());
        Order od = orderService.getOne(queryWrapper);
        if(null == od){
            return new CommonResult<>(404, "没有找到相关订单");
        }
        return new CommonResult<>(200, "查询成功", od);
    }

    @ApiModelProperty("查询所有订单接口")
    @RequestMapping("/findAllOrder")
    private CommonResult<List<Map<String,Object>>> findAllOrder(){
        return new CommonResult<>(200, "查询成功",orderService.listMaps());
    }

    @ApiModelProperty("查询订单总数接口")
    @RequestMapping("/findOrderCount")
    private CommonResult<Long> findOrderCount(){
        return new CommonResult<>(200, "查询成功", orderService.count());
    }

    @ApiModelProperty("查询某城市的订单总数")
    @RequestMapping("/findOrderByCityCount/{order_city}")
    private CommonResult<Long> findOrderByCity(@PathVariable String order_city){
        return new CommonResult<>(200, "查询成功", orderService.count(
                new QueryWrapper<Order>().eq("order_city", order_city))
        );
    }

    @ApiModelProperty("查询非广州，上海，深圳，香港的订单总数")
    @RequestMapping("/findOrderOther")
    private CommonResult<Long> findOrderOther(){
        return new CommonResult<>(200, "查询成功",
                orderService.count(new QueryWrapper<Order>()
                .ne("order_city", "广州")
                .ne("order_city", "上海")
                .ne("order_city","深圳")
                .ne("order_city","香港")));
    }

    @ApiModelProperty("查询所有城市订单总数")
    @RequestMapping("/findAllCount")
    private CommonResult<List<Map<String,Object>>> findAllCount(){
        return new CommonResult<>(200, "查询成功", cityService.listMaps());
    }

    @ApiModelProperty("查询所有订单日期")
    @RequestMapping("/findDate")
    private CommonResult<List<Map<String,Object>>> findDate(){
        return new CommonResult<>(200, "查询成功",
                orderService.listObjs(new QueryWrapper<Order>().select("order_date")));
    }
}
