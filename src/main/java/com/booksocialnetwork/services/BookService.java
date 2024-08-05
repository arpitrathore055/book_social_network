package com.booksocialnetwork.services;

import com.booksocialnetwork.common.PageResponse;
import com.booksocialnetwork.entities.Book;
import com.booksocialnetwork.entities.User;
import com.booksocialnetwork.entities.history.BookTransactionHistory;
import com.booksocialnetwork.exception.OperationNotPermittedException;
import com.booksocialnetwork.file.FileStorageService;
import com.booksocialnetwork.repositories.BookRepository;
import com.booksocialnetwork.repositories.BookTransactionHistoryRepository;
import jakarta.mail.Multipart;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;
    private final BookRepository bookRepository;
    private final BookTransactionHistoryRepository bookTransactionHistoryRepository;
    private final FileStorageService fileStorageService;

    public Integer save(BookRequest request, Authentication connectedUser){
        User user=((User)connectedUser.getPrincipal());
        Book book=bookMapper.toBook(request);
        book.setOwner(user);
        return bookRepository.save(book).getId();
    }

    public BookResponse findById(Integer bookId) {
        return bookRepository.findById(bookId).map(bookMapper::toBookReponse).orElseThrow(()->new EntityNotFoundException("No book found with the ID:: "+bookId));
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        User user=((User)connectedUser.getPrincipal());
        Pageable pageable= PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<Book> books=bookRepository.findAllDisplayableBooks(pageable,user.getId());
        List<BookResponse> bookResponses=books.stream().map(bookMapper::toBookReponse).toList();
        return new PageResponse<BookResponse>(
                bookResponses,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        User user=((User)connectedUser.getPrincipal());
        Pageable pageable= PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<Book> books=bookRepository.findAll(BookSpecification.withOwnerId(user.getId()),pageable);
        List<BookResponse> bookResponses=books.stream().map(bookMapper::toBookReponse).toList();
        return new PageResponse<BookResponse>(
                bookResponses,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        User user=((User)connectedUser.getPrincipal());
        Pageable pageable= PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks=bookTransactionHistoryRepository.findAllBorrowedBooks(pageable,user.getId());
        List<BorrowedBookResponse> booksResponse=allBorrowedBooks.stream().map(bookMapper::toBorrowedBookResponse).toList();
        return new PageResponse<BorrowedBookResponse>(
                booksResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
        User user=((User)connectedUser.getPrincipal());
        Pageable pageable= PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks=bookTransactionHistoryRepository.findAllReturnedBooks(pageable,user.getId());
        List<BorrowedBookResponse> booksResponse=allBorrowedBooks.stream().map(bookMapper::toBorrowedBookResponse).toList();
        return new PageResponse<BorrowedBookResponse>(
                booksResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public Integer updateShareableStatus(int bookId, Authentication connectedUser) {
        Book book=bookRepository.findById(bookId).orElseThrow(()->new EntityNotFoundException("No book found with Id: "+bookId));
        User user=((User)connectedUser.getPrincipal());
        if(!Objects.equals(book.getOwner().getId(),user.getId())){
            throw new OperationNotPermittedException("You cannot update books shareable status");
        }
        book.setShareable(!book.isShareable());
        return bookRepository.save(book).getId();
    }

    public Integer updateArchivedStatus(int bookId, Authentication connectedUser) {
        Book book=bookRepository.findById(bookId).orElseThrow(()->new EntityNotFoundException("No book found with Id: "+bookId));
        User user=((User)connectedUser.getPrincipal());
        if(!Objects.equals(book.getOwner().getId(),user.getId())){
            throw new OperationNotPermittedException("You cannot update books shareable status");
        }
        book.setArchived(!book.isArchived());
        return bookRepository.save(book).getId();
    }

    public Integer borrowBook(int bookId, Authentication connectedUser) {
        Book book=bookRepository.findById(bookId).orElseThrow(()->new EntityNotFoundException("No book found with Id: "+bookId));
        if(book.isArchived() || !book.isShareable()){
            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is archived or not shareable");
        }
        User user=((User)connectedUser.getPrincipal());
        if(Objects.equals(book.getOwner().getId(),user.getId())){
            throw new OperationNotPermittedException("You cannot borrow your own book");
        }
        final boolean isAlreadyBorrowed=bookTransactionHistoryRepository.isAlreadyBorrowedByUser(bookId,user.getId());
        if(isAlreadyBorrowed){
            throw new OperationNotPermittedException("The requested book is already borrowed");
        }
        BookTransactionHistory bookTransactionHistory=BookTransactionHistory.builder()
                .user(user)
                .book(book)
                .returned(false)
                .returnApproved(false)
                .build();
        return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public Integer returnBorrowedBook(int bookId, Authentication connectedUser) {
        Book book=bookRepository.findById(bookId).orElseThrow(()->new EntityNotFoundException("No book found with Id: "+bookId));
        if(book.isArchived() || !book.isShareable()){
            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is archived or not shareable");
        }
        User user=((User)connectedUser.getPrincipal());
        if(Objects.equals(book.getOwner().getId(),user.getId())){
            throw new OperationNotPermittedException("You cannot borrow or return your own book");
        }
        BookTransactionHistory bookTransactionHistory=bookTransactionHistoryRepository.findByBookIdAndUserId(bookId,user.getId()).orElseThrow(()-> new OperationNotPermittedException("You didn't borrow this book"));
        bookTransactionHistory.setReturned(true);
        return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public Integer approveReturnBorrowedBook(int bookId, Authentication connectedUser) {
        Book book=bookRepository.findById(bookId).orElseThrow(()->new EntityNotFoundException("No book found with Id: "+bookId));
        if(book.isArchived() || !book.isShareable()){
            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is archived or not shareable");
        }
        User user=((User)connectedUser.getPrincipal());
        if(Objects.equals(book.getOwner().getId(),user.getId())){
            throw new OperationNotPermittedException("You cannot borrow or return your own book");
        }
        BookTransactionHistory bookTransactionHistory=bookTransactionHistoryRepository.findByBookIdAndOwnerId(bookId,user.getId()).orElseThrow(()-> new OperationNotPermittedException("The book is not returned yet. You cannot approve it's return"));
        bookTransactionHistory.setReturnApproved(true);
        return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public void uploadBookCoverPicture(Integer bookId, MultipartFile file, Authentication connectedUser) {
        Book book=bookRepository.findById(bookId).orElseThrow(()->new EntityNotFoundException("No book found with Id: "+bookId));
        User user=((User)connectedUser.getPrincipal());
        var bookCover=fileStorageService.saveFile(file,user.getId());
        book.setBookCover(bookCover);
        bookRepository.save(book);
    }
}
