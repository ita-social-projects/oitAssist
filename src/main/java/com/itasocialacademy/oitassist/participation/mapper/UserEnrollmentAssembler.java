package com.itasocialacademy.oitassist.participation.mapper;

import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserEnrollmentAssembler {
    private final UserFacade userFacade;

    public <T, R> List<R> enrichWithUser(
        List<T> items,
        Function<T, Long> userIdExtractor,
        BiFunction<T, UserProfileDetails, R> combiner) {
        List<Long> userIds = items.stream()
            .map(userIdExtractor)
            .distinct()
            .toList();

        Map<Long, UserProfileDetails> usersById = userFacade.findProfilesByIds(userIds).stream()
            .collect(Collectors.toMap(UserProfileDetails::id, Function.identity()));

        return items.stream()
            .map(item -> combiner.apply(item, usersById.get(userIdExtractor.apply(item))))
            .toList();
    }
}
