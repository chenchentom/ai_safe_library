package com.aisafe.business.service;

import com.aisafe.business.entity.BizSupplyChainTagV2;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public interface ISupplyChainTagV2Service extends IService<BizSupplyChainTagV2> {

    List<BizSupplyChainTagV2> getTree();

    BizSupplyChainTagV2 add(BizSupplyChainTagV2 entity);

    BizSupplyChainTagV2 update(BizSupplyChainTagV2 entity);

    void delete(Long id);

    List<BizSupplyChainTagV2> search(String keyword);

    void updateSortOrder(Long id, Integer newSortOrder);

    void exportExcel(HttpServletResponse response);
}
