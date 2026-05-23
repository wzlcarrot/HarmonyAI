package com.easymusic.service.impl;

import java.util.Date;
import java.util.List;

import com.easymusic.annotation.GlobalInterceptor;
import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.dto.PayInfoDTO;
import com.easymusic.entity.dto.TokenUserInfoDTO;
import com.easymusic.entity.enums.*;
import com.easymusic.entity.po.PayCodeInfo;
import com.easymusic.entity.po.ProductInfo;
import com.easymusic.entity.po.UserInfo;
import com.easymusic.entity.query.PayCodeInfoQuery;
import com.easymusic.entity.query.ProductInfoQuery;
import com.easymusic.exception.BusinessException;
import com.easymusic.mappers.PayCodeInfoMapper;
import com.easymusic.mappers.ProductInfoMapper;

import com.easymusic.service.UserInfoService;
import com.easymusic.service.UserIntegralRecordService;
import com.easymusic.utils.DateUtil;
import com.easymusic.utils.PayUtils;
import com.easymusic.redis.RedisComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.easymusic.entity.query.PayOrderInfoQuery;
import com.easymusic.entity.po.PayOrderInfo;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.query.SimplePage;
import com.easymusic.mappers.PayOrderInfoMapper;
import com.easymusic.service.PayOrderInfoService;
import com.easymusic.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 支付订单信息 业务接口实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayOrderInfoServiceImpl implements PayOrderInfoService {

	private final PayOrderInfoMapper<PayOrderInfo, PayOrderInfoQuery> payOrderInfoMapper;

	private final ProductInfoMapper<ProductInfo, ProductInfoQuery> productInfoMapper;

	private final PayCodeInfoMapper<PayCodeInfo, PayCodeInfoQuery> payCodeInfoMapper;

	private final UserIntegralRecordService userIntegralRecordService;

	private final UserInfoService userInfoService;

	private final PayUtils payUtils;

	private final RedisComponent redisComponent;

	private final RedissonClient redissonClient;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<PayOrderInfo> findListByParam(PayOrderInfoQuery param) {
		return this.payOrderInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(PayOrderInfoQuery param) {
		return this.payOrderInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<PayOrderInfo> findListByPage(PayOrderInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<PayOrderInfo> list = this.findListByParam(param);
		PaginationResultVO<PayOrderInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(PayOrderInfo bean) {
		return this.payOrderInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<PayOrderInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.payOrderInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<PayOrderInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.payOrderInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(PayOrderInfo bean, PayOrderInfoQuery param) {
		StringTools.checkParam(param);
		return this.payOrderInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(PayOrderInfoQuery param) {
		StringTools.checkParam(param);
		return this.payOrderInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据OrderId获取对象
	 * 
	 * 使用Redis缓存 + 数据库 两层防护防止缓存穿透，支持Redis异常降级
	 * 
	 * 流程：
	 * 1. 尝试从Redis获取（带异常捕获）
	 * 2. Redis未命中或异常，使用分布式锁防止缓存击穿
	 * 3. 查询数据库
	 * 4. 更新缓存（带异常捕获）
	 */
	@Override
	public PayOrderInfo getPayOrderInfoByOrderId(String orderId) {
		// 1. 尝试从Redis获取（带异常捕获）
		PayOrderInfo orderInfo = null;
		try {
			orderInfo = redisComponent.getOrderInfo(orderId);
			if (orderInfo != null) {
				log.debug("从缓存获取订单信息, orderId={}", orderId);
				return orderInfo;
			}
		} catch (Exception e) {
			log.warn("Redis查询订单信息异常，orderId={}, 将直接查询数据库", orderId);
			// Redis异常时不做处理，继续执行数据库查询
		}

		// 2. Redis 未命中或异常，使用分布式锁防止缓存击穿
		String lockKey = "lock:order:" + orderId;
		RLock lock = redissonClient.getLock(lockKey);
		
		try {
			// 尝试获取锁，等待100ms，锁定3秒
			if (lock.tryLock(100, 3000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
				try {
					// 双重检查：再次查询 Redis（可能其他线程已经加载了）
					try {
						orderInfo = redisComponent.getOrderInfo(orderId);
						if (orderInfo != null) {
							return orderInfo;
						}
					} catch (Exception e) {
						log.warn("Redis双重检查查询异常，orderId={}", orderId);
					}
					
					// 查询数据库
					log.debug("缓存未命中，查询数据库, orderId={}", orderId);
					orderInfo = this.payOrderInfoMapper.selectByOrderId(orderId);

					// 3. 查询到数据后尝试更新缓存（带异常捕获）
					if (orderInfo != null) {
						try {
							// 根据订单状态设置不同的缓存过期时间
							// 待支付订单：10分钟过期
							// 已支付订单：1天过期
							Integer expireSeconds = PayOrderStatusEnum.NO_PAY.getStatus().equals(orderInfo.getStatus())
								? Constants.ORDER_TIMEOUT_MIN * 60
								: Constants.REDIS_KEY_EXPIRES_DAY;
							redisComponent.saveOrderInfo(orderId, orderInfo, expireSeconds);
							log.debug("订单信息查询成功并缓存, orderId={}, expireSeconds={}", orderId, expireSeconds);
						} catch (Exception e) {
							log.warn("Redis缓存订单信息异常，orderId={}, 但不影响业务流程", orderId);
							// Redis缓存失败不影响业务，只是性能降级
						}
					}
				} finally {
					// 释放锁
					if (lock.isHeldByCurrentThread()) {
						lock.unlock();
					}
				}
			} else {
				// 获取锁失败，等待一小段时间后重试 Redis
				Thread.sleep(50);
				try {
					orderInfo = redisComponent.getOrderInfo(orderId);
					// 如果还是没拿到，降级：直接查询数据库
					if (orderInfo == null) {
						orderInfo = this.payOrderInfoMapper.selectByOrderId(orderId);
						if (orderInfo != null) {
							try {
								// 根据订单状态设置不同的缓存过期时间
								Integer expireSeconds = PayOrderStatusEnum.NO_PAY.getStatus().equals(orderInfo.getStatus())
									? Constants.ORDER_TIMEOUT_MIN * 60
									: Constants.REDIS_KEY_EXPIRES_DAY;
								redisComponent.saveOrderInfo(orderId, orderInfo, expireSeconds);
							} catch (Exception e) {
								log.warn("Redis缓存订单信息异常，orderId={}, 但不影响业务流程", orderId);
							}
						}
					}
				} catch (Exception e) {
					log.warn("Redis重试查询异常，orderId={}, 将直接查询数据库", orderId);
					// 如果Redis重试也异常，直接查数据库
					orderInfo = this.payOrderInfoMapper.selectByOrderId(orderId);
					if (orderInfo != null) {
						try {
							// 根据订单状态设置不同的缓存过期时间
							Integer expireSeconds = PayOrderStatusEnum.NO_PAY.getStatus().equals(orderInfo.getStatus())
								? Constants.ORDER_TIMEOUT_MIN * 60
								: Constants.REDIS_KEY_EXPIRES_DAY;
							redisComponent.saveOrderInfo(orderId, orderInfo, expireSeconds);
						} catch (Exception e2) {
							log.warn("Redis缓存订单信息异常，orderId={}, 但不影响业务流程", orderId);
						}
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("获取订单信息锁被中断, orderId={}", orderId, e);
			// 降级：直接查询数据库
			orderInfo = this.payOrderInfoMapper.selectByOrderId(orderId);
			if (orderInfo != null) {
				try {
					// 根据订单状态设置不同的缓存过期时间
					Integer expireSeconds = PayOrderStatusEnum.NO_PAY.getStatus().equals(orderInfo.getStatus())
						? Constants.ORDER_TIMEOUT_MIN * 60
						: Constants.REDIS_KEY_EXPIRES_DAY;
					redisComponent.saveOrderInfo(orderId, orderInfo, expireSeconds);
				} catch (Exception e2) {
					log.warn("Redis缓存订单信息异常，orderId={}, 但不影响业务流程", orderId);
				}
			}
		}

		return orderInfo;
	}

	/**
	 * 根据OrderId修改
	 */
	@Override
	public Integer updatePayOrderInfoByOrderId(PayOrderInfo bean, String orderId) {
		return this.payOrderInfoMapper.updateByOrderId(bean, orderId);
	}

	/**
	 * 根据OrderId删除
	 */
	@Override
	public Integer deletePayOrderInfoByOrderId(String orderId) {
		return this.payOrderInfoMapper.deleteByOrderId(orderId);
	}


	//payType:1-支付宝支付，2-微信支付      获取订单信息
	public PayInfoDTO getPayInfo(TokenUserInfoDTO tokenUserInfoDTO, String productId, Integer payType) {
		PayInfoDTO payInfoDTO = new PayInfoDTO();

		// 获取商品信息
		ProductInfo productInfo = this.productInfoMapper.selectByProductId(productId);
		if (productInfo == null || !ProductOnSaleTypeEnum.ON_SALE.getType().equals(productInfo.getOnsaleType())) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		// 检查是否存在有效期内待付款的支付宝订单
		if (payType == 1) {
			PayOrderInfo validPendingOrder = getValidPendingAlipayOrder(tokenUserInfoDTO.getUserId());
			if (validPendingOrder != null) {
				// 存在有效期内待付款订单，直接返回该订单信息
				String qrCodeUrl = redisComponent.getQrCode(validPendingOrder.getOrderId());
				if (qrCodeUrl != null) {
					payInfoDTO.setHasValidPendingOrder(true);
					payInfoDTO.setExistingOrderId(validPendingOrder.getOrderId());
					payInfoDTO.setPayUrl(qrCodeUrl);

					// 设置商品信息（修复：确保返回完整的商品信息）
					payInfoDTO.setAmount(validPendingOrder.getAmount());
					payInfoDTO.setProductName(validPendingOrder.getProductName());
					payInfoDTO.setIntegral(validPendingOrder.getIntegral());

					// 设置过期时间（基于订单创建时间计算）
					Long expireTime = validPendingOrder.getCreateTime().getTime() + 
					                 (Constants.ORDER_TIMEOUT_MIN * 60 * 1000L);
					payInfoDTO.setExpireTime(expireTime);
					return payInfoDTO;
				}
			}
		}

		// 生成订单ID（时间戳 + 14位随机数，重复概率极低）
		String orderId = getOrderId();

		// 使用分布式锁防止并发创建订单（基于用户ID加锁，而不是订单ID）
		// 这样可以防止同一用户并发创建多个订单
		String lockKey = "lock:order:create:user:" + tokenUserInfoDTO.getUserId();
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// 尝试获取锁，等待3秒，锁定30秒
			if (lock.tryLock(3, 30, java.util.concurrent.TimeUnit.SECONDS)) {
				try {
					if (payType == 1) {
						// 支付宝支付
						try {
							String payUrl = payUtils.sendRequestToAlipay(orderId, productInfo.getPrice().floatValue(), productInfo.getProductName());

							// 验证返回的支付URL是否有效
							if (payUrl == null || payUrl.isEmpty()) {
								throw new BusinessException("创建支付宝支付订单失败：二维码生成失败");
							}

							payInfoDTO.setOrderId(orderId);
							payInfoDTO.setPayUrl(payUrl);
							payInfoDTO.setHasValidPendingOrder(false);

							// 创建预支付订单记录
							PayOrderInfo payOrderInfo = new PayOrderInfo();
							payOrderInfo.setOrderId(orderId);
							payOrderInfo.setCreateTime(new Date());
							payOrderInfo.setUserId(tokenUserInfoDTO.getUserId());
							payOrderInfo.setAmount(productInfo.getPrice());
							payOrderInfo.setProductId(productInfo.getProductId());
							payOrderInfo.setProductName(productInfo.getProductName());
							payOrderInfo.setStatus(PayOrderStatusEnum.NO_PAY.getStatus());
							payOrderInfo.setPayType(PayOrderTypeEnum.PAY_ALIPAY.getType());
							payOrderInfo.setIntegral(productInfo.getIntegral());

							int insertResult = this.payOrderInfoMapper.insert(payOrderInfo);

							if (insertResult <= 0) {
								throw new BusinessException("创建订单记录失败");
							}

							// 将二维码保存到Redis，设置过期时间为10分钟
							redisComponent.saveQrCode(orderId, payUrl, Constants.ORDER_TIMEOUT_MIN * 60);

							// 设置过期时间（基于订单创建时间计算）
							long expireTime = payOrderInfo.getCreateTime().getTime() + 
							                 (Constants.ORDER_TIMEOUT_MIN * 60 * 1000L);
							payInfoDTO.setExpireTime(expireTime);

						} catch (org.springframework.dao.DuplicateKeyException e) {
							// 订单号冲突（概率极低），直接让用户重试
							log.error("订单号冲突, orderId={}, 请用户重试", orderId, e);
							throw new BusinessException("创建订单失败，请稍后重试");
						} catch (Exception e) {
							throw new BusinessException("创建支付宝支付订单失败: " + e.getMessage());
						}
					}
				} finally {
					// 释放锁
					if (lock.isHeldByCurrentThread()) {
						lock.unlock();
					}
				}
			} else {
				throw new BusinessException("系统繁忙，请稍后重试");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BusinessException("获取锁被中断");
		}

		return payInfoDTO;
	}


	@Override
	@Transactional(rollbackFor = Exception.class)
	public void buyByPayCode(String productId, String payCode, String userId) {
		// 使用分布式锁防止支付码重复使用（基于支付码加锁）
		String lockKey = "lock:paycode:" + payCode;
		RLock lock = redissonClient.getLock(lockKey);
		
		try {
			// 尝试获取锁，等待3秒，锁定30秒
			if (lock.tryLock(3, 30, java.util.concurrent.TimeUnit.SECONDS)) {
				try {
					PayCodeInfo payCodeInfo = payCodeInfoMapper.selectByPayCode(payCode);
					if (payCodeInfo==null) {
						throw new BusinessException("支付码不正确或者已过期或者已使用");
					}
					//已使用
					if (PayCodeStatusEnum.USED.getStatus().equals(payCodeInfo.getStatus())
							|| System.currentTimeMillis() - payCodeInfo.getCreateTime().getTime() > 1000 * 60 * 30) {
						throw new BusinessException("支付码不正确或者已过期或者已使用");
					}

					ProductInfo productInfo = this.productInfoMapper.selectByProductId(productId);

					//看看商品是否是上架的
					if (productInfo == null || !ProductOnSaleTypeEnum.ON_SALE.getType().equals(productInfo.getOnsaleType())) {
						throw new BusinessException(ResponseCodeEnum.CODE_600);
					}
					//支付金额和商品价格是否一致
					if (productInfo.getPrice().compareTo(payCodeInfo.getAmount()) != 0) {
						throw new BusinessException("支付码金额与商品金额不匹配");
					}

					//生成订单
					Date curDate = new Date();
					String orderId = getOrderId();
					PayOrderInfo payOrderInfo = new PayOrderInfo();
					payOrderInfo.setOrderId(orderId);
					payOrderInfo.setCreateTime(curDate);
					payOrderInfo.setPayTime(curDate);
					payOrderInfo.setIntegral(productInfo.getIntegral());
					payOrderInfo.setUserId(userId);
					payOrderInfo.setAmount(productInfo.getPrice());
					payOrderInfo.setProductId(productInfo.getProductId());
					payOrderInfo.setProductName(productInfo.getProductName());
					payOrderInfo.setStatus(PayOrderStatusEnum.HAVE_PAY.getStatus());
					payOrderInfo.setPayType(PayOrderTypeEnum.PAY_CODE.getType());
					
					try {
						this.payOrderInfoMapper.insert(payOrderInfo);
					} catch (org.springframework.dao.DuplicateKeyException e) {
						log.warn("订单号冲突, orderId={}", orderId, e);
						throw new BusinessException("创建订单失败，请重试");
					}

					//用了付款码，后面肯定要更新付款码的信息
					PayCodeInfo updateInfo = new PayCodeInfo();
					updateInfo.setStatus(PayCodeStatusEnum.USED.getStatus());
					updateInfo.setUseUserId(userId);
					updateInfo.setUseTime(curDate);

					PayCodeInfoQuery payCodeInfoQuery = new PayCodeInfoQuery();
					payCodeInfoQuery.setPayCode(payCode);
					payCodeInfoQuery.setStatus(PayCodeStatusEnum.NO_USE.getStatus());
					//updateInfo是更新信息，payCodeInfoQuery是查询条件
					Integer updateCount = payCodeInfoMapper.updateByParam(updateInfo, payCodeInfoQuery);
					if (updateCount == 0) {
						throw new BusinessException("支付码支付失败，请联系管理员");
					}
					//更新积分
					userIntegralRecordService.changeUserIntegral(UserIntegralRecordTypeEnum.RECHARGE, payOrderInfo.getOrderId(), payOrderInfo.getUserId(),
							payOrderInfo.getIntegral(), payCodeInfo.getAmount());
				} finally {
					// 释放锁
					if (lock.isHeldByCurrentThread()) {
						lock.unlock();
					}
				}
			} else {
				throw new BusinessException("系统繁忙，请稍后重试");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BusinessException("获取锁被中断");
		}
	}

	public Integer havePay(String orderId, String userId) {
		PayOrderInfo payOrderInfo = getPayOrderInfoByOrderId(orderId);
		//查询订单状态，，也就是订单是否支付
		if (!PayOrderStatusEnum.HAVE_PAY.getStatus().equals(payOrderInfo.getStatus())) {
			return null;
		}
		UserInfo userInfo = this.userInfoService.getUserInfoByUserId(userId);
		return userInfo.getIntegral();
	}
	
	/**
	 * 检查支付订单状态
	 * @param orderId
	 * @return
	 */
	@Override
	public boolean checkPayOrder(String orderId) {
		try {
			PayOrderInfo payOrderInfo = getPayOrderInfoByOrderId(orderId);

			// 检查订单是否已超时
			if (payOrderInfo.getStatus().equals(PayOrderStatusEnum.NO_PAY.getStatus()) && !isOrderValid(payOrderInfo)) {

				// 注释：Redis数据已设置TTL自动过期，无需手动删除
				// redisComponent.deleteQrCode(orderId);

				return false;
			}

			boolean isPaid = PayOrderStatusEnum.HAVE_PAY.getStatus().equals(payOrderInfo.getStatus());
			return isPaid;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private String getOrderId(){
		return DateUtil.format(new Date(), DateTimePatternEnum.YYYYMMDDHHMMSS.getPattern())+StringTools.getRandomNumber(Constants.LENGTH_14).toLowerCase();
	}

	/**
	 * 检查是否存在有效期内待付款的支付宝订单
	 * @param userId 用户ID
	 * @return 有效期内待付款订单，如果不存在则返回null
	 */
	/**
	 * 检查是否存在有效期内待付款的支付宝订单
	 * @param userId 用户ID
	 * @return 有效期内待付款订单，如果不存在则返回null
	 */
	private PayOrderInfo getValidPendingAlipayOrder(String userId) {
		
		// 1. 先从 Redis 查用户当前待支付订单 ID
		String pendingOrderId = redisComponent.getUserPendingOrder(userId);
		if (pendingOrderId != null) {
			// 通过 Redis + MySQL 两层获取订单详情
			PayOrderInfo orderInfo = getPayOrderInfoByOrderId(pendingOrderId);
			if (orderInfo != null
					&& PayOrderStatusEnum.NO_PAY.getStatus().equals(orderInfo.getStatus())
					&& isOrderValid(orderInfo)) {
				return orderInfo;
			} else {
				// 订单不存在 / 已支付 / 已超时，清理用户维度缓存
				redisComponent.deleteUserPendingOrder(userId);
			}
		}

		PayOrderInfoQuery query = new PayOrderInfoQuery();
		query.setUserId(userId);
		query.setStatus(PayOrderStatusEnum.NO_PAY.getStatus());
		query.setPayType(PayOrderTypeEnum.PAY_ALIPAY.getType());

		// 只需要最新的一条待付款订单即可，按创建时间倒序，并限制返回 1 条
		query.setOrderBy("create_time desc");
		query.setPageNo(1);
		query.setPageSize(1);

		List<PayOrderInfo> pendingOrders = this.payOrderInfoMapper.selectList(query);
		if (pendingOrders == null || pendingOrders.isEmpty()) {
			return null;
		}

		
		PayOrderInfo latestOrder = pendingOrders.get(0);

		// 3. 回源查到有效订单后，写入 Redis 索引，TTL 10 分钟
		redisComponent.saveUserPendingOrder(
				userId,
				latestOrder.getOrderId(),
				Constants.ORDER_TIMEOUT_MIN * 60
		);

		// 检查订单是否在有效期内（10分钟内）
		return isOrderValid(latestOrder) ? latestOrder : null;
	}

	/**
	 * 检查订单是否在有效期内
	 * @param order 订单信息
	 * @return 是否有效
	 */
	private boolean isOrderValid(PayOrderInfo order) {
		if (order.getCreateTime() == null) {
			return false;
		}
		
		// 计算订单创建时间到现在的时间差（分钟）
		long createTime = order.getCreateTime().getTime();
		long currentTime = System.currentTimeMillis();
		long diffMinutes = (currentTime - createTime) / (1000 * 60);
		
		// 订单在10分钟内有效
		return diffMinutes < Constants.ORDER_TIMEOUT_MIN;
	}

	/**
	 * 检查是否有有效期内待付款订单
	 * @param tokenUserInfoDTO
	 * @return
	 */
	@Override
	public PayInfoDTO checkPendingOrder(TokenUserInfoDTO tokenUserInfoDTO) {
		PayInfoDTO payInfoDTO = new PayInfoDTO();
		
		// 检查是否存在有效期内待付款的支付宝订单
		PayOrderInfo validPendingOrder = getValidPendingAlipayOrder(tokenUserInfoDTO.getUserId());
		if (validPendingOrder != null) {
			// 存在有效期内待付款订单
			String qrCodeUrl = redisComponent.getQrCode(validPendingOrder.getOrderId());
				if (qrCodeUrl != null) {
					payInfoDTO.setHasValidPendingOrder(true);
					payInfoDTO.setExistingOrderId(validPendingOrder.getOrderId());
					payInfoDTO.setPayUrl(qrCodeUrl);
					
					// 设置商品信息
					payInfoDTO.setAmount(validPendingOrder.getAmount());
					payInfoDTO.setProductName(validPendingOrder.getProductName());
					payInfoDTO.setIntegral(validPendingOrder.getIntegral());
					
					// 设置过期时间（基于订单创建时间计算）
					Long expireTime = validPendingOrder.getCreateTime().getTime() + 
					                 (Constants.ORDER_TIMEOUT_MIN * 60 * 1000L);
					payInfoDTO.setExpireTime(expireTime);
				} else {
				// 二维码已过期（Redis TTL自动过期）
				payInfoDTO.setHasValidPendingOrder(false);
			}
		} else {
			payInfoDTO.setHasValidPendingOrder(false);
		}
		
		return payInfoDTO;
	}

}