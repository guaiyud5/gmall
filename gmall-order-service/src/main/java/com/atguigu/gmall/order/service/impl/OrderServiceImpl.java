package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.StreamUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.mockito.internal.matchers.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderInfoMapper orderInfoMapper;
    @Resource
    private OrderDetailMapper orderDetailMapper;
    @Resource
    private RedisUtil redisUtil;

    @Resource
    private ActiveMQUtil activeMQUtil;

    //涉及到多张表的添加数据库，要添加事务
    @Transactional
    @Override
    public String saveOrder(OrderInfo orderInfo) {
        // 设置创建时间
        orderInfo.setCreateTime(new Date());
        // 设置失效时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        // 生成第三方支付编号
        String outTradeNo="ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfoMapper.insertSelective(orderInfo);

        //插入订单详细信息、
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }

        // 为了跳转到支付页面使用。支付会根据订单id进行支付。
        String orderId = orderInfo.getId();
        return orderId;
    }

    //生成流水号
    @Override
    public String getTradeNo(String userId) {
        //获取redis
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String tradeNoKey = "uder:"+userId+":tradeCode";
        //生成流水号
        String tradeCode = UUID.randomUUID().toString().replace("-","");
        //放入缓存
        jedis.setex(tradeNoKey,10*60,tradeCode);
        jedis.close();
        return tradeCode;
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }


    @Override
    public OrderInfo getOrderInfo(String orderId) {

        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);

        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;

    }

    //根据订单id修改订单状态
    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());

        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = activeMQUtil.getConnection();
        String orderJson = initWareOrder(orderId);
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(order_result_queue);

            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(orderJson);
            session.commit();
            session.close();
            producer.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public String initWareOrder(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }

    //设置初始化仓库信息方法
    //将orderInfo部分字段转换为map
    @Override
    public Map initWareOrder(OrderInfo orderInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","过年买年货");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId());

        //声明一个集合来储存订单明细数据
        List<Map> mapArrayList = new ArrayList<>();
        //获取所有的订单明细集合
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // {skuId:101,skuNum:1,skuName:’小米手64G’} {skuId:201,skuNum:1,skuName:’索尼耳机’}
            HashMap<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId",orderDetail.getSkuId());
            orderDetailMap.put("skuNum",orderDetail.getSkuNum());
            orderDetailMap.put("skuName",orderDetail.getSkuName());
            mapArrayList.add(orderDetailMap);
        }
        // 订单明细放入map
        map.put("details",mapArrayList);

        return map;
    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        //存储子订单集合
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
         /*
            1.  得到原始订单
                // [{"wareId":"1","skuIds":["33"]},{"wareId":"2","skuIds":["36","36"]}]
                // [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
            2.  根据wareSkuMap 得到仓库id 与商品的对照关系， wareSkuMap 转换成我们能够操作的对象
            3.  生成新的子订单 ，给子订单赋值
            4.  将每个子订单放入集合中subOrderInfoList
            5.  保存子订单
            6.  原始订单状态变成已拆分
         */
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);

        //将数据转化为list<Map>
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);

        if (maps != null && maps.size()>0){
            //循环获取里面的数据
            for (Map map : maps) {
                //获取仓库id
                String wareId = (String) map.get("wareId");

                //获取每个仓库id中所有订的商品id
                List<String> skuIds = (List<String>) map.get("skuIds");

                //生成新的子订单
                OrderInfo subOrderInfo = new OrderInfo();
                //属性赋值
                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                //将id设置为null
                subOrderInfo.setId(null);

                //赋值父id
                subOrderInfo.setParentOrderId(orderId);

                //赋值仓库
                subOrderInfo.setWareId(wareId);

                //获取每个仓库中的商品总价格 先获取到原始订单的明细，然后根据仓库中的Id 与原始订单明细的Id 进行比较
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();

                //声明一个集合来储存子订单的明细集合
                List<OrderDetail> subOrderDetails = new ArrayList<>();
                for (OrderDetail orderDetail : orderDetailList) {
                    for (String skuId : skuIds) {
                        if (orderDetail.getSkuId().equals(skuId)){
                            subOrderDetails.add(orderDetail);
                        }
                    }
                }

                // 1订单：n订单明细
                subOrderInfo.setOrderDetailList(subOrderDetails);
                // 价格重新赋值
                subOrderInfo.sumTotalAmount();

                // 将子订单放入子订单集合
                subOrderInfoList.add(subOrderInfo);

                // 保存子订单
                saveOrder(subOrderInfo);
            }
        }
        // 修改原始订单状态！
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        return subOrderInfoList;
    }


    @Override
    public boolean checkTradeCode(String userId, String tradeNo) {
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String tradeNoKey = "uder:"+userId+":tradeCode";
        //获取流水号
        String tradeCode = jedis.get(tradeNoKey);
        jedis.close();
//        if(tradeCode != null && tradeCode.equals(tradeNo)){
//            return true;
//        }else {
//            return false;
//        }
        return tradeCode.equals(tradeNo);
    }

    @Override
    public void delTradeNo(String userId) {
        //获取redis中的流水号
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = "uder:"+userId+":tradeCode";
        String tradeNo = jedis.get(tradeNoKey);

//        jedis.del(tradeNoKey);
        String script = "if redis call ('get',KEYS[1] == ARGV[1] then return redis.call('del',KEYS[1])) else return 0 end";
        jedis.eval(script,Collections.singletonList(tradeNoKey),Collections.singletonList(tradeNo));
        jedis.close();
    }
}
