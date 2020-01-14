package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

public interface ManageService {

    /**
     * 获取一级分类列表
     * @return
     */
    List<BaseCatalog1> getCatalog1();


    /**
     * 根据一级分类id获取二级分类列表
     * @param catalog1Id
     * @return
     */
    List<BaseCatalog2> getCatalog2(String catalog1Id);


    /**
     * 根据二级分类id获取三级分类列表
     * @param catalog2Id
     * @return
     */
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    /**
     * 根据三级分类id获取平台属性
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrList(String catalog3Id);

    /**
     * 保存或修改前端输入的平台属性数据
     * @param baseAttrInfo
     */
    void addOrUpdateAttrInfo(BaseAttrInfo baseAttrInfo);


    /**
     * 根据平台属性id查询平台属性集合
     * @param attrId
     * @return
     */
    List<BaseAttrValue> getAttrValueList(String attrId);

    /**
     * 根据平台属性id查询平台属性对象
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(String attrId);

    /**
     * 根据三级分类id获取所有商品
     * @param catalogId3
     * @return
     */
    List<SpuInfo> getSpuList(String catalogId3);

    /**
     * 获取销售属性的列表
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 将前台页面添的销售属性添加到数据库（DTO方式\在现有的实体类中添加非表的字段（扩展表））
     * @param spuInfo
     * @return
     */
    void addSpuInfo(SpuInfo spuInfo);

    /**
     * 随着页面加载获取spuimage属性列表
     * @param spuImage
     * @return
     */
    List<SpuImage> getSpuImageList(SpuImage spuImage);

    /**
     * 根据销售属性的id获取销售属性的集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    /**
     * 保存前端传来的skuinfo页面
     * @param skuInfo
     */
    void addSkuInfo(SkuInfo skuInfo);

    /**
     * 通过skuId查询skuInfo
     * @param skuId
     * @return
     */
    //通过redis进行加分布式锁
    SkuInfo getSkuInfoJedis(String skuId);

    //通过redisson进行加分布式锁
    SkuInfo getSkuInfoRedisson(String skuId);

    SkuInfo getSkuInfo(String skuId);
    /**
     * 通过skuId、spuId查询销售属性集合
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 根据spuId查询销售属性值与skuid组合的数据集合
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}

