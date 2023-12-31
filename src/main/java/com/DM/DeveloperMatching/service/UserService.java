package com.DM.DeveloperMatching.service;

import com.DM.DeveloperMatching.domain.*;
import com.DM.DeveloperMatching.dto.Project.ProjectResponse;
import com.DM.DeveloperMatching.dto.Resume.CareerDto;
import com.DM.DeveloperMatching.dto.Resume.HistoryDto;
import com.DM.DeveloperMatching.dto.Suggest.SuggestResponse;
import com.DM.DeveloperMatching.dto.User.ResumeRequest;
import com.DM.DeveloperMatching.dto.User.UserInfoResponse;
import com.DM.DeveloperMatching.repository.CareerRepository;
import com.DM.DeveloperMatching.repository.HistoryRepository;
import com.DM.DeveloperMatching.repository.ProjectRepository;
import com.DM.DeveloperMatching.repository.UserRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final CareerRepository careerRepository;
    private final HistoryRepository previousProjectRepository;
    private final ProjectService projectService;
    @Value("${application.bucket.name}")
    private String bucketName;
    private final AmazonS3 amazonS3;

    //이력서 저장
    public User saveResume(ResumeRequest resumeRequest, Long userId, MultipartFile userImg) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("not found user"));
        String result = String.join(", ", resumeRequest.getTech());

        checkDuplicate(user, resumeRequest);
        List<Career> careerList = new ArrayList<>();
        List<History> history = new ArrayList<>();
        if(resumeRequest.getCareerList() != null) {
            careerList = resumeRequest.getCareerList().stream()
                    .filter(c -> !careerRepository.existsByUserAndContent(user, c.getCareer()))
                    .map(c -> {
                        try {
                            return c.toEntity(user);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        } else {
            if(!user.getCareerList().isEmpty()) {
                for(Career c : user.getCareerList()) {
                    careerRepository.delete(c);
                }
            }
            user.deleteAllCareer();
        }
        if(resumeRequest.getHistory() != null) {
            history = resumeRequest.getHistory().stream()
                    .filter(c -> !previousProjectRepository.existsByUserAndTitle(user, c.getTitle()))
                    .map(c -> c.toEntity(user))
                    .collect(Collectors.toList());
        } else {
            if(!user.getHistory().isEmpty()) {
                for(History h : user.getHistory()) {
                    previousProjectRepository.delete(h);
                }
            }
            user.deleteAllHistory();
        }
        if(userImg != null) {
            user.updateResume(resumeRequest.getPart(), resumeRequest.getLevel(),
                    resumeRequest.getIntroduction(), result, careerList, history, upload(userImg));
        }
        else {
            user.updateResume(resumeRequest.getPart(), resumeRequest.getLevel(),
                    resumeRequest.getIntroduction(), result, careerList, history);
        }
        return userRepository.save(user);
    }

    public void checkDuplicate(User user, ResumeRequest resumeRequest) {
        if(!user.getCareerList().isEmpty() && !user.getHistory().isEmpty()) {
            List<String> userC = user.getCareerList().stream()
                    .map(Career::getContent)
                    .collect(Collectors.toList());
            if(resumeRequest.getCareerList() != null) {
                List<String> requestC = resumeRequest.getCareerList().stream()
                        .map(CareerDto::getCareer)
                        .collect(Collectors.toList());
                for(String s : userC) {
                    if(!requestC.contains(s)) {
                        user.deleteCareer(s);
                        careerRepository.deleteByUserIdANDContent(user.getUId(), s);
                    }
                }
            }

            List<String> userH = user.getHistory().stream()
                    .map(History::getTitle)
                    .collect(Collectors.toList());
            if(resumeRequest.getHistory() != null) {
                List<String> requestH = resumeRequest.getHistory().stream()
                        .map(HistoryDto::getTitle)
                        .collect(Collectors.toList());
                for(String s : userH) {
                    if(!requestH.contains(s)) {
                        user.deleteHistory(s);
                        previousProjectRepository.deleteByUserIdANDTitle(user.getUId(), s);
                    }
                }
            }
        }
    }

    //내가 참여중인 프로젝트
    public List<ProjectResponse> getProceedingProject(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: "));
        List<ProjectResponse> projects = user.getUserInMember().stream()
                .filter(member -> member.getMemberStatus() != MemberStatus.MANAGER)
                .filter(member -> member.getMemberStatus() == MemberStatus.ACCEPTED)
                .filter(member -> member.getProject().getProjectStatus() == ProjectStatus.PROCEEDING ||
                        member.getProject().getProjectStatus() == ProjectStatus.RECRUITING)
                .map(member -> new ProjectResponse(member.getProject(), getUrl(member.getProject().getArticle())))
                .collect(Collectors.toList());

        return projects;
    }

    //내가 완료한 프로젝트
    public List<ProjectResponse> getDoneProject(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: "));
        List<ProjectResponse> projects = user.getUserInMember().stream()
                .filter(member -> member.getMemberStatus() != MemberStatus.MANAGER)
                .filter(member -> member.getProject().getProjectStatus() == ProjectStatus.DONE)
                .map(member -> new ProjectResponse(member.getProject(), getUrl(member.getProject().getArticle())))
                .collect(Collectors.toList());

        return projects;
    }

    //이력서 조회
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("not found user"));
    }

    //내 정보 가져오기
    public UserInfoResponse getUserInfo(Long userId, String userImg) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("not found user"));

        UserInfoResponse userInfoResponse = new UserInfoResponse(user, projectService, userImg);

        return userInfoResponse;
    }

    //내가 만든 프로젝트
    public List<ProjectResponse> getManagingProject(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: "));

        List<Member> members = user.getUserInMember().stream()
                .filter(member -> member.getMemberStatus() == MemberStatus.MANAGER)
                .collect(Collectors.toList());
        List<Project> projects = members.stream()
                .map(member -> member.getProject())
                .collect(Collectors.toList());
        List<ProjectResponse> projectResponses = projects.stream()
                .map(project -> {
                    ProjectResponse projectResponse = new ProjectResponse(project, getUrl(project.getArticle()));
                    return projectResponse;
                })
                .collect(Collectors.toList());

        return projectResponses;
    }

    //내가 좋아요 누른 프로젝트
    public List<ProjectResponse> getLikeProject(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: "));
        List<Likes> likes = user.getLikes().stream()
                .collect(Collectors.toList());
        List<Project> projects = likes.stream()
                .map(like -> like.getLikesProject())
                .collect(Collectors.toList());
        List<ProjectResponse> projectResponses = projects.stream()
                .filter(project -> project.getArticle().getArticleOwner() != user)
                .filter(project -> project.getProjectStatus() == ProjectStatus.RECRUITING)
                .map(project -> {
                    ProjectResponse projectResponse = new ProjectResponse(project, getUrl(project.getArticle()));
                    return projectResponse;
                })
                .collect(Collectors.toList());

        return projectResponses;
    }

    //참가 요청 보낸 프로젝트
    public List<ProjectResponse> getAppliedProject(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: "));
        List<ProjectResponse> projects = user.getUserInMember().stream()    //미쳤다 이게 되는겨?
                .filter(member -> member.getMemberStatus()==MemberStatus.WAITING)
                .filter(member -> member.getProject().getProjectStatus() == ProjectStatus.RECRUITING)
                .map(member -> new ProjectResponse(member.getProject(), getUrl(member.getProject().getArticle())))
                .collect(Collectors.toList());

        return projects;
    }

    //협업 요청 온 프로젝트
    public List<SuggestResponse> getSuggestedProject(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: "));
        List<SuggestResponse> suggestResponses = user.getSuggests().stream()
                .filter(suggest -> suggest.getProjectId().getProjectStatus() == ProjectStatus.RECRUITING)
                .map(suggest -> new SuggestResponse(suggest, getUrl(suggest.getProjectId().getArticle())))
                .collect(Collectors.toList());

        return suggestResponses;
    }

    public String upload(MultipartFile multipartFile) throws IOException {
        String s3FileName = UUID.randomUUID() + "-" + multipartFile.getOriginalFilename();

        ObjectMetadata objMeta = new ObjectMetadata();
        objMeta.setContentLength(multipartFile.getInputStream().available());

        amazonS3.putObject(bucketName, s3FileName, multipartFile.getInputStream(), objMeta);

        return s3FileName;
    }//내가 만든거 중 모집 중
    public List<ProjectResponse> getManagingAndRecruitingProject(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: "));

        List<Member> members = user.getUserInMember().stream()
                .filter(member -> member.getMemberStatus() == MemberStatus.MANAGER)
                .collect(Collectors.toList());
        List<Project> projects = members.stream()
                .map(member -> member.getProject())
                .collect(Collectors.toList());
        List<ProjectResponse> projectResponses = projects.stream()
                .filter(project -> project.getProjectStatus()==ProjectStatus.RECRUITING)
                .map(project -> {
                    ProjectResponse projectResponse = new ProjectResponse(project, getUrl(project.getArticle()));
                    return projectResponse;
                })
                .collect(Collectors.toList());

        return projectResponses;
    }



    //내가 만든거 중 진행중
    public List<ProjectResponse> getManagingAndProceedingProject(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: "));

        List<Member> members = user.getUserInMember().stream()
                .filter(member -> member.getMemberStatus() == MemberStatus.MANAGER)
                .collect(Collectors.toList());
        List<Project> projects = members.stream()
                .map(member -> member.getProject())
                .collect(Collectors.toList());
        List<ProjectResponse> projectResponses = projects.stream()
                .filter(project -> project.getProjectStatus() == ProjectStatus.PROCEEDING)
                .map(project -> {
                    ProjectResponse projectResponse = new ProjectResponse(project, getUrl(project.getArticle()));
                    return projectResponse;
                })
                .collect(Collectors.toList());

        return projectResponses;
    }

    //내가 만든거 중 완료
    public List<ProjectResponse> getManagedAndDoneProject(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: "));

        List<Member> members = user.getUserInMember().stream()
                .filter(member -> member.getMemberStatus() == MemberStatus.MANAGER)
                .collect(Collectors.toList());
        List<Project> projects = members.stream()
                .map(member -> member.getProject())
                .collect(Collectors.toList());
        List<ProjectResponse> projectResponses = projects.stream()
                .filter(project -> project.getProjectStatus()==ProjectStatus.DONE)
                .map(project -> {
                    ProjectResponse projectResponse = new ProjectResponse(project, getUrl(project.getArticle()));
                    return projectResponse;
                })
                .collect(Collectors.toList());

        return projectResponses;
    }

    private String getUrl(Article article) {
        URL url = amazonS3.getUrl(bucketName, article.getProjectImg());
        String urltext = "" + url;
        return urltext;
    }
}
