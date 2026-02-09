package com.foodchain.iotbackend.mapper;

import com.foodchain.iotbackend.dto.auth.UserDto;
import com.foodchain.iotbackend.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(UserEntity entity) {
        return new UserDto(
                entity.getId().toString(),
                entity.getEmail(),
                entity.getName(),
                entity.getProfileImageUrl()
        );
    }
}
