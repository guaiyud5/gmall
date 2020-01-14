package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Controller
public class OrderController {

    @Reference
    private UserService userService;
    @Reference
    private CartService cartService;
    @Reference
    private OrderService orderService;
    @Reference
    private ManageService manageService;

    /**
     * 在订单页面有订单明细(需要获取选中的订单)
     * 订单地址
     * 买家信息
     * @param request
     * @return
     */
    @RequestMapping("trade")
    @LoginRequire
    public String getAddress(HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");
        //获取地址
        List<UserAddress> userAddressesList = userService.getUserAddressByUserId(userId);
        //获取购物车选中的数据
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        //为订单明细赋值
        List<OrderDetail> detailArrayList = new ArrayList<>();
        for (CartInfo cartInfo : cartInfoList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            //添加订单明细
            detailArrayList.add(orderDetail);
        }
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        orderInfo.sumTotalAmount();
        request.setAttribute("userAddressesList",userAddressesList);
        request.setAttribute("detailArrayList",detailArrayList);
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeCode",tradeNo);
        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(HttpServletRequest request,OrderInfo orderInfo){
        String userId = (String) request.getAttribute("userId");
        //参数初始化
        orderInfo.setUserId(userId);

        //判断是否重复提交
        //获取页面的流水号
        //getParameter是获取表单中name的名字
        String tradeNo = request.getParameter("tradeNo");
        //调用比较方法验证流水号
        boolean flag = orderService.checkTradeCode(userId,tradeNo);
        //验证失败
        if(!flag){
            request.setAttribute("errMsg","该页面失效，请重新结算");
            return "tradeFail";
        }


        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            //校验库存
            boolean result = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
            if(!result){
                request.setAttribute("errMsg",orderDetail.getSkuName() + "商品库存不足，请重新结算");
                return "tradeFail";
            }
            //校验价格
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            int res = skuInfo.getPrice().compareTo(orderDetail.getOrderPrice());
            if(res != 0){
                request.setAttribute("errMsg",orderDetail.getSkuName() + "商品价格有变动，请重新结算");
                //更新价格并加载到缓存中
                cartService.loadCartCache(userId);
                return "tradeFail";
            }

        }

        //保存订单
        String orderId = orderService.saveOrder(orderInfo);

        //删除tradeNo
        orderService.delTradeNo(userId);
        //重定向的到支付界面
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        // [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        String wareSkuMap = request.getParameter("wareSkuMap");

        //调用服务层的拆单方法
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(orderId,wareSkuMap);

        //声明一个存储map的集合
        List<Map> mapArrayList = new ArrayList<>();
        //循环子订单获取订单中的惊悚字符串
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map map = orderService.initWareOrder(orderInfo);
            mapArrayList.add(map);
        }
        return JSON.toJSONString(mapArrayList);
    }


}
