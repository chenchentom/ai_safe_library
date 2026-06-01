package com.aisafe.business.mapper;

import com.aisafe.business.entity.BizSupplyChainTagV2;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BizSupplyChainTagV2Mapper extends BaseMapper<BizSupplyChainTagV2> {

    @Delete("DELETE FROM biz_supply_chain_tag_v2 WHERE module = #{module} AND tag_code = #{tagCode} AND del_flag = '1'")
    int physicalDeleteSoftDeletedByCode(@Param("module") String module, @Param("tagCode") String tagCode);
}
