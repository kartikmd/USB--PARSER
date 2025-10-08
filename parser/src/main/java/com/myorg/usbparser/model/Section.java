package com.myorg.usbparser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Section {
    private String docTitle;
    private String sectionId;
    private String title;
    private int page;
    private int level;
    private String parentId;
    private String fullPath;

    @Builder.Default
    private List<String> tags = List.of();

    private String content; // Will not appear in JSON if null

    // Defensive getter for collections: returns an unmodifiable copy
    public List<String> getTags() {
        return tags == null ? List.of() : List.copyOf(tags);
    }
}
