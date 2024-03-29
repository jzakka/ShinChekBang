package com.ll.ShinChekBang.boundedContext.order.entity;

import com.ll.ShinChekBang.base.entity.BaseEntity;
import com.ll.ShinChekBang.boundedContext.book.entity.Book;
import com.ll.ShinChekBang.boundedContext.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Table(name = "orders")
public class Order extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "memberId")
    @ToString.Exclude
    private Member member;

    private int paymentAccount;

    @Column(unique = true)
    private String orderId;

    @ManyToMany
    @Builder.Default
    @ToString.Exclude
    private List<Book> books = new ArrayList<>();

}
