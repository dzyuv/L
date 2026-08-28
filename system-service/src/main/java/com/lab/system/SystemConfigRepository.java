package com.lab.system;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.Optional;

public interface SystemConfigRepository extends CrudMapper<SystemConfig> {
    default Optional<SystemConfig> findByConfigKey(String configKey) {
        return Optional.ofNullable(selectOne(Wrappers.<SystemConfig>query().eq("config_key", configKey)));
    }
}
