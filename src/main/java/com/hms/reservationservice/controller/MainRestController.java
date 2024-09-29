package com.hms.reservationservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hms.reservationservice.BookingDetailRepository;
import com.hms.reservationservice.RequestIdExtractor;
import com.hms.reservationservice.kafka.KafkaProducer;
import com.hms.reservationservice.model.BookRoomRequest;
import com.hms.reservationservice.model.BookingDetail;
import com.hms.reservationservice.model.BookingStatus;
import com.hms.reservationservice.model.BookingStatusRequest;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.CircuitBreaker;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.UUID;

@Tag(name = "Reservation Service", description = "Reservation Service APIs")
@RestController
@RequestMapping("/api/v1")
public class MainRestController {
    private static final Logger log = LoggerFactory.getLogger(MainRestController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final String BOOKING_MESSAGE = "Room booking initiated. Please check the status for your booking!";
    private static final Set VALID_ROOMS = new HashSet<>(Arrays.asList("101", "102", "103", "104", "105", "106", "201",
            "202", "203", "204", "301", "302"));

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private final WebClient hotelManagementServiceClientGetAllRooms;

    @Autowired
    private final WebClient hotelManagementServiceClientGetRoomAvailability;

    @Autowired
    private final WebClient paymentServiceClientGetPaymentStatus;

    @Autowired
    private final BookingDetailRepository bookingDetailRepository;

    @Autowired
    private final RequestIdExtractor requestIdExtractor;

    @Autowired
    private final KafkaProducer kafkaProducer;

    @Autowired
    public MainRestController(WebClient hotelManagementServiceClientGetAllRooms,
                              WebClient hotelManagementServiceClientGetRoomAvailability,
                              WebClient paymentServiceClientGetPaymentStatus,
                              BookingDetailRepository bookingDetailRepository, RequestIdExtractor requestIdExtractor, KafkaProducer kafkaProducer) {
        this.hotelManagementServiceClientGetAllRooms = hotelManagementServiceClientGetAllRooms;
        this.hotelManagementServiceClientGetRoomAvailability = hotelManagementServiceClientGetRoomAvailability;
        this.paymentServiceClientGetPaymentStatus = paymentServiceClientGetPaymentStatus;
        this.bookingDetailRepository = bookingDetailRepository;
        this.requestIdExtractor = requestIdExtractor;
        this.kafkaProducer = kafkaProducer;
    }

    @GetMapping("/getAvailableRooms")
    public ResponseEntity<JsonNode> getAvailableRooms() {

        Mono<JsonNode> responseMono = hotelManagementServiceClientGetAllRooms
                .get()
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseJsonResponse);

        return ResponseEntity.ok(responseMono.block());
    }

