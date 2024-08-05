package com.booksocialnetwork.entities;

import com.booksocialnetwork.entities.base.BaseEntity;
import com.booksocialnetwork.entities.history.BookTransactionHistory;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Book extends BaseEntity{

    private String title;
    private String authorName;
    private String isbn;
    private String synopsis;
    private String bookCover;
    private boolean archived;
    private boolean shareable;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "book")
    private List<Feedback> feedbacks;

    @OneToMany(mappedBy = "book")
    private List<BookTransactionHistory> histories;

    @Transient
    public double getrate(){
        if(feedbacks == null || feedbacks.isEmpty()){
            return 0.0;
        }
        var rate=this.feedbacks.stream().mapToDouble(Feedback::getNote).average().orElse(0.0);
        double roundedRate=Math.round(rate*10.0)/10.0;
        return roundedRate;
    }

}
