package com.nembx.userservice.controller;

import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nembx.userservice.domian.User;
import com.nembx.userservice.domian.UserInfo;
import com.nembx.userservice.result.CommonResult;
import com.nembx.userservice.service.UserInfoService;
import com.nembx.userservice.service.UserService;
import com.nembx.userservice.util.JwtokenUtil;
import io.swagger.annotations.ApiModelProperty;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Lian
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private UserInfoService userInfoService;

    @ApiModelProperty("登录接口")
    @RequestMapping("/login")
    private CommonResult<Map<Object, Object>> login(@RequestBody User user) throws JsonProcessingException {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername())
                    .eq("password", user.getPassword());
        if (null == userService.getOne(queryWrapper)){
            return new CommonResult<>(404, "账号不存在");
        }
        if(userService.findToken(user).isEmpty()){
            userService.refreshToken(user.getUsername());
        }
        return new CommonResult<>(200, "登录成功", userService.findToken(user));
    }

    @ApiModelProperty("注册接口")
    @RequestMapping("/register")
    private CommonResult<Boolean> register(@RequestBody User user) throws JsonProcessingException {
        if(login(user).getCode() == 200){
            return new CommonResult<>(409, "账号已存在");
        }
        userService.save(user);
        return new CommonResult<>(201, "创建成功",userService.createToken(user));
    }

    @ApiModelProperty("生成验证码接口")
    @RequestMapping("/produce/code")
    private CommonResult<String> produceCode(){
        return new CommonResult<>(201, "成功",userService.produceCode());
    }

    @ApiModelProperty("发送邮件接口")
    @RequestMapping("/send/email/{email}")
    private String sendEmail(@PathVariable String email){
        return userService.sendEmail(email);
    }

    @ApiModelProperty("更新头像接口")
    @RequestMapping("/update/img")
    private Boolean updateImg(@RequestBody User user){
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("image",user.getImage());
        return userService.update(user,queryWrapper);
    }

    @ApiModelProperty("获取用户信息接口")
    @RequestMapping("/getInfo")
    private CommonResult<UserInfo> getUserInfo(@RequestHeader("Authorization") String token){
        if (!JwtokenUtil.verifyToken(token)){
            return new CommonResult<>(401, "token过期");
        }
        Object username = JWTUtil.parseToken(token).getPayload("username");
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username",username);
        return new CommonResult<>(201, "成功",userInfoService.getOne(queryWrapper));
    }

    @ApiModelProperty("刷新Token接口")
    @RequestMapping("/refreshToken")
    private CommonResult<Map<Object, Object>> getToken(@RequestHeader("Authorization") String token){
        if (!JwtokenUtil.verifyToken(token)){
            return new CommonResult<>(401, "token过期");
        }
        String username = JWTUtil.parseToken(token).getPayload("username").toString();
        return new CommonResult<>(201, "刷新Token成功",userService.refreshToken(username));
    }
}
