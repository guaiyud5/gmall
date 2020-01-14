package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Resource
    private CartInfoMapper cartInfoMapper;
    @Resource
    private RedisUtil redisUtil;
    @Reference
    private ManageService manageService;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        /*
            1.先查看数据库中是否有该商品
                select * from cartInfo where skuId=？and userId=?
                true :进行数量相加
                false：直接添加到数据库
            2.更新redis
         */
        //获取redis
        Jedis jedis = redisUtil.getJedis();
        //定义key
        //数控类型：jedis.hset(key,field,value)
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        if(!jedis.exists(cartKey)){
            // 加载数据到缓存！
            loadCartCache(userId);
        }
        //到数据库查询是否有该商品
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userId).andEqualTo("skuId",skuId);
        CartInfo cartInfoExist = cartInfoMapper.selectOneByExample(example);


        if(cartInfoExist != null){
            //说明数据库中有该商品，直接进行数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //初始化skuprice  防止为空
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            //更新数据库
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
            //跟新redis
        }else {
            //直接添加到数据库
            //添加数据来源。商品详情中的数据
            //根据skuId查询商品信息
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();

            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setSkuId(skuId);
            cartInfo1.setUserId(userId);
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            //添加到数据库
            cartInfoMapper.insertSelective(cartInfo1);
            //更新redis 令 cartInfoExist = cartInfo1;实现更新redis的代码共用
        }
        //更新redis
        jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));
        //设置过期时间
        setCartKeyExpire(userId, jedis, cartKey);
        jedis.close();
    }

    //根据用户id查询数据库并放入缓存
    @Override
    public List<CartInfo> loadCartCache(String userId) {
        //查询最新的数据缓存
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if(cartInfoList == null || cartInfoList.size() == 0){
            return null;
        }
        //循环遍历数据并将数据添加到缓存
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        Map<String,String> map = new HashMap<>();
        for(CartInfo cartInfo :cartInfoList){
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        jedis.hmset(cartKey,map);
        jedis.close();
        return cartInfoList;
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        /*
            1.获取redis中的购物车数据
            2.如果redis中没有，从mysql获取并放入redis中
         */
        List<CartInfo> cartInfoList = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();
        //定义key user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        List<String> stringList = jedis.hvals(cartKey);
        if(stringList != null && stringList.size()>0){
            for (String cartJson : stringList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            //按照cartInfo的id进行排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    //此处的id为string类型，按照string类型进行排序
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else {
            //走db --- 放入redis
            cartInfoList = loadCartCache(userId);
            return  cartInfoList;
        }
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartInfoArrayList, String userId) {
        //获取到登录时的购物车数据
        List<CartInfo> cartInfoListLogin = cartInfoMapper.selectCartListWithCurPrice(userId);

        if (cartInfoListLogin != null && cartInfoListLogin.size()>0){
            for (CartInfo cartInfoNoLogin : cartInfoArrayList) {
                //声明一个boolean类型变量(来判断是否有相同的商品)
                boolean isMatch = false;
                //如果数据库中一条数据都没有
                for (CartInfo cartInfoLogin : cartInfoListLogin) {
                    if(cartInfoNoLogin.getSkuId().equals(cartInfoLogin.getSkuId())){
                        //数量合并
                        cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum());
                        //更新数据库
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfoLogin);
                        isMatch=true;
                    }
                }

                //没有相同的商品
                if(!isMatch){
                    //直接添加到数据库
                    cartInfoNoLogin.setId(null);
                    cartInfoNoLogin.setUserId(userId);
                    cartInfoMapper.insertSelective(cartInfoNoLogin);
                }
            }
        }else {
            // 数据库为空！直接添加到数据库！
            for (CartInfo cartInfo : cartInfoArrayList) {
                cartInfo.setId(null);
                cartInfo.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfo);
            }
        }
        //更新redis(合并之后的汇总)
        List<CartInfo> cartInfoList = loadCartCache(userId);
        //判断状态合并购物车
        if (cartInfoList != null && cartInfoList.size()>0){
            for (CartInfo cartInfoLogin : cartInfoListLogin) {
                for (CartInfo cartInfoNoLogin : cartInfoArrayList) {
                    //判断商品相同
                    if(cartInfoNoLogin.getSkuId() == cartInfoLogin.getSkuId()){
                        //更改勾选框状态
                        if("1".equals(cartInfoNoLogin.getIsChecked())){
                            //选择未登录时的勾选状态作为合并后的勾选状态
                            cartInfoLogin.setIsChecked("1");
                            //调用选中的方法
                            checkCart("1",userId,cartInfoLogin.getSkuId());
                        }
                    }
                }
            }
        }
        return cartInfoList;
    }

    @Override
    public void deleteCartList(String userTempId) {
        //删除未登录购物车数据：redis + mysql

        //1. 先删除表中数据
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userTempId);
        cartInfoMapper.deleteByExample(example);

        //2. 删除缓存中的数据
        Jedis jedis = redisUtil.getJedis();
        //定义key ：user:userTemId:cart
        String cartKey = CartConst.USER_KEY_PREFIX+userTempId+CartConst.USER_CART_KEY_SUFFIX;
        jedis.del(cartKey);
    }

    @Override
    public void checkCart(String ischecked, String userId, String skuId) {
        /*
            1.修改数据库
            2.修改缓存
            对于缓存修改的两种方案：
                一：直接修改缓存，将缓存中的数据进行覆盖
                二：先删除缓存，在将数据添加到缓存中去
         */
        //方案一：直接删除缓存
        //1.修改数据库
        //sql语句：update cartInfo set ischecked=? where userId=? and skuId=?
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(ischecked);
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userId).andEqualTo("skuId",skuId);
        cartInfoMapper.updateByExampleSelective(cartInfo,example);

        //2.直接修改缓存进行覆盖
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        String cartJson = jedis.hget(cartKey, skuId);
        //将json转化为对象
        CartInfo cartInfoJson = JSON.parseObject(cartJson, CartInfo.class);
        cartInfoJson.setIsChecked(ischecked);
        String strJson = JSON.toJSONString(cartInfoJson);
        jedis.hset(cartKey,skuId,strJson);
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        //从缓存中获取已勾选的购物车数据
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        List<CartInfo> cartInfoList = new ArrayList<>();
        List<String> stringList = jedis.hvals(cartKey);
        //判断缓存中的购物车数据是否存在
        if (stringList != null && stringList.size()>0){
            //对购物车数据循环遍历获取勾选的购物车数据
            for (String cartJson : stringList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                //筛选购物车ischecked=1的购物车数据
                if ("1".equals(cartInfo.getIsChecked())){
                    cartInfoList.add(cartInfo);
                }

            }
        }
        jedis.close();
        return cartInfoList;
    }

    private void setCartKeyExpire(String userId, Jedis jedis, String cartKey) {
        // 设置过期时间 key = { 根据用户的购买力！ 根据用户的过期时间设置购物车的过期时间}
        // 获取用户的key
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        if(jedis.exists(userKey)){
            //获取用户的过期时间
            Long ttl = jedis.ttl(userKey);

            //将用户的过期时间给购物车的过期时间
            jedis.expire(cartKey,ttl.intValue());
        }else {
            jedis.expire(cartKey,7*24*3600);
        }
    }
}
