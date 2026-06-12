package com.aisafe.system.service;

import com.aisafe.system.entity.BizTagCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 分类标签 Service 接口
 */
public interface ITagCategoryService extends IService<BizTagCategory> {

    /**
     * 获取指定模块的完整树结构
     */
    List<BizTagCategory> getTreeByModule(String module);

    /**
     * 新增标签（自动维护 tag_path / tag_level）
     */
    BizTagCategory add(BizTagCategory entity);

    /**
     * 更新标签
     */
    BizTagCategory update(BizTagCategory entity);

    /**
     * 删除标签（逻辑删除，级联删除所有子节点）
     */
    void delete(Long id);

    /**
     * 按关键字模糊搜索标签名称
     */
    List<BizTagCategory> search(String keyword);

    /**
     * 更新排序号
     */
    void updateSortOrder(Long id, Integer newSortOrder);

    /**
     * Excel 导出
     */
    void exportExcel(HttpServletResponse response, String module);
}
