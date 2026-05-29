package com.rabbiter.em.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Service
public class AlipayService {

    @Resource
    private AlipayClient alipayClient;

    /**
     * 创建支付宝支付表单
     * @param orderNo 订单号
     * @param totalAmount 支付金额
     * @param subject 订单标题
     * @return 支付表单HTML
     */
    public String createPayForm(String orderNo, String totalAmount, String subject) {
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setReturnUrl("http://localhost:5173/pay-success");
            request.setNotifyUrl("http://localhost:9191/api/alipay/notify");
            
            request.setBizContent("{" +
                    "\"out_trade_no\":\"" + orderNo + "\"," +
                    "\"total_amount\":\"" + totalAmount + "\"," +
                    "\"subject\":\"" + subject + "\"," +
                    "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"" +
                    "}");
            
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            return response.getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 查询订单支付状态
     * @param orderNo 订单号
     * @return 支付状态
     */
    public boolean queryOrderStatus(String orderNo) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
            
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            
            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
            }
            return false;
        } catch (AlipayApiException e) {
            return false;
        }
    }

    /**
     * 支付宝退款
     * @param orderNo 订单号
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @return 退款结果
     */
    public boolean refund(String orderNo, BigDecimal refundAmount, String refundReason) {
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            request.setBizContent("{" +
                    "\"out_trade_no\":\"" + orderNo + "\"," +
                    "\"refund_amount\":\"" + refundAmount + "\"," +
                    "\"refund_reason\":\"" + refundReason + "\"" +
                    "}");
            
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            
            if (response.isSuccess()) {
                return "10000".equals(response.getCode());
            }
            return false;
        } catch (AlipayApiException e) {
            return false;
        }
    }
}
