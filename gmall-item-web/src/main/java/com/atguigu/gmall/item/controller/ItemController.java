package com.atguigu.gmall.item.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuAttrValue;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class ItemController {

    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;

    @RequestMapping("{skuId}.html")
    @LoginRequire(autoRedirect = false)
    public String skuInfoPage(@PathVariable("skuId") String skuId, HttpServletRequest httpServletRequest){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //保存skuInfo给页面渲染
        httpServletRequest.setAttribute("skuInfo",skuInfo);

        //根据spuId查询销售属性值与skuid组合的数据集合
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        //拼接规则：1.skuId相同进行拼接，skuId不同停止拼接
        //          2.循环到末尾停止拼接
        String key = "";
        Map<String, String> map = new HashMap<>();
        if(skuSaleAttrValueList != null && skuSaleAttrValueList.size()>0 ){
            for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
                //得到每个value值
                SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);

                //判断“|”的拼接
                if(key.length() > 0){
                    key += "|";
                }

                //拼接key
                key += skuSaleAttrValue.getSaleAttrValueId();

                //停止拼接
                if((i+1)==skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId())){
                    map.put(key,skuSaleAttrValue.getSkuId());
                    key="";
                }
                //将map转换为json
                String valuesSkuJson = JSON.toJSONString(map);
                httpServletRequest.setAttribute("valuesSkuJson",valuesSkuJson);
            }
        }

        //通过skuId、spuId查询销售属性集合
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        httpServletRequest.setAttribute("spuSaleAttrList",spuSaleAttrList);
        //记录热度排名
        listService.incrHotScore(skuId);
        return "item";
    }



}
