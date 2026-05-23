package com.easymusic.service;

import java.util.List;

import com.easymusic.entity.dto.PayInfoDTO;
import com.easymusic.entity.dto.TokenUserInfoDTO;
import com.easymusic.entity.query.PayOrderInfoQuery;
import com.easymusic.entity.po.PayOrderInfo;
import com.easymusic.entity.vo.PaginationResultVO;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

/**
 * 支付订单信息 业务接口
 */
public interface PayOrderInfoService {

	/**
	 * 根据条件查询列表
	 */
	List<PayOrderInfo> findListByParam(PayOrderInfoQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(PayOrderInfoQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<PayOrderInfo> findListByPage(PayOrderInfoQuery param);

	/**
	 * 新增
	 */
	Integer add(PayOrderInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<PayOrderInfo> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<PayOrderInfo> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(PayOrderInfo bean,PayOrderInfoQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(PayOrderInfoQuery param);

	/**
	 * 根据OrderId查询对象
	 */
	PayOrderInfo getPayOrderInfoByOrderId(String orderId);

	/**
	 * 根据OrderId修改
	 */
	Integer updatePayOrderInfoByOrderId(PayOrderInfo bean,String orderId);

	/**
	 * 根据OrderId删除
	 */
	Integer deletePayOrderInfoByOrderId(String orderId);

	public PayInfoDTO getPayInfo(TokenUserInfoDTO tokenUserInfoDTO, String productId, Integer payType);

	//通过支付码来购买
    void buyByPayCode(String productId, String payCode, String userId);

    Integer havePay(String orderId, String userId);
    
    /**
     * 检查支付订单状态
     * @param orderId
     * @return
     */
    boolean checkPayOrder(String orderId);

    /**
     * 检查是否有有效期内待付款订单
     * @param tokenUserInfoDTO
     * @return
     */
    PayInfoDTO checkPendingOrder(TokenUserInfoDTO tokenUserInfoDTO);

}