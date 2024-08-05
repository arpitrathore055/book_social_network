package com.booksocialnetwork.services;

import com.booksocialnetwork.common.PageResponse;
import com.booksocialnetwork.entities.Book;
import com.booksocialnetwork.entities.Feedback;
import com.booksocialnetwork.entities.User;
import com.booksocialnetwork.exception.OperationNotPermittedException;
import com.booksocialnetwork.repositories.BookRepository;
import com.booksocialnetwork.repositories.FeedbackRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final BookRepository bookRepository;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackRepository feedbackRepository;

    public Integer save(FeedbackRequest request, Authentication connectedUser) {
        Book book=bookRepository.findById(request.bookId()).orElseThrow(()->new EntityNotFoundException("No book found with Id: "+request.bookId()));
        if(book.isArchived() || !book.isShareable()){
            throw new OperationNotPermittedException("You cannot give a feedback for an archived or non-shareable book");
        }
        User user=((User)connectedUser.getPrincipal());
        if(Objects.equals(book.getOwner().getId(),user.getId())){
            throw new OperationNotPermittedException("You cannot give a feedback to your own book");
        }
        Feedback feedback=feedbackMapper.toFeedback(request);
        return feedbackRepository.save(feedback).getId();
    }

    public PageResponse<FeedbackResponse> findAllFeedbacksByBook(Integer bookId, int page, int size, Authentication connectedUser) {
        Pageable pageable= PageRequest.of(page,size);
        User user=((User)connectedUser.getPrincipal());
        Page<Feedback> feedbacks=feedbackRepository.findAllByBookId(bookId,pageable);
        List<FeedbackResponse> feedbackResponse=feedbacks.stream().map(feedback -> feedbackMapper.toFeedbackResponse(feedback,user.getId())).toList();
        return new PageResponse<FeedbackResponse>(
                feedbackResponse,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast()
        );
    }
}
