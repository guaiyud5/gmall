package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

//业务层接口
public interface UserService {

    /**
     * 查询所以数据
     */
    List<UserInfo> findAll();

    /**
     * 根据用户id查询用户地址
     */
    List<UserAddress> getUserAddressByUserId(String userId);

    /**
     * 登录
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 对登录信息进行验证
     * 解密token，获取userId
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
