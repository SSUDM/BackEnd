package com.DM.DeveloperMatching.dto.Recommend;

import com.DM.DeveloperMatching.domain.Article;
import com.DM.DeveloperMatching.domain.Level;
import com.DM.DeveloperMatching.domain.ProjectStatus;
import jakarta.persistence.Lob;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class RecommendProjectDto {
    private Long aId;
    private Long pId;
    private String title;
    private List<String> recPart;
    private List<String> recTech;
    private Level recLevel;
    private ProjectStatus projectStatus;
    private String projectImg;
}