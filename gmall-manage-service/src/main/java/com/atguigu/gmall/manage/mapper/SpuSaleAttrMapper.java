package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SpuSaleAttr;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {
    List<SpuSaleAttr> selectSpuSaleAttrList(String spuId);

    //通过skuId、spuId查询销售属性集合并锁定
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(String id, String spuId);
}
