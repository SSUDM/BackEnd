package com.DM.DeveloperMatching.dto.Resume;

import com.DM.DeveloperMatching.domain.History;
import com.DM.DeveloperMatching.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HistoryDto {
    private String title;
    private String content;

    public History toEntity(User user) {
        History project = new History();
        project.setUser(user);
        project.setTitle(this.title);
        project.setContent(this.content);
        return project;
    }

//    public HistoryDto(String title, String content) {
//        this.title = title;
//        this.content = content;
//    }
}