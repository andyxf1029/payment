package org.klose.payment.server.service.impl;


import org.klose.payment.server.bo.BillingData;
import org.klose.payment.server.common.utils.Assert;
import org.klose.payment.server.constant.PaymentConstant;
import org.klose.payment.server.service.PaymentIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service("mockPaymentIntegtationService")
public class MockPaymentIntegrationServiceImpl implements
		PaymentIntegrationService {

	private Logger logger = LoggerFactory
			.getLogger(MockPaymentIntegrationServiceImpl.class);

	@Override
	public BillingData prepareBillingData(String businessNumber) {
		Assert.isNotNull(businessNumber, "business number can not be blank");

		if (logger.isDebugEnabled())
			logger.debug("businessNumber = " + businessNumber);

		BillingData result = new BillingData();

		result.setSubject("test subject");
		result.setDescription("test payment");
		result.setBizNo(businessNumber);
		result.setPrice(BigDecimal.ONE);
		result.setCurrency(PaymentConstant.CURRENCY_CNY);
		result.setQuantity(1);
		result.setBusinessEffectiveDate(new Date());

		prepareExtensionData(result);

		if (logger.isDebugEnabled())
			logger.debug("prepared billing data : " + result);
		return result;
	}


	/**
	 * override for extension
	 * @param data
     */
	protected void prepareExtensionData(BillingData data) {
//		Assert.isNotNull(data, "billing data is null");
		data.addExtData(PaymentConstant.KEY_WEIXIN_PRODUCT_ID, "test product");

	}

	@Override
	public void saveTransactionId(String businessNumber, Long transactionId) {
		//@ TODO the tranactionId should be saved in db with the order of commodity
		logger.debug("before save transactionId. businessNumber = "
					+ businessNumber + " transactionId = " + transactionId);
//		agreementService.persistPolicyWithTransactionId(businessNumber,
//				transactionId);

	}

}