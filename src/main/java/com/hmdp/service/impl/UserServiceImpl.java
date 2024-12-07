package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //检验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //不符合，返回错误信息
            return Result.fail("手机号格式有误");
        }
        //符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //redis保存验证码,有效期为2分钟    set key value ex 120
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY +phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码 模拟 log需要类上添加@Slf4j注解
        log.debug("发送验证码成功,验证码为:{}", code);
        //返回成功信息
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        //检验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //不符合，返回错误信息
            return Result.fail("手机号格式有误");
        }
        //从redis中获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if(cacheCode == null || !cacheCode.equals(code)){
            return Result.fail("验证码失效或有误");
        }
        //手机号查询看用户是否存在 select * form tb_user where phone = ?
        //使用mybatis-plus
        User user = query().eq("phone", phone).one();
        //不存在，创建用户
        if (user == null){
            user = createUserWithPhone(phone);
        }
        //用户信息存到redis中
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //这里将UserDTO中的属性转为string类型的
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString())
        );
        //生成随机token
        String token = UUID.randomUUID().toString(true);
        //将用户信息放到redis中
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,userMap);
        //设置有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);
        //返回token
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        //获取当天日期
        LocalDateTime now = LocalDateTime.now();
        String nowFormat = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        //获取当天是该月的第几天
        int day = now.getDayOfMonth();
        //key
        String key = USER_SIGN_KEY + userId+nowFormat;
        //签到
        stringRedisTemplate.opsForValue()
                .setBit(key,day-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        //获取key
        Long userId = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        String format = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY+userId+format;
        //获取当前是本月的第几天
        int day = now.getDayOfMonth();
        //获取月初到现在的bit：BITFIELD key get u[dayOfMonth] 0
        List<Long> res = stringRedisTemplate.opsForValue()
                .bitField(key,
                        BitFieldSubCommands.create().get(
                                BitFieldSubCommands.BitFieldType.unsigned(day)
                        ).valueAt(0));
        if (res == null||res.isEmpty()){
            return Result.ok(0);
        }
        Long days = res.get(0);
        if (days == null || days == 0){
            return Result.ok(0);
        }
        int count = 0;
        while (true){
            if ((days&1)==0){
                break;
            }else {
                count++;
            }
            days>>>=1;
        }

        return Result.ok(count);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        save(user);
        return user;
    }

}
