package com.easymusic.controller;

import com.easymusic.entity.config.AppConfig;
import com.easymusic.entity.dto.TokenUserInfoDTO;
import com.easymusic.entity.vo.CheckCodeVO;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.exception.BusinessException;
import com.easymusic.redis.RedisComponent;
import com.easymusic.service.UserInfoService;
import com.easymusic.utils.StringTools;
import com.wf.captcha.ArithmeticCaptcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * 用户信息 Controller
 */
@RestController
@Slf4j
@Validated
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController extends ABaseController{

	private final RedisComponent redisComponent;
	private final AppConfig appConfig;

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

	@RequestMapping(value = "/login")
	public ResponseVO login(
							@NotEmpty String checkCodeKey,
							@NotEmpty String account,
							@NotEmpty String password,
							@NotEmpty String checkCode,
							HttpSession session) {

		if(redisComponent.getCheckCode(checkCodeKey)!=null&&!redisComponent.getCheckCode(checkCodeKey).equals(checkCode)){
			throw new BusinessException("图片验证码不正确");
		}

		if(!account.equals(appConfig.getAdminAccount())||!password.equals(StringTools.encodeByMD5(appConfig.getAdminPassword()))){
			throw new BusinessException("用户名或密码错误");
		}
		session.setAttribute("adminUser", account);
		session.setAttribute("adminLoginTime", System.currentTimeMillis());
		session.setMaxInactiveInterval(30 * 60);  //tomcat默认的过期时间是30分钟
		TokenUserInfoDTO tokenUserInfoDTO = new TokenUserInfoDTO();
		tokenUserInfoDTO.setAccount(account);

		return getSuccessResponseVO(tokenUserInfoDTO);

	}

	@RequestMapping("/logout")
	public ResponseVO logout(HttpSession session){
		// 或者使整个会话失效
		session.invalidate();
		return getSuccessResponseVO("退出登录成功");
	}
}