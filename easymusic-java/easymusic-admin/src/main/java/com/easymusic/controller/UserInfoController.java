package com.easymusic.controller;

import com.easymusic.entity.enums.UserIntegralRecordTypeEnum;
import com.easymusic.entity.po.UserInfo;
import com.easymusic.entity.query.UserInfoQuery;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.service.UserInfoService;
import com.easymusic.service.UserIntegralRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/user")
@Slf4j
@Validated
@RequiredArgsConstructor
public class UserInfoController extends ABaseController {

    private final UserInfoService userInfoService;

    private final UserIntegralRecordService userIntegralRecordService;

    @RequestMapping("/loadUser")
    public ResponseVO loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("u.create_time desc");
        PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/changeUserStatus")
    public ResponseVO changeUserStatus(@NotEmpty String userId, @NotNull Integer status) {
        UserInfo updateInfo = new UserInfo();
        updateInfo.setStatus(status);
        userInfoService.updateUserInfoByUserId(updateInfo, userId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/changeIntegral")
    public ResponseVO changeIntegral(@NotEmpty String userId, @NotNull Integer integral) {
        userIntegralRecordService.changeUserIntegral(integral < 0 ? UserIntegralRecordTypeEnum.ADMIN_DEDUCT : UserIntegralRecordTypeEnum.ADMIN_ADD, null, userId,
                integral, null);
        return getSuccessResponseVO(null);
    }
}
