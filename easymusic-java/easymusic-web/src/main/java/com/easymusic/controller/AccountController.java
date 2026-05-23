package com.easymusic.controller;

import com.easymusic.annotation.GlobalInterceptor;
import com.easymusic.entity.dto.TokenUserInfoDTO;
import com.easymusic.entity.enums.MusicStatusEnum;
import com.easymusic.entity.enums.ResponseCodeEnum;
import com.easymusic.entity.po.UserInfo;
import com.easymusic.entity.query.MusicInfoActionQuery;
import com.easymusic.entity.query.MusicInfoQuery;
import com.easymusic.entity.vo.CheckCodeVO;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.exception.BusinessException;
import com.easymusic.redis.RedisComponent;
import com.easymusic.service.MusicInfoActionService;
import com.easymusic.service.MusicInfoService;
import com.easymusic.service.UserInfoService;

import com.wf.captcha.ArithmeticCaptcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.*;

/**
 * 用户信息 Controller
 */
@RestController
@Slf4j
@Validated
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController extends ABaseController {

	private final UserInfoService userInfoService;

	private final MusicInfoService musicInfoService;

	private final MusicInfoActionService musicInfoActionService;
	
	private final RedisComponent redisComponent;

	@RequestMapping("/checkCode")
	public ResponseVO checkCode() {
		ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 48);
		String code = captcha.text();  //code是验证码计算出的结果
		log.info("code:" + code);
		String checkCodeBase64 = captcha.toBase64();

		String checkCodeKey = redisComponent.saveCode(code);

		CheckCodeVO result = new CheckCodeVO();

		result.setCheckCodeKey(checkCodeKey);
		result.setCheckCode(checkCodeBase64);
		return getSuccessResponseVO(result);

	}

	@RequestMapping(value = "/register")
	public ResponseVO register(@NotEmpty String checkCodeKey,
							   @NotEmpty @Email @Size(max = 50) String email,
							   @NotEmpty @Size(min = 8, max = 18) String password,
							   @NotEmpty String rePassword,
							   @NotEmpty @Size(max = 20) String nickName,
							   @NotEmpty String checkCode) {
		try {
			if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
				throw new BusinessException("图片验证码不正确");
			}
			log.info("password: " + password + " " + rePassword);
			if (password.equals(rePassword) == false) {
				throw new BusinessException("密码不一致");
			}
			userInfoService.register(email, password, nickName);
			return getSuccessResponseVO(null);
		} finally {
			redisComponent.cleanCheckCode(checkCodeKey);
		}
	}

	@RequestMapping(value = "/login")
	public ResponseVO login(
							@NotEmpty String checkCodeKey,
							@NotEmpty @Email @Size(max = 50) String email,
							@NotEmpty @Size(max = 32) String password,
							@NotEmpty String checkCode) {

		if (redisComponent.getCheckCode(checkCodeKey) != null && redisComponent.getCheckCode(checkCodeKey).equals(checkCode) == false) {
			throw new BusinessException("图片验证码不正确");
		}

		TokenUserInfoDTO tokenUserInfoDTO = userInfoService.login(email, password);

		if (redisComponent.getCheckCode(checkCodeKey) != null) {
			redisComponent.cleanCheckCode(checkCodeKey);
		}
		return getSuccessResponseVO(tokenUserInfoDTO);

	}

	//这个接口的作用主要是实现自动登录，获取token。
	@RequestMapping("/getLoginInfo")
	public ResponseVO getLoginInfo() {
		// 1. 先从请求中解析 token 对应的用户信息，可能为 null
		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO();
		if (tokenUserInfoDTO == null) {
			// 未登录或 token 已失效，统一按登录超时处理
			throw new BusinessException(ResponseCodeEnum.CODE_901);
		}

		// 2. 再根据 userId 获取用户信息（走布隆过滤器 + 缓存 + 数据库）
		UserInfo userInfo = this.userInfoService.getUserInfoByUserId(tokenUserInfoDTO.getUserId());
		if (userInfo == null) {
			// 用户被删除或布隆过滤器/缓存不一致，也统一按登录超时处理
			throw new BusinessException(ResponseCodeEnum.CODE_901);
		}

		// 3. 补充积分信息返回前端
		tokenUserInfoDTO.setIntegral(userInfo.getIntegral());

		// 4. 统计歌曲总数
		MusicInfoQuery musicInfoQuery = new MusicInfoQuery();
		musicInfoQuery.setUserId(tokenUserInfoDTO.getUserId());
		musicInfoQuery.setMusicStatus(MusicStatusEnum.CREATED.getStatus());
		Integer musicCount = this.musicInfoService.findCountByParam(musicInfoQuery);
		tokenUserInfoDTO.setMusicCount(musicCount);

		// 5. 统计该用户所有歌曲的总点赞数
		MusicInfoActionQuery actionQuery = new MusicInfoActionQuery();
		actionQuery.setMusicUserId(tokenUserInfoDTO.getUserId());
		Integer goodCount = musicInfoActionService.findCountByParam(actionQuery);
		tokenUserInfoDTO.setGoodCount(goodCount);

		return getSuccessResponseVO(tokenUserInfoDTO);
	}

	@RequestMapping("/logout")
	public ResponseVO logout() {
		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO();
		if(tokenUserInfoDTO==null){
			throw new BusinessException(ResponseCodeEnum.CODE_901);
		}

		redisComponent.cleanTokenInfo(tokenUserInfoDTO.getToken());
		return getSuccessResponseVO("退出登录成功");
	}
}