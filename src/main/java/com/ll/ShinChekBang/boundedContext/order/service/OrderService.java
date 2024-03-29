package com.ll.ShinChekBang.boundedContext.order.service;

import com.ll.ShinChekBang.base.result.RsData;
import com.ll.ShinChekBang.base.ut.Utils;
import com.ll.ShinChekBang.boundedContext.book.entity.Book;
import com.ll.ShinChekBang.boundedContext.book.repository.BookRepository;
import com.ll.ShinChekBang.boundedContext.member.entity.Member;
import com.ll.ShinChekBang.boundedContext.member.repository.MemberRepository;
import com.ll.ShinChekBang.boundedContext.order.repository.BillRepository;
import com.ll.ShinChekBang.boundedContext.order.repository.OrderRepository;
import com.ll.ShinChekBang.boundedContext.order.temporary.Bill;
import com.ll.ShinChekBang.boundedContext.order.entity.Order;
import com.ll.ShinChekBang.boundedContext.order.temporary.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BillRepository billRepository;
    private final RestTemplate restTemplate;
    private final String TOSS_PG_URL = "https://api.tosspayments.com/v1/payments/";
    private final int PAGE_SIZE = 5;
    @Value("${api.tosspayments.secret}")
    private String tossSecretKey;
    @Transactional
    public RsData<Order> order(Member member, List<Book> orderBooks, int paymentAmount) {
        List<Bill.BookVO> bookVOS = orderBooks.stream()
                .map(book -> new Bill.BookVO(book.getId(), book.getTitle(), book.getTitle(), book.getPrice()))
                .toList();
        Bill bill = new Bill(UUID.randomUUID().toString(), bookVOS, paymentAmount, member.getUsername() + "의 초기주문");

        return order(member, bill);
    }

    @Transactional
    public RsData<Order> order(Member member, Bill receipt) {
        List<Long> bookIds = receipt.getBooks().stream().map(Bill.BookVO::getId).toList();
        List<Book> books = bookRepository.findAllById(bookIds);

        books.forEach(Book::increaseSell);

        Order order = Order.builder()
                .member(member)
                .books(books)
                .paymentAccount(receipt.getPaymentAmount())
                .build();
        member.getOrders().add(order);
        orderRepository.save(order);
        memberRepository.save(member);
        memberRepository.flush();

        return RsData.of("S-12", "주문을 생성했습니다.", order);
    }

    public Bill makeBill(List<Book> books) {
        // 추후에 할인정책에 따라 결제총액 계산방법이 변경될 수도 있음
        int paymentAmount = books.stream().mapToInt(Book::getPrice).sum();
        Bill bill = new Bill( books, paymentAmount);
        billRepository.save(bill);

        return bill;
    }

    @Transactional
    public RsData<Order> pay(Member member, String orderId, int amount, String paymentKey) {
        HttpHeaders headers = new HttpHeaders();

        HttpEntity requestEntity = getRequestEntity(orderId, amount, headers);

        ResponseEntity<String> approvedPayment = restTemplate.postForEntity(
                TOSS_PG_URL + paymentKey,
                requestEntity,
                String.class
        );

        if (approvedPayment.getStatusCode().is2xxSuccessful()) {
            Bill bill = billRepository.findById(orderId)
                    .orElseThrow(() -> {
                        billRepository.deleteById(orderId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "주문정보를 찾을 수 없습니다.");
                    });
            List<Long> bookIds = bill.getBooks().stream().map(Bill.BookVO::getId).toList();
            List<Book> books = bookRepository.findAllById(bookIds);
            int paymentAmount = bill.getPaymentAmount();

            books.forEach(Book::increaseSell);

            Order order = Order.builder()
                    .member(member)
                    .books(books)
                    .paymentAccount(paymentAmount)
                    .orderId(orderId)
                    .build();
            billRepository.deleteById(orderId);

            return RsData.successOf(orderRepository.save(order));
        }

        billRepository.deleteById(orderId);
        return RsData.of("F-20", "잘못된 결제요청입니다.");
    }

    private HttpEntity getRequestEntity(String orderId, int amount, HttpHeaders headers) {
        headers.setBasicAuth(Utils.getPGAuthorizations(tossSecretKey+":"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        String params = getParamsJson(orderId, amount);
        HttpEntity data = new HttpEntity<>(params, headers);
        return data;
    }

    private String getParamsJson(String orderId, int amount) {
        return """
                {
                    "orderId" : "%s",
                    "amount" : %d
                }
                """.formatted(orderId, amount);
    }

    public Page<Receipt> getOrders(Member member, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<Order> orders = orderRepository.findTop5ByMemberOrderByCreateDateDesc(member, pageable);
        return orders.map(Receipt::new);
    }
}
