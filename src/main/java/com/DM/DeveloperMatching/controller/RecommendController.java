package com.DM.DeveloperMatching.controller;

import com.DM.DeveloperMatching.domain.Article;
import com.DM.DeveloperMatching.domain.User;
import com.DM.DeveloperMatching.dto.Recommend.RecommendProjectDto;
import com.DM.DeveloperMatching.dto.Recommend.RecommendRequest;
import com.DM.DeveloperMatching.dto.Recommend.RecommendUserDto;
import com.DM.DeveloperMatching.service.RecommendService;
import com.DM.DeveloperMatching.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api")
public class RecommendController {
    private final RecommendService recommendService;
    private final UserService userService;

    @GetMapping(value = "/rec-project")
    public ResponseEntity<List<RecommendProjectDto>> recommendProject(@RequestBody(required = false) RecommendRequest request) {
        Long userId = 1L;
        if(request == null) {
            List<Article> articles = recommendService.recommendProjectByCS(userId);
            List<RecommendProjectDto> recommendDtos = articles.stream()
                    .map(RecommendProjectDto::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(recommendDtos);
        }
        else {
            List<Article> articles = recommendService.recommendProjectByCS(userId, request.getRecPart(),
                    request.getRecTech(),
                    request.getRecLevel());

            List<RecommendProjectDto> recommendDtos = articles.stream()
                    .map(RecommendProjectDto::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(recommendDtos);
        }
    }

    @GetMapping(value = "/rec-teammate/{id}")
    public ResponseEntity<Map<String, List<RecommendUserDto>>> recommendUser(@PathVariable Long id) {
        List<List<User>> users = recommendService.recommendUserByCS(id);
        Map<String, List<RecommendUserDto>> result = new HashMap<>();
        for(List<User> userList : users) {
            if(!result.containsKey(userList.get(0).getPart())) {
                result.put(userList.get(0).getPart(), userList.stream()
                        .map(RecommendUserDto::toDto)
                        .collect(Collectors.toList()));
            }
            else {
                result.get(userList.get(0).getPart()).addAll(userList.stream()
                        .map(RecommendUserDto::toDto)
                        .collect(Collectors.toList()));
            }
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(result);
    }
}