package com.ll.ShinChekBang.boundedContext.review.service;

import com.ll.ShinChekBang.base.result.RsData;
import com.ll.ShinChekBang.boundedContext.book.entity.Book;
import com.ll.ShinChekBang.boundedContext.book.repository.BookRepository;
import com.ll.ShinChekBang.boundedContext.member.entity.Member;
import com.ll.ShinChekBang.boundedContext.review.entity.Review;
import com.ll.ShinChekBang.boundedContext.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;

    @Transactional
    public RsData<Review> review(Member member, Book book, float rate, String content) {
        Optional<Review> byMemberAndBook = reviewRepository.findByMemberAndBook(member, book);
        if (byMemberAndBook.isPresent()) {
            Review review = byMemberAndBook.get();
            review.setRate(rate);
            review.setContent(content);
            Review modifiedReview = reviewRepository.save(review);
            return RsData.of("S-9", "리뷰를 성공적으로 수정했습니다.", modifiedReview);
        }

        Review review = Review.builder()
                .member(member)
                .rate(rate)
                .content(content)
                .build();
        book.addReview(review);
        bookRepository.save(book);
        bookRepository.flush();
        return RsData.of("S-8", "리뷰를 성공적으로 작성했습니다.", review);
    }
}
