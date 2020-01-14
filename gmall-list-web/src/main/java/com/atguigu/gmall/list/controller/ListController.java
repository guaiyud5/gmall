package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;
    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
    public String getList(SkuLsParams skuLsParams, Model model){
        // 每页显示的条数
        skuLsParams.setPageSize(2);
        //根据参数返回sku列表
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        model.addAttribute("skuLsInfoList",skuLsResult.getSkuLsInfoList());
        //从结果中取出平台属性值列表
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> attrList = manageService.getAttrList(attrValueIdList);
        //已选的属性值列表
        String urlParam = makeUrlParam(skuLsParams);
        // 声明一个保存面包屑的集合
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();
        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo =  iterator.next();
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length>0){
                    for (String valueId : skuLsParams.getValueId()) {
                        if(valueId.equals(baseAttrValue.getId())){
                            //删除baseAttrInfo
                            iterator.remove();
                            //组装面包屑 平台属性值：平台属性值名称
                            // 将面包屑的内容 赋值给了平台属性值对象的名称
                            BaseAttrValue baseAttrValueed = new BaseAttrValue();
                            baseAttrValueed.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            String newUrlParam = makeUrlParam(skuLsParams, valueId);
                            //赋值最新的参数列表
                            baseAttrValueed.setUrlParam(newUrlParam);
                            //将每个面包屑都放入集合中
                            baseAttrValueArrayList.add(baseAttrValueed);
                        }
                    }
                }
            }
        }
        //分页
        model.addAttribute("totalPages", skuLsResult.getTotalPages());
        model.addAttribute("pageNo",skuLsParams.getPageNo());

        model.addAttribute("urlParam",urlParam);
        model.addAttribute("attrList",attrList);
        // 保存数据
        model.addAttribute("keyword",skuLsParams.getKeyword());
        model.addAttribute("baseAttrValueArrayList",baseAttrValueArrayList);
        // 获取sku属性值列表
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        model.addAttribute("skuLsInfoList",skuLsInfoList);
        return "list";
    }

    //制作查询的参数
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String urlParam = "";
        //判断用户是否输入keyword
        if(skuLsParams.getKeyword() != null){
            urlParam += "keyword=" + skuLsParams.getKeyword();
        }
        //判断用户是否输入三级分类id
        if(skuLsParams.getCatalog3Id() != null){
            urlParam += "catalog3Id=" + skuLsParams.getCatalog3Id();
        }
        //判断用户是否输入平台属性值检id 索条件
        if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length>0){
            for (int i = 0; i < skuLsParams.getValueId().length ; i++) {
                String valueId = skuLsParams.getValueId()[i];
                if(excludeValueIds != null && excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if(excludeValueId.equals(valueId)){
                        //停止本次循环
                        continue ;
                    }
                }
//                if(urlParam.length() > 0){
//                    urlParam += "&";
//                }
                urlParam += "&valueId=" + valueId;
            }
        }
        return urlParam;
    }


}
