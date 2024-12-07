package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //现在要基于redis了。
        //获取请求头的token
        String token = request.getHeader("authorization");   //为什么是authorization？
        if(StrUtil.isBlank(token)){//token不存在的话，则进行拦截，返回401
            //response.setStatus(401);   就是这样出错了11.28.
            return true;   //挪到这里话，就不需要拦截了
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
        return true;
        //*/

        /*//获取token
        String token = request.getHeader("authorization");
        //token不存在，放行
        if (token == null){
            return true;
        }
        //redis获取用户信息
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        //用户是否存在
        //不存在，拦截，状态码：401 未授权
        if (map.isEmpty()){
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        //存在，把用户放到ThreadLocal中
        UserHolder.saveUser(userDTO);
        //刷新token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL, TimeUnit.MINUTES);
        //放行
        return true;*/
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
