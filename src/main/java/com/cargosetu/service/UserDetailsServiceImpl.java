package com.cargosetu.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.model.Role;
import com.cargosetu.model.Staff;
import com.cargosetu.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private  StaffRepository staffRepository;

    public UserDetailsServiceImpl() {}

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Staff staff = null;
        staff = staffRepository.findByEmail(username).orElse(null);
        if(staff == null){
            staff = staffRepository.findByUserName(username).orElse(null);
        }
        if(staff == null){
            throw new UsernameNotFoundException(username);
        }
        LoggedInUser loggedInUser = convertStaffToLoggedInUser(staff);
        System.out.println("loggedInUser: " + loggedInUser);
        return loggedInUser;
    }

    private LoggedInUser convertStaffToLoggedInUser(Staff staff) {
        LoggedInUser loggedInUser = new LoggedInUser();
        loggedInUser.setRoles(staff.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()));
        loggedInUser.setUserName(staff.getUserName());
        loggedInUser.setStaffId(staff.getStaffId());
        loggedInUser.setEmail(staff.getEmail());
        loggedInUser.setFirstName(staff.getFirstName());
        loggedInUser.setLastName(staff.getLastName());
        loggedInUser.setPassword(staff.getPassword());
        loggedInUser.setMobileNumber(staff.getMobileNumber());
        loggedInUser.setUuid(staff.getUuid());

        return loggedInUser;
    }
}
