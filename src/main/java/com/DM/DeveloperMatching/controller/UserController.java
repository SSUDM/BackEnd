package com.DM.DeveloperMatching.controller;

import com.DM.DeveloperMatching.config.jwt.JwtTokenUtils;
import com.DM.DeveloperMatching.domain.User;
import com.DM.DeveloperMatching.dto.Project.ProjectResponse;
import com.DM.DeveloperMatching.dto.User.ResumeRequest;
import com.DM.DeveloperMatching.dto.User.ResumeResponse;
import com.DM.DeveloperMatching.dto.User.UserInfoResponse;
import com.DM.DeveloperMatching.service.ProjectService;
import com.DM.DeveloperMatching.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api")
public class UserController {

    private final UserService userService;
    private final ProjectService projectService;
    private final JwtTokenUtils jwtTokenUtils;

    @Value("${jwt.secret-key}")
    private String secretKey;

    //이력서 저장
    @PostMapping("/resume")
    public ResponseEntity<ResumeResponse> saveResume(@RequestHeader HttpHeaders headers,
                                                     @RequestBody ResumeRequest resumeRequest) {
        String token = headers.getFirst("Authorization");
        Long uId = jwtTokenUtils.extractUserId(token,secretKey);
        User savedUser = userService.saveResume(resumeRequest, uId);
        ResumeResponse resumeResponse = new ResumeResponse(savedUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeResponse);
    }

    //이력서 조회
    @GetMapping("/resume")
    public ResponseEntity<ResumeResponse> getResume(@RequestHeader HttpHeaders headers) {
        String token = headers.getFirst("Authorization");
        Long uId = jwtTokenUtils.extractUserId(token,secretKey);
        User user = userService.findUserById(uId);

        ResumeResponse resumeResponse = new ResumeResponse(user);

        return ResponseEntity.status(HttpStatus.OK)
                .body(resumeResponse);
    }

    //이력서 수정
    @PutMapping("/resume")
    public ResponseEntity<ResumeResponse> updateResume(@RequestHeader HttpHeaders headers,
                                                       @RequestBody ResumeRequest resumeRequest) {
        String token = headers.getFirst("Authorization");
        Long uId = jwtTokenUtils.extractUserId(token,secretKey);
        User updatedUser = userService.saveResume(resumeRequest, uId);
        ResumeResponse resumeResponse = new ResumeResponse(updatedUser);

        return ResponseEntity.status(HttpStatus.OK)
                .body(resumeResponse);
    }

    //내 정보 보기
    @GetMapping("/user/info")
    public ResponseEntity<UserInfoResponse> getUserInfo(@RequestHeader HttpHeaders headers) {
        String token = headers.getFirst("Authorization");
        Long uId = jwtTokenUtils.extractUserId(token,secretKey);
        UserInfoResponse userInfo = userService.getUserInfo(uId);

        return ResponseEntity.ok()
                .body(userInfo);
    }

    //유저가 한 프로젝트 전체 조회
    @GetMapping("/user/get-all-my-projects")
    public ResponseEntity<List<ProjectResponse>> getMyProjects(@RequestHeader HttpHeaders headers) {
        String token = headers.getFirst("Authorization");
        Long uId = jwtTokenUtils.extractUserId(token,secretKey);
        List<ProjectResponse> projects = projectService.getAllUserProjects(uId);

        return ResponseEntity.ok()
                .body(projects);
    }

    //내가 만든 프로젝트
    @GetMapping("/user/get-managing-projects")
    public ResponseEntity<List<ProjectResponse>> getManagingProject(@RequestHeader HttpHeaders headers) {
        String token = headers.getFirst("Authorization");
        Long uId = jwtTokenUtils.extractUserId(token,secretKey);
        List<ProjectResponse> projects = userService.getManagingProject(uId);

        return ResponseEntity.ok()
                .body(projects);
    }

    //내가 좋아요 누른 프로젝트
    @GetMapping("/user/get-like-projects")
    public ResponseEntity<List<ProjectResponse>> getLikeProject(@RequestHeader HttpHeaders headers) {
        String token = headers.getFirst("Authorization");
        Long uId = jwtTokenUtils.extractUserId(token,secretKey);
        List<ProjectResponse> projects = userService.getLikeProject(uId);

        return ResponseEntity.ok()
                .body(projects);
    }

    //참가 요청 보낸 프로젝트
    @GetMapping("/user/get-applied-projects")
    public ResponseEntity<List<ProjectResponse>> getAppliedProject(@RequestHeader HttpHeaders headers) {
        String token = headers.getFirst("Authorization");
        Long uId = jwtTokenUtils.extractUserId(token,secretKey);
        List<ProjectResponse> projects = userService.getAppliedProject(uId);

        return ResponseEntity.ok()
                .body(projects);
    }

    //협업 요청 온 프로젝트
    @GetMapping("/user/get-suggested-projects")
    public ResponseEntity<List<ProjectResponse>> getSuggestedProject(@RequestHeader HttpHeaders headers) {
        String token = headers.getFirst("Authorization");
        Long uId = jwtTokenUtils.extractUserId(token,secretKey);
        List<ProjectResponse> projects = userService.getSuggestedProject(uId);

        return ResponseEntity.ok()
                .body(projects);
    }

}