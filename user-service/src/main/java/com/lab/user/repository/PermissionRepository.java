package com.lab.user.repository;
import com.lab.user.entity.*;
import com.lab.user.client.*;
import com.lab.user.seed.*;
import com.lab.user.scheduler.*;
import com.lab.user.security.*;
import com.lab.user.config.*;
import com.lab.user.controller.*;
import com.lab.user.service.*;
import com.lab.user.service.impl.*;

import com.lab.common.persistence.CrudMapper;

public interface PermissionRepository extends CrudMapper<Permission> {
}
