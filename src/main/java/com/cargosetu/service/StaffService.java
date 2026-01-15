package com.cargosetu.service;

import com.cargosetu.model.Staff;
import com.cargosetu.repository.StaffRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class StaffService {

    private final StaffRepository staffRepository;

    public StaffService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    private String getModuleName(){
        return "Staff[StaffService]";
    }

    @Transactional
    public Boolean saveStaff(){
        return false;
    }

}
