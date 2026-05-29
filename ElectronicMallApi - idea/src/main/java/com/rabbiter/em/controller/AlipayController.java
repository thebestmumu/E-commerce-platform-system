package com.rabbiter.em.controller;

import com.rabbiter.em.annotation.Authority;
import com.rabbiter.em.common.Result;
import com.rabbiter.em.entity.AuthorityType;
import com.rabbiter.em.entity.Order;
import com.rabbiter.em.service.AlipayService;
import com.rabbiter.em.service.OrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/alipay")
public class AlipayController {

    @Resource
    private AlipayService alipayService;

    @Resource
    private OrderService orderService;

    /**
     * 创建支付表单
     */
    @PostMapping("/pay")
    @Authority(AuthorityType.requireLogin)
    public Result createPayForm(@RequestBody Map<String, Object> params) {
        try {
            String orderNo = (String) params.get("orderNo");
            String totalAmount = params.get("totalAmount").toString();
            String subject = (String) params.get("subject");

            String payForm = alipayService.createPayForm(orderNo, totalAmount, subject);
            return Result.success(payForm);
        } catch (Exception e) {
            return Result.error("500", "创建支付表单失败: " + e.getMessage());
        }
    }

    /**
     * 查询订单支付状态
     */
    @GetMapping("/query/{orderNo}")
    public Result queryOrderStatus(@PathVariable String orderNo) {
        try {
            boolean isPaid = alipayService.queryOrderStatus(orderNo);
            return Result.success(isPaid);
        } catch (Exception e) {
            return Result.error("500", "查询订单状态失败: " + e.getMessage());
        }
    }

    /**
     * 支付宝异步通知回调
     * 注意：此接口不需要登录验证，支付宝服务器会直接调用
     */
    @PostMapping("/notify")
    public String notify(@RequestParam Map<String, String> params) {
        try {
            String outTradeNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");

            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                orderService.payOrder(outTradeNo);
                return "success";
            }
            return "failure";
        } catch (Exception e) {
            return "failure";
        }
    }

    /**
     * 支付宝退款
     */
    @PostMapping("/refund")
    @Authority(AuthorityType.requireAuthority)
    public Result refund(@RequestBody Map<String, Object> params) {
        try {
            String orderNo = (String) params.get("orderNo");
            BigDecimal refundAmount = new BigDecimal(params.get("refundAmount").toString());
            String refundReason = (String) params.get("refundReason");

            boolean success = alipayService.refund(orderNo, refundAmount, refundReason);
            if (success) {
                orderService.refundOrder(orderNo);
                return Result.success("退款成功");
            } else {
                return Result.error("500", "退款失败");
            }
        } catch (Exception e) {
            return Result.error("500", "退款失败: " + e.getMessage());
        }
    }
}
