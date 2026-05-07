package com.system.service;

import com.system.dto.CustomerProfileSaveDTO;
import com.system.vo.CustomerProfileVO;

public interface CustomerProfileService {
    CustomerProfileVO getMyProfile();

    CustomerProfileVO saveMyProfile(CustomerProfileSaveDTO dto);
}
