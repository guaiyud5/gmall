package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.config.JwtUtil;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    private UserService userService;
    @Value("${token.key}")
    private String key;

    /**
     * 登录功能的实现。在进行登录之后要跳转到上一个页面
     * 此外，由于数据库中的密码是加密的，所以要对页面中输入的密码进行加密处理
     *
     * @return
     */
    // 获取用户点击的url 后面的参数
    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    //获取表单提交的数据
    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){

        UserInfo info = userService.login(userInfo);
        if(info != null){
            //data ---token
            Map<String, Object> map = new HashMap<>();
            map.put("userId",info.getId());
            map.put("nickName",info.getNickName());
            String salt = request.getHeader("X-forwarded-for");
            String token = JwtUtil.encode(key, map, salt);
            return token;
        }
        return "fail";
    }


    // 直接将token ，salt 以参数的形式传入到控制器
    // http://passport.atguigu.com/verify?token=xxxx&salt=xxx
    @RequestMapping("verify")
    @ResponseBody
    public String verity(HttpServletRequest request){
        //request.getparameter表示从url中获取数据
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");
        Map<String, Object> map = JwtUtil.decode(token,key,salt);
        if(map != null && map.size()>0){
            //检查redis信息
            String userId = (String) map.get("userId");
            UserInfo userInfo = userService.verify(userId);
            if(userInfo != null){
                return "success";
            }
        }
        return "fail";
    }
}
