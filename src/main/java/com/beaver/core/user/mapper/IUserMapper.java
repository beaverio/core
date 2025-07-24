package com.beaver.core.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import com.beaver.core.user.User;
import com.beaver.core.user.dto.UpdateSelf;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface IUserMapper {
    void mapToUser(UpdateSelf dto, @MappingTarget User user);
}