package com.easymusic.controller;

import com.easymusic.entity.po.ProductInfo;
import com.easymusic.entity.query.ProductInfoQuery;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.service.ProductInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@RestController
@Slf4j
@Validated
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductInfoController extends ABaseController{

	private final ProductInfoService productInfoService;

	@RequestMapping("/loadProduct")
	public ResponseVO loadProduct(){
		ProductInfoQuery query = new ProductInfoQuery();
		query.setOrderBy("p.sort asc");
		List<ProductInfo> productInfoList = productInfoService.findListByParam(query);

		return getSuccessResponseVO(productInfoList);
	}

	@RequestMapping("/saveProduct")
	public ResponseVO saveProduct(MultipartFile coverFile,ProductInfo productInfo){
		productInfoService.saveProduct(coverFile,productInfo);

		return getSuccessResponseVO(null);
	}

	@RequestMapping("/changeProductSort")
	public ResponseVO changeProductSort(@NotEmpty String productIds){

		productInfoService.changeProductSort(productIds);
		return getSuccessResponseVO("改变顺序成功");
	}


	@RequestMapping("/delProduct")
	public ResponseVO delProduct(@NotEmpty String productId){
		productInfoService.deleteProductInfoByProductId(productId);
		return getSuccessResponseVO("删除成功");
	}

}