package com.ll.ShinChekBang.boundedContext.member.entity;

import com.ll.ShinChekBang.base.entity.BaseEntity;
import com.ll.ShinChekBang.boundedContext.book.entity.Book;
import com.ll.ShinChekBang.boundedContext.cart.entity.Cart;
import com.ll.ShinChekBang.boundedContext.order.entity.Order;
import com.ll.ShinChekBang.boundedContext.review.entity.Review;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class Member extends BaseEntity {
    @Column(unique = true)
    private String username;
    private String password;

    @Column(unique = true)
    private String email;

    @Setter
    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Cart cart;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "member")
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "member")
    @Builder.Default
    @ToString.Exclude
    private List<Order> orders = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "Library")
    @Builder.Default
    @ToString.Exclude
    private List<Book> Books = new ArrayList<>();

    public List<? extends GrantedAuthority> getGrantedAuthority() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority("member"));

        if (username.equals("admin")) {
            authorities.add(new SimpleGrantedAuthority("admin"));
        }

        return authorities;
    }
}
