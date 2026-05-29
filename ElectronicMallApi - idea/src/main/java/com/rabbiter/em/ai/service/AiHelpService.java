package com.rabbiter.em.ai.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 智能帮助服务
 * 提供常见问题解答、操作指导、售后服务等功能
 */
@Service
public class AiHelpService {
    
    /**
     * 常见问题库
     */
    private static final Map<String, List<FAQ>> FAQ_DATABASE = new HashMap<>();
    
    static {
        // 订单相关 FAQ
        List<FAQ> orderFaqs = new ArrayList<>();
        orderFaqs.add(new FAQ("如何查看我的订单？", "您可以通过以下方式查看订单：\n1. 点击页面右上角的'我的订单'\n2. 在 AI 助手中输入'我的订单'\n3. 在个人中心查看订单列表", "order"));
        orderFaqs.add(new FAQ("订单状态有哪些？", "订单状态包括：\n- 待支付：订单已创建，等待支付\n- 已支付：支付成功，等待发货\n- 已发货：商品已发出，等待收货\n- 已收货：您已确认收货\n- 已取消：订单已取消\n- 退款中：正在处理退款", "order"));
        orderFaqs.add(new FAQ("如何取消订单？", "取消订单的方法：\n1. 在订单列表中找到要取消的订单\n2. 点击'取消订单'按钮\n3. 选择取消原因并确认\n注意：只有'待支付'和'已支付'状态的订单可以取消", "order"));
        orderFaqs.add(new FAQ("订单发货后多久能收到？", "发货后配送时间：\n- 同城配送：1-2 天\n- 省内城市：2-3 天\n- 跨省城市：3-5 天\n- 偏远地区：5-7 天\n具体以物流公司实际配送时间为准", "order"));
        FAQ_DATABASE.put("order", orderFaqs);
        
        // 支付相关 FAQ
        List<FAQ> paymentFaqs = new ArrayList<>();
        paymentFaqs.add(new FAQ("支持哪些支付方式？", "我们支持以下支付方式：\n- 支付宝\n- 微信支付\n- 银联卡支付\n- 余额支付\n您可以在支付页面选择任意一种支付方式", "payment"));
        paymentFaqs.add(new FAQ("支付失败怎么办？", "支付失败的解决方法：\n1. 检查网络连接是否正常\n2. 确认账户余额充足\n3. 检查银行卡是否过期\n4. 尝试更换支付方式\n5. 联系银行确认是否有限额\n如仍无法解决，请联系客服", "payment"));
        paymentFaqs.add(new FAQ("如何申请退款？", "申请退款流程：\n1. 进入订单详情页\n2. 点击'申请退款'按钮\n3. 选择退款原因和退款金额\n4. 上传相关凭证（如有）\n5. 提交申请等待审核\n退款将在 1-3 个工作日内处理", "payment"));
        paymentFaqs.add(new FAQ("退款多久能到账？", "退款到账时间：\n- 支付宝/微信：1-3 个工作日\n- 银行卡：3-7 个工作日\n- 余额支付：即时到账\n具体到账时间以银行或支付平台为准", "payment"));
        FAQ_DATABASE.put("payment", paymentFaqs);
        
        // 商品相关 FAQ
        List<FAQ> goodsFaqs = new ArrayList<>();
        goodsFaqs.add(new FAQ("商品是正品吗？", "我们承诺：\n- 所有商品均为正品\n- 支持专柜验货\n- 假一赔十\n- 提供正规发票\n请放心购买", "goods"));
        goodsFaqs.add(new FAQ("如何查看商品详情？", "查看商品详情的方法：\n1. 在商品列表点击商品图片\n2. 在 AI 助手中输入商品名称\n3. 搜索商品后点击查看详情\n详情页包含商品图片、规格、价格、库存等信息", "goods"));
        goodsFaqs.add(new FAQ("商品有货吗？", "查看库存的方法：\n1. 在商品详情页查看库存数量\n2. 在 AI 助手中输入'XX 商品有货吗'\n3. 加入购物车时会检查库存\n如显示缺货，可点击'到货通知'", "goods"));
        goodsFaqs.add(new FAQ("可以修改订单中的商品吗？", "订单商品修改规则：\n- 待支付状态：可以取消订单重新下单\n- 已支付状态：需要联系客服处理\n- 已发货状态：无法修改，可申请退货\n建议在支付前仔细核对订单信息", "goods"));
        FAQ_DATABASE.put("goods", goodsFaqs);
        
        // 售后相关 FAQ
        List<FAQ> afterSalesFaqs = new ArrayList<>();
        afterSalesFaqs.add(new FAQ("支持七天无理由退货吗？", "我们的退货政策：\n- 支持七天无理由退货\n- 商品需保持完好，不影响二次销售\n- 包装、配件、赠品需齐全\n- 定制商品、虚拟商品等不支持\n退货前请先联系客服确认", "after_sales"));
        afterSalesFaqs.add(new FAQ("如何申请售后服务？", "申请售后的方式：\n1. 在订单详情页点击'申请售后'\n2. 在 AI 助手中输入'售后'\n3. 拨打售后客服热线\n4. 联系在线客服\n请提供订单号和问题描述", "after_sales"));
        afterSalesFaqs.add(new FAQ("商品有质量问题怎么办？", "质量问题处理流程：\n1. 拍照或录像保留证据\n2. 在订单详情页申请售后\n3. 选择'质量问题'作为原因\n4. 上传相关凭证\n5. 等待客服审核\n我们会承担往返运费", "after_sales"));
        afterSalesFaqs.add(new FAQ("保修期是多久？", "商品保修政策：\n- 电子产品：全国联保 1 年\n- 家用电器：保修 1-3 年不等\n- 服装鞋帽：质量问题 7 天内可退换\n- 其他商品：按国家三包政策执行\n具体以商品详情页说明为准", "after_sales"));
        FAQ_DATABASE.put("after_sales", afterSalesFaqs);
        
        // 账户相关 FAQ
        List<FAQ> accountFaqs = new ArrayList<>();
        accountFaqs.add(new FAQ("如何注册账号？", "注册账号的方法：\n1. 点击页面右上角'注册'按钮\n2. 输入手机号获取验证码\n3. 设置登录密码\n4. 同意用户协议并注册\n注册后可使用手机号和密码登录", "account"));
        accountFaqs.add(new FAQ("忘记密码怎么办？", "找回密码的步骤：\n1. 在登录页面点击'忘记密码'\n2. 输入注册手机号\n3. 获取并输入验证码\n4. 设置新密码\n5. 使用新密码重新登录\n建议妥善保管密码", "account"));
        accountFaqs.add(new FAQ("如何修改个人信息？", "修改个人信息：\n1. 进入个人中心\n2. 点击'编辑资料'\n3. 修改昵称、头像等信息\n4. 点击保存\n部分信息（如手机号）需要验证后修改", "account"));
        FAQ_DATABASE.put("account", accountFaqs);
        
        // 配送相关 FAQ
        List<FAQ> deliveryFaqs = new ArrayList<>();
        deliveryFaqs.add(new FAQ("配送范围有哪些？", "我们的配送范围：\n- 全国大部分地区可配送\n- 港澳台地区暂不支持\n- 偏远地区可能延迟\n- 特殊时期（如春节）可能调整\n具体以下单时显示的配送信息为准", "delivery"));
        deliveryFaqs.add(new FAQ("运费是多少？", "运费标准：\n- 订单满 99 元包邮\n- 不满 99 元收取 10 元运费\n- 特殊商品（如大件）另计\n- 偏远地区运费可能上浮\n具体运费在结算页面显示", "delivery"));
        deliveryFaqs.add(new FAQ("可以指定配送时间吗？", "配送时间说明：\n- 暂不支持指定具体时间\n- 工作日和周末均可配送\n- 节假日配送可能延迟\n- 特殊要求可备注说明\n快递员会在配送前联系您", "delivery"));
        FAQ_DATABASE.put("delivery", deliveryFaqs);
    }
    
