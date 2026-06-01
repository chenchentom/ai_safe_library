package com.aisafe.system.mapper;

import com.aisafe.system.entity.BizTagCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 分类标签 Mapper
 */
@Mapper
public interface BizTagCategoryMapper extends BaseMapper<BizTagCategory> {

    /** 物理清理已逻辑删除、仍占用唯一编码的历史数据 */
    @Delete("DELETE FROM biz_tag_category WHERE module = #{module} AND tag_code = #{tagCode} AND del_flag = '1'")
    int physicalDeleteSoftDeletedByCode(@Param("module") String module, @Param("tagCode") String tagCode);
}
