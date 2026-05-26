package com.aisafe.business.service;

import com.aisafe.business.entity.BizSupplyChainTag;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ISupplyChainTagService extends IService<BizSupplyChainTag> {

    List<BizSupplyChainTag> getTree();

    BizSupplyChainTag add(BizSupplyChainTag entity);

    BizSupplyChainTag update(BizSupplyChainTag entity);

    void delete(Long id);

    List<BizSupplyChainTag> search(String keyword);
}
