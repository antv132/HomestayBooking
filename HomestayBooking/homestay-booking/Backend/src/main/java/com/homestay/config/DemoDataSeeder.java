package com.homestay.config;

import com.homestay.entity.Booking;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.Floor;
import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.Promotion;
import com.homestay.entity.Property;
import com.homestay.entity.Review;
import com.homestay.entity.Room;
import com.homestay.entity.RoomImage;
import com.homestay.entity.User;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.FloorRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PromotionRepository;
import com.homestay.repository.PropertyRepository;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomImageRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Seed dữ liệu demo khi {@code app.seed.enabled=true}: tài khoản 4 vai trò, 6 homestay
 * (tầng, phòng, ảnh), khuyến mãi, booking đã trả phòng kèm đánh giá.
 * Chỉ seed homestay khi DB chưa có property nào; tài khoản chỉ tạo nếu email chưa tồn tại.
 * Reset: chạy scripts/reset-demo.sql rồi restart backend.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.30");

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final FloorRepository floorRepository;
    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final ManagerPropertyAssignmentRepository managerAssignmentRepository;
    private final EmployeePropertyAssignmentRepository employeeAssignmentRepository;
    private final PromotionRepository promotionRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    private record UserSeed(String fullName, String email, String password, String phone, User.Role role) {}

    private static final String STAFF_PASSWORD = "A12345678";

    /** Admin + khách hàng — khớp AccTest.txt ở thư mục gốc dự án. Manager/Employee tạo theo từng homestay. */
    private static final UserSeed[] USERS = {
            new UserSeed("Quản trị viên", "admin@homestay.local", "Admin@123456", "0900000001", User.Role.ADMIN),
            new UserSeed("Khách hàng Thành", "thanhnxhe181198@fpt.edu.vn", "A1234567", "0900000002", User.Role.CUSTOMER),
            new UserSeed("Nguyễn Văn An", "an.nguyen@demo.com", "A1234567", "0900000003", User.Role.CUSTOMER),
            new UserSeed("Trần Thị Bình", "binh.tran@demo.com", "A1234567", "0900000004", User.Role.CUSTOMER),
    };

    /** Loại phòng: hệ số giá so với giá gốc của homestay. */
    private record RoomTemplate(String type, double priceFactor, int capacity, int area, List<String> amenities) {}

    private static final RoomTemplate STANDARD = new RoomTemplate("Standard", 1.0, 2, 22,
            List.of("WiFi", "Điều hòa", "TV"));
    private static final RoomTemplate DELUXE = new RoomTemplate("Deluxe", 1.4, 2, 30,
            List.of("WiFi", "Điều hòa", "Smart TV", "Minibar", "Ban công"));
    private static final RoomTemplate FAMILY = new RoomTemplate("Family", 1.9, 4, 42,
            List.of("WiFi", "Điều hòa", "Smart TV", "Bếp nhỏ"));
    private static final RoomTemplate SUITE = new RoomTemplate("Suite", 2.5, 3, 50,
            List.of("WiFi", "Điều hòa", "Smart TV", "Minibar", "Ban công", "Room service"));

    /**
     * Mỗi homestay có bộ ảnh riêng (ảnh đầu = cảnh đặc trưng địa điểm) và Manager + Employee riêng.
     * {@code managerEmail} null = manager.{slug}@homestay.local.
     */
    private record PropertySeed(String name, String city, String slug, String managerEmail, String address,
                                String description, long basePrice, List<String> extraAmenities, List<String> photos) {}

    private static final PropertySeed[] PROPERTIES = {
            new PropertySeed("Hà Nội Old Quarter Inn", "Hà Nội", "hanoi", "viet@gmail.com", "25 Hàng Bạc, Hà Nội",
                    "Homestay phố cổ Hà Nội, đi bộ tới Hồ Hoàn Kiếm.", 650_000, List.of(),
                    List.of("photo-1555921015-5532091f6026", "photo-1611892440504-42a792e24d32",
                            "photo-1559599238-308793637427", "photo-1505693416388-ac5ce068fe85")),
            new PropertySeed("Sunset Resort Đà Nẵng", "Đà Nẵng", "danang", null, "123 Nguyễn Tất Thành, Đà Nẵng",
                    "Resort sát biển Thanh Khê, ngắm hoàng hôn từ ban công.", 1_200_000, List.of("View biển", "Bãi đỗ xe"),
                    List.of("photo-1571896349842-33c89424de2d", "photo-1586611292717-f828b167408c",
                            "photo-1618773928121-c32242e63f39", "photo-1582719478250-c89cae4dc85b")),
            new PropertySeed("Hội An Garden Villa", "Hội An", "hoian", null, "78 Phan Bội Châu, Hội An",
                    "Biệt thự sân vườn yên tĩnh, đạp xe 5 phút tới phố cổ Hội An.", 950_000, List.of("Bãi đỗ xe"),
                    List.of("photo-1566073771259-6a8506099945", "photo-1502672260266-1c1ef2d93688",
                            "photo-1590490360182-c33d57733427", "photo-1595576508898-0ad5c879a061")),
            new PropertySeed("Phú Quốc Beach House", "Phú Quốc", "phuquoc", null, "12 Trần Hưng Đạo, Phú Quốc",
                    "Nhà nghỉ bên bãi Trường, có hồ bơi và bar ven biển.", 1_500_000, List.of("View biển", "Hồ bơi riêng"),
                    List.of("photo-1499793983690-e29da59ef1c2", "photo-1584132967334-10e028bd69f7",
                            "photo-1520483601560-389dff434fdf", "photo-1578683010236-d716f9a3f461")),
            new PropertySeed("Mountain View Đà Lạt", "Đà Lạt", "dalat", null, "456 Trần Phú, Đà Lạt",
                    "Homestay đồi thông, view núi và sương sớm Đà Lạt.", 700_000, List.of("Bãi đỗ xe"),
                    List.of("photo-1596394516093-501ba68a0ba6", "photo-1445019980597-93fa8acb246c",
                            "photo-1470246973918-29a93221c455", "photo-1631049307264-da0ec9d70304")),
            new PropertySeed("Nha Trang Ocean Homestay", "Nha Trang", "nhatrang", null, "88 Trần Phú, Nha Trang",
                    "Căn hộ homestay đối diện biển Nha Trang, gần chợ đêm.", 850_000, List.of("View biển"),
                    List.of("photo-1615460549969-36fa19521a4f", "photo-1560448204-e02f11c3d0e2",
                            "photo-1522708323590-d24dbb6b0267", "photo-1560185007-cde436f6a4d0")),
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<User> users = seedUsers();
        if (propertyRepository.count() == 0) {
            List<Room> rooms = seedProperties(users.get(0));
            seedBookingsWithReviews(rooms, users.get(2), users.get(3));
        }
        if (promotionRepository.count() == 0) {
            seedPromotions();
        }
    }

    private List<User> seedUsers() {
        List<User> result = new ArrayList<>();
        for (UserSeed seed : USERS) {
            result.add(ensureUser(seed));
        }
        return result;
    }

    private User ensureUser(UserSeed seed) {
        return userRepository.findByEmail(seed.email()).orElseGet(() -> {
            User user = new User();
            user.setFullName(seed.fullName());
            user.setEmail(seed.email());
            user.setPasswordHash(passwordEncoder.encode(seed.password()));
            user.setPhone(seed.phone());
            user.setRole(seed.role());
            user.setStatus(User.Status.ACTIVE);
            log.info("Seeded demo user {} ({})", seed.email(), seed.role());
            return userRepository.save(user);
        });
    }

    private List<Room> seedProperties(User admin) {
        List<Room> allRooms = new ArrayList<>();
        for (int p = 0; p < PROPERTIES.length; p++) {
            PropertySeed seed = PROPERTIES[p];
            String managerEmail = seed.managerEmail() != null
                    ? seed.managerEmail() : "manager." + seed.slug() + "@homestay.local";
            User manager = ensureUser(new UserSeed("Quản lý " + seed.city(), managerEmail, STAFF_PASSWORD,
                    String.format("09110000%02d", p + 1), User.Role.MANAGER));
            User employee = ensureUser(new UserSeed("Nhân viên " + seed.city(),
                    "employee." + seed.slug() + "@homestay.local", STAFF_PASSWORD,
                    String.format("09120000%02d", p + 1), User.Role.EMPLOYEE));

            Property property = new Property();
            property.setName(seed.name());
            property.setAddress(seed.address());
            property.setDescription(seed.description());
            property.setStatus(Property.Status.ACTIVE);
            property = propertyRepository.save(property);

            Floor floor1 = saveFloor(property, 1);
            Floor floor2 = saveFloor(property, 2);
            allRooms.add(saveRoom(property, floor1, "101", 0, STANDARD, seed));
            allRooms.add(saveRoom(property, floor1, "102", 1, DELUXE, seed));
            allRooms.add(saveRoom(property, floor2, "201", 2, FAMILY, seed));
            allRooms.add(saveRoom(property, floor2, "202", 3, SUITE, seed));

            ManagerPropertyAssignment ma = new ManagerPropertyAssignment();
            ma.setManager(manager);
            ma.setProperty(property);
            ma.setAssignedBy(admin);
            ma.setAssignedAt(LocalDateTime.now());
            managerAssignmentRepository.save(ma);

            // Employee: at most one ACTIVE assignment (uq_epa_one_active_per_employee)
            EmployeePropertyAssignment ea = new EmployeePropertyAssignment();
            ea.setEmployee(employee);
            ea.setProperty(property);
            ea.setAssignedBy(admin);
            ea.setAssignedAt(LocalDateTime.now());
            employeeAssignmentRepository.save(ea);
        }
        log.info("Seeded {} demo properties with {} rooms", PROPERTIES.length, allRooms.size());
        return allRooms;
    }

    private Floor saveFloor(Property property, int number) {
        Floor floor = new Floor();
        floor.setProperty(property);
        floor.setFloorNumber(number);
        floor.setDescription("Tầng " + number);
        return floorRepository.save(floor);
    }

    private Room saveRoom(Property property, Floor floor, String number, int position, RoomTemplate template,
                          PropertySeed seed) {
        Room room = new Room();
        room.setProperty(property);
        room.setFloor(floor);
        room.setRoomNumber(number);
        room.setRoomType(template.type());
        room.setPricePerNight(BigDecimal.valueOf(Math.round(seed.basePrice() * template.priceFactor() / 10_000) * 10_000));
        room.setCapacity(template.capacity());
        room.setArea(BigDecimal.valueOf(template.area()));
        room.setDescription("Phòng " + template.type() + " " + template.area() + "m², tối đa "
                + template.capacity() + " khách tại " + seed.name() + ".");
        List<String> amenities = new ArrayList<>(template.amenities());
        amenities.addAll(seed.extraAmenities());
        room.setAmenities(amenities);
        room.setStatus(Room.Status.AVAILABLE);
        room = roomRepository.save(room);

        // Gallery: bộ ảnh của homestay, xoay theo vị trí phòng để mỗi phòng có ảnh đại diện khác nhau
        List<String> photos = new ArrayList<>(seed.photos());
        Collections.rotate(photos, -position);
        for (int i = 0; i < photos.size(); i++) {
            RoomImage image = new RoomImage();
            image.setRoom(room);
            image.setImageUrl("https://images.unsplash.com/" + photos.get(i) + "?w=1200&q=80");
            image.setIsPrimary(i == 0);
            image.setSortOrder(i);
            roomImageRepository.save(image);
        }
        return room;
    }

    private record StaySeed(int roomIndex, int customer, int daysAgo, int nights, int guests, int rating, String comment) {}

    private static final StaySeed[] STAYS = {
            new StaySeed(0, 0, 40, 2, 2, 5, "Vị trí tuyệt vời ngay phố cổ, chủ nhà rất nhiệt tình."),
            new StaySeed(1, 1, 25, 3, 2, 4, "Phòng sạch, ban công đẹp, buổi tối hơi ồn."),
            new StaySeed(5, 0, 60, 3, 2, 5, "Hoàng hôn từ ban công đẹp không tưởng, sẽ quay lại!"),
            new StaySeed(7, 1, 18, 2, 3, 5, "Suite rộng, view biển, nhân viên phục vụ chu đáo."),
            new StaySeed(9, 1, 33, 2, 2, 4, "Sân vườn yên tĩnh, đạp xe ra phố cổ rất tiện."),
            new StaySeed(14, 0, 12, 4, 4, 5, "Hồ bơi đẹp, gia đình mình rất thích."),
            new StaySeed(16, 1, 50, 2, 2, 3, "Không khí trong lành nhưng đường lên hơi dốc."),
            new StaySeed(21, 0, 8, 2, 2, 4, "Gần biển và chợ đêm, giá hợp lý."),
    };

    private void seedBookingsWithReviews(List<Room> rooms, User... customers) {
        LocalDate today = LocalDate.now();
        for (StaySeed stay : STAYS) {
            Room room = rooms.get(stay.roomIndex());
            User customer = customers[stay.customer()];
            LocalDate checkIn = today.minusDays(stay.daysAgo());
            LocalDate checkOut = checkIn.plusDays(stay.nights());

            BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(checkIn, checkOut)));
            BigDecimal deposit = total.multiply(DEPOSIT_RATE).setScale(0, RoundingMode.HALF_UP);
            Booking booking = new Booking();
            booking.setCustomer(customer);
            booking.setRoom(room);
            booking.setCheckInDate(checkIn);
            booking.setCheckOutDate(checkOut);
            booking.setGuestCount(stay.guests());
            booking.setTotalAmount(total);
            booking.setDepositAmount(deposit);
            booking.setRemainingAmount(total.subtract(deposit));
            booking.setStatus(Booking.Status.CHECKED_OUT);
            booking = bookingRepository.save(booking);

            Review review = new Review();
            review.setBooking(booking);
            review.setCustomer(customer);
            review.setRoom(room);
            review.setRating(stay.rating());
            review.setComment(stay.comment());
            review.setStatus(Review.Status.PUBLISHED);
            reviewRepository.save(review);

            // Denormalized aggregates (FR-14)
            int count = room.getTotalReviews() + 1;
            BigDecimal sum = (room.getAverageRating() == null ? BigDecimal.ZERO
                    : room.getAverageRating().multiply(BigDecimal.valueOf(room.getTotalReviews())))
                    .add(BigDecimal.valueOf(stay.rating()));
            room.setTotalReviews(count);
            room.setAverageRating(sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
            roomRepository.save(room);
        }
        log.info("Seeded {} checked-out demo bookings with reviews", STAYS.length);
    }

    private void seedPromotions() {
        savePromotion(0, "Ưu đãi cuối tuần", "Giảm 20%\nthứ 6 – chủ nhật",
                "Áp dụng cho phòng trống cuối tuần tại tất cả homestay.", "Đặt ngay →", "red",
                "photo-1540541338287-41700207dee6");
        savePromotion(1, "Đặt sớm hè 2026", "Combo 3 đêm\n+ bữa sáng miễn phí",
                "Ưu đãi có hạn — đặt trước 31/08/2026.", "Khám phá →", "blue",
                "photo-1507525428034-b723cf961d3e");
        savePromotion(2, "Lưu trú dài hạn", "Giảm thêm 15%\ncho booking từ 5 đêm",
                "Lý tưởng cho kỳ nghỉ dài ngày hoặc công tác.", "Xem phòng →", "orange",
                "photo-1520250497591-112f2f40a3f4");
        log.info("Seeded 3 demo promotions");
    }

    private void savePromotion(int sort, String subtitle, String title, String description, String cta, String color,
                               String photo) {
        Promotion promotion = new Promotion();
        promotion.setSubtitle(subtitle);
        promotion.setTitle(title);
        promotion.setDescription(description);
        promotion.setCtaText(cta);
        promotion.setCtaUrl("/rooms");
        promotion.setColorTheme(color);
        promotion.setImageUrl("https://images.unsplash.com/" + photo + "?w=1920&q=80");
        promotion.setActive(true);
        promotion.setSortOrder(sort);
        promotionRepository.save(promotion);
    }
}
