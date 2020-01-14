package com.atguigu.gmall.manage.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import lombok.SneakyThrows;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;

    @SneakyThrows
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage){
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        if("success".equals(result)){
            //更新支付状态
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);

            //通知减库存
            orderService.sendOrderStatus(orderId);
            orderService.updateOrderStatus(orderId,ProcessStatus.DELEVERED);
        }else {
            orderService.updateOrderStatus(orderId,ProcessStatus.UNPAID);
        }
    }

    @SneakyThrows
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage){
        //获取消息
        String status = mapMessage.getString("status");
        String orderId = mapMessage.getString("orderId");

        if ("DEDUCTED".equals(status)){
            //减库存成功
            //更新订单的状态，代发货
            orderService.updateOrderStatus(orderId,ProcessStatus.WAITING_DELEVER);
        }else {
            // 减库存异常
            orderService.updateOrderStatus(orderId, ProcessStatus.STOCK_EXCEPTION);
            /*
            1.  发送一个消息队列。调用其他库存！
            2.  如果其他库存减库存成功！
             */
        }
    }
}