    private JsonNode parseJsonResponse(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    @PostMapping("/bookRoom")
    public ResponseEntity<String> bookRoom(@RequestBody BookRoomRequest request) {

        logRequest(request);

        if (!validRoom(request)) {
            ResponseEntity.badRequest().body("Invalid room, please try again!");
        } else if (!validDates(request)) {
            ResponseEntity.badRequest().body("Invalid Date, please try again!");
        } else if (!validCustomer(request)) {
            ResponseEntity.badRequest().body("Invalid Customer, please try again!");
        }

        BookingDetail bookingDetail = createBookingDetailsFromRequest(request);
        bookingDetailRepository.save(bookingDetail);

        return ResponseEntity.ok(BOOKING_MESSAGE + String.format(" BookingId: %s", bookingDetail.getId()));

    }

    private void logRequest(BookRoomRequest request) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            log.info("Incoming book room request: {}", requestJson);
        } catch (Exception e) {
            log.error("Error logging book room request", e);
        }
    }

    private boolean validCustomer(BookRoomRequest request) {
        try {
            UUID.fromString(request.getCustomerId());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private BookingDetail createBookingDetailsFromRequest(BookRoomRequest request) {
        BookingDetail bookingDetail = new BookingDetail();
        bookingDetail.setId(UUID.randomUUID());
        bookingDetail.setCheckindate(request.getCheckInDate());
        bookingDetail.setCheckoutdate(request.getCheckOutDate());
        bookingDetail.setRoomno(Integer.valueOf(request.getRoomId()));
        bookingDetail.setCustomerid(UUID.fromString(request.getCustomerId()));
        return bookingDetail;
    }

    private boolean validDates(BookRoomRequest request) {
        return request.getCheckInDate() != null
                && request.getCheckOutDate() != null
                && !request.getCheckInDate().isBefore(LocalDate.now())
                && !request.getCheckInDate().isBefore(request.getCheckOutDate());
    }

    private List<Cookie> getCookieList(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        return (cookies != null) ? Arrays.asList(cookies) : new ArrayList<>();
    }

    private boolean validRoom(BookRoomRequest request) {
        return StringUtils.isNotEmpty(request.getRoomId()) && VALID_ROOMS.contains(request.getRoomId());
    }

    @CircuitBreaker()
    @RateLimiter(name = "hotelManagementService")
    @Retry(name = "hotelManagementService")
    @PostMapping("/getBookingStatus")
    public ResponseEntity<String> getBookingStatus(@RequestBody BookingStatusRequest bookingStatusRequest,
                                                   HttpServletRequest request, HttpServletResponse servletResponse) throws JsonProcessingException {
        UUID bookingId = UUID.fromString(bookingStatusRequest.getBookingId());
        List<Cookie> cookieList = getCookieList(request);
        logCookies(cookieList);

        if (cookieList.stream().noneMatch(cookie -> cookie.getName().startsWith("rs-1"))) {
            BookingStatus bookingStatus = new BookingStatus();
            String requestId = requestIdExtractor.getRequestId(request);
            Cookie cookie1 = new Cookie("rs-1", "rs-1-" + requestId);
            cookie1.setMaxAge(3600);

            Integer roomId;
            Optional<BookingDetail> bookingDetail = bookingDetailRepository.findById(bookingId);
            if (bookingDetail.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid bookingId, booking details not present!");
            } else {
                log.info("Booking Details -> {}", bookingDetail.get());
                roomId = bookingDetail.get().getRoomno();
            }

            //STEP 1: ASYNC REQUEST TO HMS
            processHotelManagementAsync(roomId, cookie1, bookingStatus);

            //STEP 2: ASYNC REQUEST TO PaymentService
            processPaymentServiceAsync(bookingId, cookie1, bookingStatus);

            //STEP 3: Sending cookie and update response back to the user.
            servletResponse.addCookie(cookie1);
            return ResponseEntity.ok().body("Booking order recieved. We're processing your booking and will soon provide an update on your stay. " +
                    "Thank you for choosing Suite Spott");
        } else {
            BookingStatus bookingStatus = null;
            Optional<Cookie> cookie = cookieList.stream().filter(c -> c.getName().startsWith("rs-1")).findAny();
            if (cookie.isPresent()) {
                bookingStatus = (BookingStatus) redisTemplate.opsForValue().get(cookie.get().getValue());
            }

            if (bookingStatus == null) {
                return ResponseEntity.notFound().build();
            } else if (!bookingStatus.getHotelManagementServiceResponse()) {
                return ResponseEntity.ok().body("Apologies, The desired room is already booked. " +
                        "Please try again with another room.");
            } else if (bookingStatus.getPaymentServiceResponse() == null) {
                return ResponseEntity.ok().body("Congratulations, room is available, checking payment status, please wait.");
            } else if (!bookingStatus.getPaymentServiceResponse()) {
                return ResponseEntity.ok().body("Apologies, your payment has failed. " +
                        "Any amount deducted will be refunded within 7 business days. Please try again!");
            } else {
                bookingDetailRepository.updateBookingstatusBy(bookingId, true);
                //kafkaProducer.pubBookingConfirmedEvent(bookingId);
                return ResponseEntity.ok().body("Booking successful. Have a wonderful Stay!");
            }
        }
    }

    private void processPaymentServiceAsync(UUID bookingId, Cookie cookie1, BookingStatus bookingStatus) {
        Mono<Boolean> paymentServiceResponse = paymentServiceClientGetPaymentStatus
                .post()
                .body(Mono.just(bookingId), UUID.class)
                .retrieve()
                .bodyToMono(Boolean.class); // ASYNCHRONOUS

        paymentServiceResponse.subscribe(
                response2 -> {
                    log.info("response from the payment service -> {}", response2);
                    bookingStatus.setPaymentServiceResponse(response2);
                    redisTemplate.opsForValue().set(cookie1.getValue(), bookingStatus);
                },
                error2 -> log.info("error processing the response " + error2));
    }

    private void processHotelManagementAsync(Integer roomId, Cookie cookie1, BookingStatus bookingStatus) {
        Mono<Boolean> hotelManagementServiceResponse = hotelManagementServiceClientGetRoomAvailability
                .post()
                .body(Mono.just(roomId), Integer.class)
                .retrieve()
                .bodyToMono(Boolean.class); // ASYNCHRONOUS

        hotelManagementServiceResponse.subscribe( // ASYNC RESPONSE HANDLER
                response1 -> { // SUCCESS HANDLER
                    log.info("response from the hotelManagement service -> {}", response1);
                    bookingStatus.setHotelManagementServiceResponse(response1);
                    redisTemplate.opsForValue().set(String.valueOf(cookie1.getValue()), bookingStatus);
                },
                error1 -> log.info("error processing the response " + error1));
    }

    private void logCookies(List<Cookie> cookieList) {
        if (cookieList.isEmpty()) {
            log.info("No cookies present in the request");
        } else {
            StringBuilder cookieLog = new StringBuilder("Cookies in the request:\n");
            for (Cookie cookie : cookieList) {
                cookieLog.append(String.format("Name: %s, Value: %s, Domain: %s, Path: %s\n",
                        cookie.getName(),
                        cookie.getValue(),
                        cookie.getDomain(),
                        cookie.getPath()));
            }
            log.info(cookieLog.toString());
        }
    }
}
