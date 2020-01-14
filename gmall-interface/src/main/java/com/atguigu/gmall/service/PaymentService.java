package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    // 保存记录{void}并生成二维码{返回页面String} PaymentInfo
    void savePaymentInfo(PaymentInfo paymentInfo);

    // 更新交易记录中的状态
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);

    // 根据outTradeNo 查询数据
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    // 退款
    boolean refund(String orderId);

    // 微信支付
    Map createNative(String orderId, String totalAmout);

    //将订单支付结果发送到mq
    void sendPaymentResult(PaymentInfo paymentInfo, String result);
}
