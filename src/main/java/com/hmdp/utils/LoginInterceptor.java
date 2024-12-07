package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

public class LoginInterceptor implements HandlerInterceptor {
//HandlerInterceptor
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    if (UserHolder.getUser() ==null){
        response.setStatus(401);
        return false;
    }
    return true;
}

    /*public StringRedisTemplate stringRedisTemplate;
    //这里写了一个构造函数
    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    //preHandle就是在请求拦截器之前进行的
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //现在要基于redis了。
        //获取请求头的token
        String token = request.getHeader("token");
        if(StrUtil.isBlank(token)){//token不存在的话，则进行拦截，返回401
            response.setStatus(401);
            return false;
        }
       //在基于token 去获取redis中的用户 ,redis中我们是按照hashmap存储的，而不是字符串。
        String key = LOGIN_USER_KEY + token;
        Map<Object, Object> usermap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
        //再去判断用户是否存在
        if(usermap.isEmpty()){
            response.setStatus(401);
            return false;
        }
        //再将查询到的hash数据转化为UserDto对象
         UserDTO userDTO= BeanUtil.fillBeanWithMap(usermap, new UserDTO(),false);
        //保存信息到Threadlocal中  ，Threadlocal就是一个线程，建立起一个链接 就是会有一个Threadlocal
        UserHolder.saveUser(userDTO);

        //7刷新token的有效期
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;*/

    }

    /*//移除用户,避免内存泄露
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, @Nullable Exception ex) throws Exception {
        UserHolder.removeUser();
    }*/




 /*if (UserHolder.getUser() ==null){
            response.setStatus(401);
            return false;
        }
        return true;*//*
        //基于session的怎么实现
        HttpSession session = request.getSession();
        Object user = session.getAttribute("user");//获取到session中的useer
        if (user == null) {
            response.setStatus(401);
            return false;
        }
        //存放，保存用户信息到ThreadLocal
        UserHolder.saveUser((UserDTO)user);//(User)user 表示为强转类型
        return true;//就是放行*/
