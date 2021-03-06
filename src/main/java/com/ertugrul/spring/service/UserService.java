package com.ertugrul.spring.service;

import com.ertugrul.spring.converter.UserMapper;
import com.ertugrul.spring.dto.UserDto;
import com.ertugrul.spring.entity.User;
import com.ertugrul.spring.exception.UserNotFoundException;
import com.ertugrul.spring.service.entityservice.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserEntityService userEntityService;
    private final ValidationService validationService;

    public List<UserDto> findAll() {

        List<User> userList = userEntityService.findAll();

        return UserMapper.INSTANCE.convertAllUserDtoToUser(userList);
    }

    public UserDto findById(Long id) {

        Optional<User> optionalUser = userEntityService.findById(id);

        User user = validationService.validateUser(optionalUser);

        return UserMapper.INSTANCE.convertUserDtoToUser(user);
    }

    public UserDto findByUsername(String username) {

        Optional<User> user = userEntityService.findByUsername(username);

        User validatedUser = validationService.validateUser(user);

        return UserMapper.INSTANCE.convertUserDtoToUser(validatedUser);
    }

    @Transactional
    public UserDto save(UserDto userDto) {

        User user = UserMapper.INSTANCE.convertUserDtoToUser(userDto);

        user = userEntityService.save(user);

        return UserMapper.INSTANCE.convertUserDtoToUser(user);
    }

    @Transactional
    public void delete(Long id) {

        User user = findUserById(id);

        userEntityService.delete(user);
    }

    private User findUserById(Long id) {
        Optional<User> optionalUser = userEntityService.findById(id);

        return validationService.validateUser(optionalUser);
    }


}
