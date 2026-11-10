package com.lab.system.repository;
import com.lab.system.entity.*;
import com.lab.system.seed.*;
import com.lab.system.scheduler.*;
import com.lab.system.config.*;
import com.lab.system.controller.*;
import com.lab.system.service.*;
import com.lab.system.service.impl.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.Optional;

public interface SystemConfigRepository extends CrudMapper<SystemConfig> {
    default Optional<SystemConfig> findByConfigKey(String configKey) {
        return Optional.ofNullable(selectOne(Wrappers.<SystemConfig>query().eq("config_key", configKey)));
    }
}
