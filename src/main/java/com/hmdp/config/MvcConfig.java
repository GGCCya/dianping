package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
//import com.hmdp.utils.RefreshTokenInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
//11.28 MvcConfig 的作用是用来配置拦截器的
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {  //去重写这个方法，来添加拦截器
        registry.addInterceptor(new LoginInterceptor())  //registry  就是用来拦截的意思
                //配置放行路径
                .excludePathPatterns(
                        "/voucher/**",
                        "/upload/**",
                        "/user/login",
                        "/user/code",
                        "/shop/**",
                        "/shop-type/**",
                        "/blog/hot"
                ).order(1);
        //token刷新拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).order(0);
    }//order(0)的数字就是执行的顺序
}
