package com.learning.silveragepass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatusStatsDTO {
    private String date;
    private Long pending;
    private Long accepted;
    private Long inProgress;
    private Long completed;
    private Long cancelled;
}
