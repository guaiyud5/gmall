package com.atguigu.gmall.cart.mapper;

import com.atguigu.gmall.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CartInfoMapper extends Mapper<CartInfo> {
    //根据用户id查询购物车数据
    List<CartInfo> selectCartListWithCurPrice(String userId);


}
