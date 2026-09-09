package com.uniticket.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniticket.config.JwtProperties;
import com.uniticket.dto.LoginFormDTO;
import com.uniticket.dto.Result;
import com.uniticket.dto.TokenPairDTO;
import com.uniticket.entity.User;
import com.uniticket.mapper.UserMapper;
import com.uniticket.service.IUserService;
import com.uniticket.utils.AuthConstants;
import com.uniticket.utils.JwtUtil;
import com.uniticket.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private JwtProperties jwtProperties;
    
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
        TokenPairDTO tokenPair = createTokenPair(user.getId());
        return Result.ok(tokenPair);
    }

    @Override
    public Result refresh(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            return Result.fail("InvalidRefreshToken");
        }
        String rotatedKey = REFRESH_TOKEN_ROTATED_KEY + refreshToken;
        String rotatedRefresh = stringRedisTemplate.opsForValue().get(rotatedKey);
        if (StrUtil.isNotBlank(rotatedRefresh)) {
            String userIdValue = stringRedisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + rotatedRefresh);
            if (StrUtil.isBlank(userIdValue)) {
                return Result.fail("InvalidRefreshToken");
            }
            Long userId = Long.valueOf(userIdValue);
            String accessToken = jwtUtil.createAccessToken(userId, AuthConstants.DEFAULT_SCOPES);
            return Result.ok(new TokenPairDTO(accessToken, rotatedRefresh, accessTokenExpiresInSeconds()));
        }

        String revokedUserId = stringRedisTemplate.opsForValue().get(REFRESH_TOKEN_REVOKED_KEY + refreshToken);
        if (StrUtil.isNotBlank(revokedUserId)) {
            revokeAllRefreshTokens(Long.valueOf(revokedUserId));
            return Result.fail("RefreshTokenReused");
        }

        String userIdValue = stringRedisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + refreshToken);
        if (StrUtil.isBlank(userIdValue)) {
            return Result.fail("RefreshTokenExpired");
        }
        Long userId = Long.valueOf(userIdValue);

        String newRefreshToken = UUID.randomUUID().toString();
        Boolean firstRotate = stringRedisTemplate.opsForValue()
                .setIfAbsent(rotatedKey, newRefreshToken, jwtProperties.getRefreshBufferSeconds(), TimeUnit.SECONDS);
        if (firstRotate == null) {
            return Result.fail("RefreshTokenExpired");
        }
        if (Boolean.FALSE.equals(firstRotate)) {
            String existingRefresh = stringRedisTemplate.opsForValue().get(rotatedKey);
            if (StrUtil.isBlank(existingRefresh)) {
                return Result.fail("RefreshTokenExpired");
            }
            String accessToken = jwtUtil.createAccessToken(userId, AuthConstants.DEFAULT_SCOPES);
            return Result.ok(new TokenPairDTO(accessToken, existingRefresh, accessTokenExpiresInSeconds()));
        }

        stringRedisTemplate.delete(REFRESH_TOKEN_KEY + refreshToken);
        stringRedisTemplate.opsForSet().remove(REFRESH_TOKEN_USER_SET_KEY + userId, refreshToken);
        saveRefreshToken(userId, newRefreshToken);
        stringRedisTemplate.opsForValue().set(
                REFRESH_TOKEN_REVOKED_KEY + refreshToken,
                String.valueOf(userId),
                jwtProperties.getRefreshTtlDays(),
                TimeUnit.DAYS
        );

        String accessToken = jwtUtil.createAccessToken(userId, AuthConstants.DEFAULT_SCOPES);
        return Result.ok(new TokenPairDTO(accessToken, newRefreshToken, accessTokenExpiresInSeconds()));
    }

    @Override
    public Result logout(Long userId) {
        if (userId == null) {
            return Result.ok();
        }
        revokeAllRefreshTokens(userId);
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);  // 保存到数据库
        return user;
    }

    private TokenPairDTO createTokenPair(Long userId) {
        String accessToken = jwtUtil.createAccessToken(userId, AuthConstants.DEFAULT_SCOPES);
        String refreshToken = UUID.randomUUID().toString();
        saveRefreshToken(userId, refreshToken);
        return new TokenPairDTO(accessToken, refreshToken, accessTokenExpiresInSeconds());
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        stringRedisTemplate.opsForValue().set(
                REFRESH_TOKEN_KEY + refreshToken,
                String.valueOf(userId),
                jwtProperties.getRefreshTtlDays(),
                TimeUnit.DAYS
        );
        stringRedisTemplate.opsForSet().add(REFRESH_TOKEN_USER_SET_KEY + userId, refreshToken);
    }

    private void revokeAllRefreshTokens(Long userId) {
        String userSetKey = REFRESH_TOKEN_USER_SET_KEY + userId;
        if (stringRedisTemplate.hasKey(userSetKey)) {
            for (String token : stringRedisTemplate.opsForSet().members(userSetKey)) {
                stringRedisTemplate.delete(REFRESH_TOKEN_KEY + token);
            }
        }
        stringRedisTemplate.delete(userSetKey);
    }

    private long accessTokenExpiresInSeconds() {
        return jwtProperties.getAccessTtlMinutes() * 60L;
    }
}
