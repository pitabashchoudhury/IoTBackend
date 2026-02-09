package com.foodchain.auth.mapper;

import com.foodchain.auth.entity.UserEntity;
import com.foodchain.common.dto.auth.UserDto;
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
