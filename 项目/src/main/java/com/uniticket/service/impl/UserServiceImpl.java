package com.uniticket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniticket.dto.LoginFormDTO;
import com.uniticket.dto.Result;
import com.uniticket.dto.UserDTO;
import com.uniticket.entity.User;
import com.uniticket.mapper.UserMapper;
import com.uniticket.service.IUserService;
import com.uniticket.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.uniticket.utils.RedisConstants.*;
import static com.uniticket.utils.SystemConstants.USER_NICK_NAME_PREFIX;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        // 2.如果不符合返回错误信息
        if (RegexUtils.isPhoneInvalid(phone)){return Result.fail("phone number format error!");}
        //3.如果符合就生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4.保存验证码到Redis，设置5分钟过期，login：code为业务前缀
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5.发送验证码（简易）
        log.debug("send code:{} to phone:{}",code, phone);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){return Result.fail("phone number format error!");}
        //2.从 Redis 获取并校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)){return Result.fail("code error!");}
        //3.根据手机号查询用户 SELECT * FROM tb_user WHERE phone = ?
        User user = query().eq("phone", phone).one();
        //4.判断用户是否存在
        if (user == null) {
            //不存在，创建新用户并保存
            user = createUserWithPhone(phone);
        }
        //5保存用户信息到redis（转换为UserDTO避免敏感信息泄露）
        //5.1生成token
        String token = UUID.randomUUID().toString();
        //5.2将UserDTO转为hash，保存到redis中
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class); //保留同名字段 注意要参考目标段的.class对象
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token, userMap);
        //5.3设置token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token, LOGIN_USER_TTL, TimeUnit.MINUTES);

        //返回用户信息
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);  // 保存到数据库
        return user;
    }
}
