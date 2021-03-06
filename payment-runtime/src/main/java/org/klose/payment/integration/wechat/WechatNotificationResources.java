package org.klose.payment.integration.wechat;


import org.apache.commons.io.IOUtils;
import org.jdom.JDOMException;
import org.klose.payment.common.exception.GeneralRuntimeException;
import org.klose.payment.common.utils.Assert;
import org.klose.payment.common.utils.JSONHelper;
import org.klose.payment.common.utils.LogUtils;
import org.klose.payment.common.utils.XMLUtils;
import org.klose.payment.dao.AccountDao;
import org.klose.payment.dao.TransactionDao;
import org.klose.payment.integration.wechat.constant.WeChatConstant;
import org.klose.payment.po.AccountPO;
import org.klose.payment.po.TransactionPO;
import org.klose.payment.service.AccountService;
import org.klose.payment.service.notification.ProcessNotificationService;
import org.klose.payment.common.utils.sign.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Path("payment/wechat")
@Component
public class WechatNotificationResources {

    @Autowired
    AccountService accountService;

    @Autowired
    AccountDao accountDao;

    @Autowired
    TransactionDao transactionDao;

    @Autowired
    ProcessNotificationService notificationService;


    private final static String WECHAT_RESPONSE_MSG =
            "<xml><return_code><![CDATA[%s]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @SuppressWarnings("unchecked")
    @POST
    @Path("/notification")
    public Response handlePaymentCallback(@Context HttpServletRequest request) {

        String encoding = request.getCharacterEncoding();
        Map<String, String> params = parseRequestData(request);
        String notifyData = params.get("xml");

        Map<String, String> notifyParams;
        try {
            notifyParams = XMLUtils.parseToMap(notifyData, encoding);
            logger.debug("notifyParams : {}", LogUtils.getMapContent(notifyParams));

        } catch (JDOMException | IOException e1) {
            throw new GeneralRuntimeException("exception occurs by parsing response");
        }

        Assert.isNotNull(notifyParams);
        String paymentResult = notifyParams.get("return_code");
        String orderNo = notifyParams.get("out_trade_no");

        TransactionPO transaction = transactionDao.findByTransactionNo(orderNo);
        AccountPO account = accountDao.findByAccountNo(transaction.getAccountNo());
        Map<String, Object> accountConfig =
                (Map<String, Object>) JSONHelper.parse(account.getConfigData());

        if (verifySign(notifyParams,
                (String) accountConfig.get(WeChatConstant.KEY_WEIXIN_SECURITY),
                encoding != null ? encoding : "UTF-8") &&
                orderNo != null) {
            notificationService.handlePaymentCallback(
                    orderNo, notifyData, notifyParams.get("transaction_id"), "", null);
        }

        String return_msg = String.format(WECHAT_RESPONSE_MSG, paymentResult);
        return Response.status(Response.Status.OK).entity(return_msg).type(MediaType.APPLICATION_XML_TYPE).build();
    }

    private Map parseRequestData(HttpServletRequest request) {
        Map<String, String> resultMap = new HashMap<>();

        try {
            InputStream is = request.getInputStream();
            resultMap.put("xml", IOUtils.toString(is));
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        for (String key : resultMap.keySet())
            logger.debug("key = {}, value = {}", key, resultMap.get(key));

        return resultMap;
    }

    @SuppressWarnings("rawtypes")
    private boolean verifySign(Map params, String securityKey, String encoding) {
        SortedMap<String, String> sortedParams = new TreeMap<>();

        Iterator iter = params.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            sortedParams.put(key, params.get(key) != null ? params.get(key).toString().trim() : "");
        }

        StringBuilder sb = new StringBuilder();

        Set es = sortedParams.entrySet();
        iter = es.iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String k = (String) entry.getKey();
            String v = (String) entry.getValue();
            if (!"sign".equals(k) && null != v && !"".equals(v)) {
                sb.append(k).append("=").append(v).append("&");
            }
        }

        sb.append("key=").append(securityKey);

        String sign = MD5Util.MD5Encode(sb.toString(), encoding).toLowerCase();
        String tenpaySign = params.get("sign").toString().toLowerCase();

        return tenpaySign.equals(sign);
    }
}
