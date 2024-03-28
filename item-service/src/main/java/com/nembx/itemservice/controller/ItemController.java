package com.nembx.itemservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.nembx.itemservice.domain.Item;
import com.nembx.itemservice.result.CommonResult;
import com.nembx.itemservice.service.ItemService;
import io.swagger.annotations.ApiModelProperty;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Lian
 */
@RestController
@Slf4j
@RequestMapping("/item")
public class ItemController {
    @Resource
    private ItemService itemService;

    @ApiModelProperty("查询所有商品接口")
    @RequestMapping("/getAllItem")
    private CommonResult<List<Map<String,Object>>> getItem(){
        return new CommonResult<>(200, "查询成功",itemService.listMaps());
    }

    @ApiModelProperty("查询某商品接口")
    @RequestMapping("/searchItem/{item_name}")
    private CommonResult<List<Map<String,Object>>> searchItem(@PathVariable String item_name){
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("item_name",item_name);
        if (null == itemService.listMaps(queryWrapper)){
            return new CommonResult<>(404, "查询失败");
        }
        return new CommonResult<>(200, "查询成功",itemService.listMaps(queryWrapper));
    }

    @RequestMapping("/searchItemCount/{itemName}")
    private int searchItemCount(@PathVariable String itemName){
        return itemService.getOne(new QueryWrapper<Item>().eq("item_name", itemName).select("item_amount")).getItemAmount();
    }

    @RequestMapping("/search/{item_name}")
    private Item search(@PathVariable String item_name) {
        return itemService.getOne(new QueryWrapper<Item>().like("item_name", item_name));
    }

    @RequestMapping("/searchByCity/{item_city}")
    private Item searchByCity(@PathVariable String item_city) {
        return itemService.getOne(new QueryWrapper<Item>().eq("item_city", item_city));
    }

    @ApiModelProperty("减少库存接口")
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(name = "item-exchange", type = "direct"),
            value = @Queue("item-queue"),
            key = "decrease"
    ))
    private void decrease(String orderName){
        System.out.println("收到的信息是" + orderName);
        int count = itemService.getOne(new QueryWrapper<Item>().eq("item_name", orderName)).getItemAmount();
        if (count == 0){
               log.error("库存不足");
        }else {
            itemService.update(new UpdateWrapper<Item>()
                    .eq("item_name", orderName)
                    .set("item_amount", count - 1));
        }
    }
}
