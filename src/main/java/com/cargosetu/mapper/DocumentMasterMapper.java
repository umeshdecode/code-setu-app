package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.document.model.DocumentMaster;
import com.cargosetu.dto.DocumentMasterDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface DocumentMasterMapper extends BaseUpdateMapper<DocumentMasterDTO, DocumentMaster> {
}
