package com.family.finance.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.family.finance.ledger.domain.LedgerEntry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerEntryMapper extends BaseMapper<LedgerEntry> {}
