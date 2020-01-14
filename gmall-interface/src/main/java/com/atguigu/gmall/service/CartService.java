package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    /**
     * 添加购物车
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void addToCart(String skuId, String userId, Integer skuNum);

    /**
     *
     * @param userTemId
     * @return
     */
    List<CartInfo> getCartList(String userTemId);

    /**
     * 进行购物车的合并
     * @param cartInfoArrayList
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartInfoArrayList, String userId);

    /**
     * 删除未登录购物车数据
     */
    void deleteCartList(String userTemId);

    /**
     * 修改用户的勾选状态
     * @param ischecked
     * @param userId
     * @param skuId
     */
    void checkCart(String ischecked, String userId, String skuId);

    /**
     * 获取选中的购物车数据
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 更新缓存
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
