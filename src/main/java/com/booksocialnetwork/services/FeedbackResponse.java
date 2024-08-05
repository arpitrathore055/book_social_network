package com.booksocialnetwork.services;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FeedbackResponse {

    private Double note;
    private String comment;
    private boolean ownFeedback;//just to highlight a particular feedback provided by current or connectedUser to differentiate his feedback from rest of the feedbacks

}