    /**
     * 根据问题获取答案
     * @param category 分类
     * @param question 问题
     * @return FAQ 答案
     */
    public FAQ getAnswer(String category, String question) {
        List<FAQ> faqs = FAQ_DATABASE.get(category);
        if (faqs == null || faqs.isEmpty()) {
            return null;
        }
        
        // 简单匹配：查找包含关键词的 FAQ
        for (FAQ faq : faqs) {
            if (question.contains(faq.getQuestion()) || faq.getQuestion().contains(question)) {
                return faq;
            }
        }
        
        // 返回第一个作为默认
        return faqs.get(0);
    }
    
    /**
     * 获取某个分类的所有 FAQ
     */
    public List<FAQ> getFaqsByCategory(String category) {
        return FAQ_DATABASE.getOrDefault(category, new ArrayList<>());
    }
    
    /**
     * 搜索 FAQ
     */
    public List<FAQ> searchFaqs(String keyword) {
        List<FAQ> results = new ArrayList<>();
        
        for (List<FAQ> faqs : FAQ_DATABASE.values()) {
            for (FAQ faq : faqs) {
                if (faq.getQuestion().contains(keyword) || faq.getAnswer().contains(keyword)) {
                    results.add(faq);
                }
            }
        }
        
        return results;
    }
    
    /**
     * 获取所有分类
     */
    public Set<String> getCategories() {
        return FAQ_DATABASE.keySet();
    }
    
    /**
     * FAQ 实体类
     */
    public static class FAQ {
        private String question;
        private String answer;
        private String category;
        
        public FAQ() {}
        
        public FAQ(String question, String answer, String category) {
            this.question = question;
            this.answer = answer;
            this.category = category;
        }
        
        public String getQuestion() {
            return question;
        }
        
        public void setQuestion(String question) {
            this.question = question;
        }
        
        public String getAnswer() {
            return answer;
        }
        
        public void setAnswer(String answer) {
            this.answer = answer;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
    }
}
