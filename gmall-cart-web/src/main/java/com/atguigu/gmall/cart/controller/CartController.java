package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CookieUtil;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.sun.net.httpserver.Authenticator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    private ManageService manageService;
    @Reference
    private CartService cartService;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        String skuNum = request.getParameter("num");
        String skuId = request.getParameter("skuId");
        String userId = (String) request.getAttribute("userId");
        //判断用户是否登录
        if(userId == null){
            //用户id可能存放在cookie中
            userId = CookieUtil.getCookieValue(request, "user-key", false);
            if(userId == null){
                //用户未登录
                userId = UUID.randomUUID().toString().replace("-", "");
                CookieUtil.setCookie(request,response,"user-key",userId,7*24*3600,false);
            }
        }
        //添加购物车
        cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        //页面渲染
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartInfoList = new ArrayList<>();
        //判断用户是否登录
        if(userId != null){
            //用户已经登录
            cartInfoList = cartService.getCartList(userId);
            //合并购物车
            //1.获取cookie中的临时用户id
            String userTemId = CookieUtil.getCookieValue(request,"user-key",false);
            List<CartInfo> cartTempList = new ArrayList<>();
            if(userTemId != null){
                //2.获取临时用户的购物车记录
                cartTempList = cartService.getCartList(userTemId);
                //cartInfoList = cartService.getCartList(userTemId);
                if(cartTempList != null && cartTempList.size() >0){
                    //3.合并购物车
                    cartInfoList = cartService.mergeToCartList(cartTempList,userId);
                    //4.删除未登录购物车数据
                    cartService.deleteCartList(userTemId);
                }
            }
            //上述两个{后面的else（未登录||未登录没有购物数据）所写的逻辑代码一样，故在次数做出整合
            if (userTemId == null || (cartTempList == null || cartTempList.size() == 0)){
                //说明未登录或者未登录没有购物记录，直接获取数据库
                cartInfoList = cartService.getCartList(userId);
            }
        }else {
            //获取cookie中的临时用户id
            String userTemId = CookieUtil.getCookieValue(request,"user-key",false);
            if(userTemId != null){
                cartInfoList = cartService.getCartList(userTemId);
            }
        }
        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }

    //判断勾选状态
    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request){
        //调用服务层
        String ischecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        //获取用户id
        String userId = (String) request.getAttribute("userId");

        //判断用户状态
        //用户未登录
        if(userId == null){
            //获取临时用户id
            userId = CookieUtil.getCookieValue(request, "user-key", false);
            //修改勾选状态
        }
        //用户已登录，修改勾选状态
        cartService.checkCart(ischecked,userId,skuId);
    }

    @RequestMapping("toTrade")
    @LoginRequire
    public  String toTrade(HttpServletRequest request){
        //获取userId
        String userId = (String) request.getAttribute("userId");
        //获取临时用户id
        String userTempId = CookieUtil.getCookieValue(request, "user-key", false);
        if(!StringUtils.isEmpty(userTempId)){
            //获取未登录的购物车数据
            List<CartInfo> cartInfoListNoLogin = cartService.getCartList(userTempId);
            if (cartInfoListNoLogin != null && cartInfoListNoLogin.size()>0){
                cartService.mergeToCartList(cartInfoListNoLogin,userId);
            }
        }
        return "redirect://trade.gmall.com/trade";
    }

}
