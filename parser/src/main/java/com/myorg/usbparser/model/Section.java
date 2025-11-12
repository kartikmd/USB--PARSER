package com.myorg.usbparser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * Model representing a document section / ToC line.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Section {

    @JsonProperty("doc_title")
    private String docTitle;

    @JsonProperty("section_id")
    private String sectionId;

    @JsonProperty("title")
    private String title;

    /**
     * Use Integer so that missing page numbers map to null instead of 0.
     */
    @JsonProperty("page")
    private Integer page;

    /**
     * Section level (nullable).
     */
    @JsonProperty("level")
    private Integer level;

    @JsonProperty("parent_id")
    private String parentId;

    @JsonProperty("full_path")
    private String fullPath;

    @Builder.Default
    @JsonProperty("tags")
    private List<String> tags = List.of();

    // present in sections.jsonl; will be null/omitted for pure ToC lines
    @JsonProperty("content")
    private String content;

    // Defensive getter to keep immutability of collections
    public List<String> getTags() {
        return tags == null ? List.of() : List.copyOf(tags);
    }
}
