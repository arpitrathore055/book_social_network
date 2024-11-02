package com.booksocialnetwork.services;

import com.booksocialnetwork.common.PageResponse;
import com.booksocialnetwork.entities.Book;
import com.booksocialnetwork.entities.User;
import com.booksocialnetwork.entities.history.BookTransactionHistory;
import com.booksocialnetwork.exception.OperationNotPermittedException;
import com.booksocialnetwork.file.FileUtils;
import com.booksocialnetwork.repositories.BookRepository;
import com.booksocialnetwork.repositories.BookTransactionHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookServiceTest {

    @InjectMocks
    private BookService bookService;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private BookTransactionHistoryRepository bookTransactionHistoryRepository;

    @Mock
    private BookTransactionHistory bookTransactionHistory;

    @BeforeEach
    public void setUp(){
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest
    @ValueSource(ints={
            1,2,3,4
    })
    public void testFindById(int bookId){

        Book fetchedBook = Book.builder()
                .id(bookId)
                .isbn("123")
                .title("my book")
                .shareable(true)
                .archived(true)
                .synopsis("this is my first book")
                .authorName("arpit rathore")
                .bookCover("")
                .owner(User.builder()
                        .id(2)
                        .email("arpitrathore010@gmail.com")
                        .createdDate(LocalDateTime.now())
                        .firstname("arpit")
                        .accountLocked(false)
                        .build())
                .createdBy(bookId)
                .createdDate(LocalDateTime.now())
                .build();

        BookResponse convertedBookResponse=BookResponse.builder()
                .id(fetchedBook.getId())
                .title(fetchedBook.getTitle())
                .authorName(fetchedBook.getAuthorName())
                .isbn(fetchedBook.getIsbn())
                .synopsis(fetchedBook.getSynopsis())
                .archived(fetchedBook.isArchived())
                .shareable(fetchedBook.isShareable())
                .owner(fetchedBook.getOwner().getId().toString())
                .cover(fetchedBook.getBookCover().getBytes())
                .build();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(fetchedBook));

        assertNotNull(bookRepository.findById(bookId));

        when(bookMapper.toBookReponse(fetchedBook)).thenReturn(convertedBookResponse);

        BookResponse fetchedBookResponse=bookService.findById(bookId);

        assertEquals(fetchedBookResponse.getId(),convertedBookResponse.getId());

    }

    @ParameterizedTest
    @ValueSource(ints={
            1,2,3,4
    })
    public void testBorrowBook_successful(int bookId){

        Book fetchedBook = Book.builder()
                .id(bookId)
                .isbn("123")
                .title("my book")
                .shareable(true)
                .archived(false)
                .synopsis("this is my first book")
                .authorName("arpit rathore")
                .bookCover("")
                .owner(User.builder()
                        .id(2)
                        .email("arpitrathore010@gmail.com")
                        .createdDate(LocalDateTime.now())
                        .firstname("arpit")
                        .accountLocked(false)
                        .build())
                .createdBy(bookId)
                .createdDate(LocalDateTime.now())
                .build();

        User connectedUser=User.builder()
                .id(3)
                .email("arpitrathore010@gmail.com")
                .createdDate(LocalDateTime.now())
                .firstname("arpit")
                .accountLocked(false)
                .build();

        BookTransactionHistory createdBookTransactionHistory=BookTransactionHistory.builder()
                .book(fetchedBook)
                .returnApproved(false)
                .returned(false)
                .user(connectedUser)
                .id(12)
                .build();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(fetchedBook));
        when(authentication.getPrincipal()).thenReturn(connectedUser);
        when(bookTransactionHistoryRepository.isAlreadyBorrowedByUser(bookId,connectedUser.getId())).thenReturn(false);
        when(bookTransactionHistoryRepository.save(any(BookTransactionHistory.class))).thenReturn(createdBookTransactionHistory);


        //main test method
        assertEquals(createdBookTransactionHistory.getId(),bookService.borrowBook(bookId,authentication));

    }

    @ParameterizedTest
    //@Execution(ExecutionMode.CONCURRENT)
    @ValueSource(ints={
            3,2,1
    })
    public void testBorrowBook_book_not_found(int bookId){

        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        Exception exception=assertThrows(EntityNotFoundException.class,()->bookService.borrowBook(bookId,authentication));

        assertEquals("No book found with Id: "+bookId,exception.getMessage());
    }

    @Test
    public void testBorrowBook_isNotShareable_or_isArchived(){

        Book fetchedBook=Book.builder()
                .id(1)
                .isbn("123")
                .title("my book")
                .shareable(false)
                .archived(true)
                .synopsis("this is my first book")
                .authorName("arpit rathore")
                .bookCover("")
                .owner(User.builder()
                        .id(2)
                        .email("arpitrathore010@gmail.com")
                        .createdDate(LocalDateTime.now())
                        .firstname("arpit")
                        .accountLocked(false)
                        .build())
                .createdBy(1)
                .createdDate(LocalDateTime.now())
                .build();

        when(bookRepository.findById(1)).thenReturn(Optional.of(fetchedBook));

        Exception exception=assertThrows(OperationNotPermittedException.class,()->bookService.borrowBook(1,authentication));

        assertEquals("The requested book cannot be borrowed since it is archived or not shareable",exception.getMessage());

    }

    @Test
    public void testBorrowBook_CannotBorrowOwnBook(){

        Book fetchedBook=Book.builder()
                .id(1)
                .isbn("123")
                .title("my book")
                .shareable(true)
                .archived(false)
                .synopsis("this is my first book")
                .authorName("arpit rathore")
                .bookCover("")
                .owner(User.builder()
                        .id(2)
                        .email("arpitrathore010@gmail.com")
                        .createdDate(LocalDateTime.now())
                        .firstname("arpit")
                        .accountLocked(false)
                        .build())
                .createdBy(1)
                .createdDate(LocalDateTime.now())
                .build();

        User connectedUser=User.builder()
                .id(2)
                .email("arpitrathore010@gmail.com")
                .createdDate(LocalDateTime.now())
                .firstname("arpit")
                .accountLocked(false)
                .build();

        when(bookRepository.findById(1)).thenReturn(Optional.of(fetchedBook));
        when(authentication.getPrincipal()).thenReturn(connectedUser);
//        when(fetchedBook.getOwner().getId()).thenReturn(2);
//        when(connectedUser.getId()).thenReturn(2);

        Exception exception=assertThrows(OperationNotPermittedException.class,()->bookService.borrowBook(1,authentication));

        assertEquals("You cannot borrow your own book",exception.getMessage());


    }

    @Test
    public void testBorrowBook_BookAlreadyBorrowed(){

        Book fetchedBook=Book.builder()
                .id(1)
                .isbn("123")
                .title("my book")
                .shareable(true)
                .archived(false)
                .synopsis("this is my first book")
                .authorName("arpit rathore")
                .bookCover("")
                .owner(User.builder()
                        .id(2)
                        .email("arpitrathore010@gmail.com")
                        .createdDate(LocalDateTime.now())
                        .firstname("arpit")
                        .accountLocked(false)
                        .build())
                .createdBy(2)
                .createdDate(LocalDateTime.now())
                .build();
        User connectedUser=User.builder()
                .id(3)
                .email("arpitrathore010@gmail.com")
                .createdDate(LocalDateTime.now())
                .firstname("arpit")
                .accountLocked(false)
                .build();

        when(bookRepository.findById(1)).thenReturn(Optional.of(fetchedBook));
        when(authentication.getPrincipal()).thenReturn(connectedUser);
        when(bookTransactionHistoryRepository.isAlreadyBorrowedByUser(1, connectedUser.getId())).thenReturn(true);

        Exception exception=assertThrows(OperationNotPermittedException.class,()->bookService.borrowBook(1,authentication));

        assertEquals("The requested book is already borrowed",exception.getMessage());

    }

    @Test
    public void testReturnedBorrowedBook(){

        Book fetchedBook=Book.builder()
                .id(1)
                .isbn("123")
                .title("my book")
                .shareable(true)
                .archived(false)
                .synopsis("this is my first book")
                .authorName("arpit rathore")
                .bookCover("")
                .owner(User.builder()
                        .id(2)
                        .email("arpitrathore010@gmail.com")
                        .createdDate(LocalDateTime.now())
                        .firstname("arpit")
                        .accountLocked(false)
                        .build())
                .createdBy(2)
                .createdDate(LocalDateTime.now())
                .build();

        User connectedUser=User.builder()
                .id(3)
                .email("arpitrathore010@gmail.com")
                .createdDate(LocalDateTime.now())
                .firstname("arpit")
                .accountLocked(false)
                .build();

        BookTransactionHistory fetchedBookTransactionHistory=BookTransactionHistory.builder()
                .book(fetchedBook)
                .returnApproved(false)
                .returned(false)
                .user(connectedUser)
                .id(12)
                .build();

        when(bookRepository.findById(1)).thenReturn(Optional.of(fetchedBook));
        when(authentication.getPrincipal()).thenReturn(connectedUser);
        when(bookTransactionHistoryRepository.findByBookIdAndUserId(1, connectedUser.getId())).thenReturn(Optional.of(fetchedBookTransactionHistory));
        when(bookTransactionHistoryRepository.save(fetchedBookTransactionHistory)).thenReturn(fetchedBookTransactionHistory);

        int expectedTransactionHistoryId= bookService.returnBorrowedBook(1,authentication);

        assertEquals(fetchedBookTransactionHistory.getId(),expectedTransactionHistoryId);
        assertTrue(fetchedBookTransactionHistory.getReturned());

    }

    @ParameterizedTest
    @Execution(ExecutionMode.CONCURRENT)
    @ValueSource(ints={
            1,2,4,Integer.MAX_VALUE
    })
    public void testReturnBorrowedBook_entityNotFoundException(int bookId){

        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        Exception exception=assertThrows(EntityNotFoundException.class,()->bookService.returnBorrowedBook(bookId,authentication));

        assertEquals("No book found with Id: "+bookId,exception.getMessage());

    }

    @Test
    public void testReturnBorrowedBook_isArchivedOrNotShareable(){

        Book fetchedBook=Book.builder()
                .id(1)
                .isbn("123")
                .title("my book")
                .shareable(false)
                .archived(true)
                .synopsis("this is my first book")
                .authorName("arpit rathore")
                .bookCover("")
                .owner(User.builder()
                        .id(2)
                        .email("arpitrathore010@gmail.com")
                        .createdDate(LocalDateTime.now())
                        .firstname("arpit")
                        .accountLocked(false)
                        .build())
                .createdBy(2)
                .createdDate(LocalDateTime.now())
                .build();

        when(bookRepository.findById(1)).thenReturn(Optional.of(fetchedBook));

        Exception exception=assertThrows(OperationNotPermittedException.class,()->bookService.returnBorrowedBook(1,authentication));

        assertEquals("The requested book cannot be borrowed since it is archived or not shareable",exception.getMessage());

    }

    @Test
    public void testReturnBorrowedBook_operationNotPermittedException(){

        Book fetchedBook=Book.builder()
                .id(1)
                .isbn("123")
                .title("my book")
                .shareable(true)
                .archived(false)
                .synopsis("this is my first book")
                .authorName("arpit rathore")
                .bookCover("")
                .owner(User.builder()
                        .id(2)
                        .email("arpitrathore010@gmail.com")
                        .createdDate(LocalDateTime.now())
                        .firstname("arpit")
                        .accountLocked(false)
                        .build())
                .createdBy(2)
                .createdDate(LocalDateTime.now())
                .build();

        User connectedUser=User.builder()
                .id(2)
                .email("arpitrathore010@gmail.com")
                .createdDate(LocalDateTime.now())
                .firstname("arpit")
                .accountLocked(false)
                .build();

        when(bookRepository.findById(1)).thenReturn(Optional.of(fetchedBook));
        when(authentication.getPrincipal()).thenReturn(connectedUser);

        Exception exception=assertThrows(OperationNotPermittedException.class,()->bookService.returnBorrowedBook(1,authentication));

        assertEquals("You cannot borrow or return your own book",exception.getMessage());

    }

    @Test
    public void testReturnBorrowedBook_bookNotBorrowedByUser(){

        Book fetchedBook=Book.builder()
                .id(1)
                .isbn("123")
                .title("my book")
                .shareable(true)
                .archived(false)
                .synopsis("this is my first book")
                .authorName("arpit rathore")
                .bookCover("")
                .owner(User.builder()
                        .id(2)
                        .email("arpitrathore010@gmail.com")
                        .createdDate(LocalDateTime.now())
                        .firstname("arpit")
                        .accountLocked(false)
                        .build())
                .createdBy(2)
                .createdDate(LocalDateTime.now())
                .build();

        User connectedUser=User.builder()
                .id(3)
                .email("arpitrathore010@gmail.com")
                .createdDate(LocalDateTime.now())
                .firstname("arpit")
                .accountLocked(false)
                .build();

        BookTransactionHistory fetchedBookTransactionHistory=BookTransactionHistory.builder()
                .book(fetchedBook)
                .returnApproved(false)
                .returned(false)
                .user(connectedUser)
                .id(12)
                .build();

        when(bookRepository.findById(1)).thenReturn(Optional.of(fetchedBook));
        when(authentication.getPrincipal()).thenReturn(connectedUser);
        when(bookTransactionHistoryRepository.findByBookIdAndUserId(1, connectedUser.getId())).thenReturn(Optional.empty());

        Exception exception=assertThrows(OperationNotPermittedException.class,()->bookService.returnBorrowedBook(1,authentication));

        assertEquals("You didn't borrow this book",exception.getMessage());

    }

    @Test
    public void testFindAllReturnedBooks_successful(){

        Book fetchedBook=Book.builder()
                .id(1)
                .isbn("123")
                .title("my book")
                .shareable(true)
                .archived(false)
                .synopsis("this is my first book")
                .authorName("arpit rathore")
                .bookCover("")
                .owner(User.builder()
                        .id(2)
                        .email("arpitrathore010@gmail.com")
                        .createdDate(LocalDateTime.now())
                        .firstname("arpit")
                        .accountLocked(false)
                        .build())
                .createdBy(2)
                .createdDate(LocalDateTime.now())
                .build();

        User connectedUser=User.builder()
                .id(3)
                .email("arpitrathore010@gmail.com")
                .createdDate(LocalDateTime.now())
                .firstname("arpit")
                .accountLocked(false)
                .build();

        Pageable pageable= PageRequest.of(0,1, Sort.by("createdDate").descending());

        BookTransactionHistory fetchedBookTransactionHistory=BookTransactionHistory.builder()
                .book(fetchedBook)
                .returnApproved(false)
                .returned(false)
                .user(connectedUser)
                .id(12)
                .build();

        BorrowedBookResponse borrowedBookResponse= BorrowedBookResponse.builder()
                .returnApproved(fetchedBookTransactionHistory.getReturnApproved())
                .returned(fetchedBookTransactionHistory.getReturned())
                .id(12)
                .authorName(fetchedBookTransactionHistory.getBook().getAuthorName())
                .isbn(fetchedBookTransactionHistory.getBook().getIsbn())
                .title(fetchedBookTransactionHistory.getBook().getTitle())
                .build();

        List<BookTransactionHistory> fetchedBookTransactionHistoryList=List.of(fetchedBookTransactionHistory);

        Page<BookTransactionHistory> fetchedBookTransactionHistoryPages=new PageImpl<>(fetchedBookTransactionHistoryList,pageable,fetchedBookTransactionHistoryList.size());

        when(authentication.getPrincipal()).thenReturn(connectedUser);
        when(bookTransactionHistoryRepository.findAllReturnedBooks(pageable,connectedUser.getId())).thenReturn(fetchedBookTransactionHistoryPages);
        when(bookMapper.toBorrowedBookResponse(any(BookTransactionHistory.class))).thenReturn(borrowedBookResponse);

        PageResponse finalPageResult=bookService.findAllReturnedBooks(0,1,authentication);

        verify(bookTransactionHistoryRepository).findAllReturnedBooks(pageable, connectedUser.getId());
        verify(bookMapper,times(1)).toBorrowedBookResponse(any(BookTransactionHistory.class));

        assertNotNull(finalPageResult);
        assertEquals(borrowedBookResponse,finalPageResult.getContent().get(0));
        assertEquals(1,finalPageResult.getSize());
        assertEquals(0,finalPageResult.getNumber());

    }

}