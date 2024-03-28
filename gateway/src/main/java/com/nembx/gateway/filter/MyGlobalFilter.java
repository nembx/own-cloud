package com.nembx.gateway.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.nembx.gateway.properties.Authproperties;
import com.nembx.gateway.utils.JwtokenUtil;
import com.nembx.gateway.utils.RedisUtil;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Lian
 */


@Order(-1)
@Component
@RequiredArgsConstructor
public class MyGlobalFilter implements GlobalFilter {
    @Resource
    private final Authproperties authproperties;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private final RedisUtil redisUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (isSkip(request.getPath().toString())){
            return chain.filter(exchange);
        }
        String token = null;
        List<String> authorization = request.getHeaders().get("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            token = authorization.get(0);
        }
        String jwToken;
        String name;
        try {
            JWT jwt = JWTUtil.parseToken(token);
            jwToken = jwt.getPayload().toString();
            name = jwt.getPayload("username").toString();
        }catch (Exception e){
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        if(!redisUtil.hasKey("token:"+name) || redisUtil.getExpire("token:"+name) <= 1){
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        System.out.println(jwToken);
        return chain.filter(exchange);
    }

    private boolean isSkip(String url){
        for (String path : authproperties.getSkip()){
            if (antPathMatcher.match(path,url)){
                return true;
            }
        }
        return false;
    }
}
