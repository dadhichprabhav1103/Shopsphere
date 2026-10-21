import com.shopsphere.model.*;
import com.shopsphere.service.*;
import java.util.*;

public class ServiceIT {
    static int pass = 0, fail = 0;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS  " : "FAIL  ") + name); if (ok) pass++; else fail++; }
    static boolean fails(Callable r) { try { r.run(); return false; } catch (ServiceException e) { return true; } catch (Exception e) { e.printStackTrace(); return false; } }
    interface Callable { void run() throws Exception; }

    public static void main(String[] a) throws Exception {
        AuthService auth = new AuthService();
        ProductService ps = new ProductService();
        CartService cart = new CartService();
        CheckoutService co = new CheckoutService();
        OrderService os = new OrderService();
        AdminService admin = new AdminService();
        ReviewService rs = new ReviewService();

        String email = "t" + System.currentTimeMillis() + "@test.com";
        User u = auth.register("Test User", email, "Passw0rd1", "Passw0rd1", "9876543210");
        check("valid registration", u.getUserId() > 0);
        check("duplicate email rejected", fails(() -> auth.register("Test User", email, "Passw0rd1", "Passw0rd1", "9876543210")));
        check("weak password rejected", fails(() -> auth.register("X Y", "w@t.com", "abc", "abc", "9876543210")));
        check("valid login", auth.login(email, "Passw0rd1").getUserId() == u.getUserId());
        check("invalid login", fails(() -> auth.login(email, "wrong")));
        check("seed admin login", auth.login("admin@shopsphere.com", "Admin@123").isAdmin());
        check("seed demo login", !auth.login("demo@shopsphere.com", "Demo@123").isAdmin());

        List<Product> found = ps.search("java", null, null, null, "name");
        check("search 'java' finds books", found.size() >= 2);
        check("sql-injection-ish keyword is harmless", ps.search("' OR '1'='1", null, null, null, null).isEmpty());
        check("category filter", ps.search(null, 3, null, null, "price_asc").size() == 3);
        check("price filter", ps.search(null, null, 100.0, 700.0, "price_desc").stream().allMatch(p -> p.getFinalPrice() <= 700));
        check("sort price_asc", isSorted(ps.search(null, null, null, null, "price_asc")));
        check("inactive hidden / out-of-stock still listed", ps.search("Bottle", null, null, null, null).size() == 1);

        Product earbuds = ps.getActive(1);
        check("final price (2999 -20% = 2399.2)", Math.abs(earbuds.getFinalPrice() - 2399.20) < 0.001);
        check("rating avg joined", earbuds.getAvgRating() == 5.0 && earbuds.getReviewCount() == 1);

        cart.add(u.getUserId(), 1, 2);
        cart.add(u.getUserId(), 1, 1);
        cart.add(u.getUserId(), 5, 1);
        CartSummary cs = cart.getCart(u.getUserId());
        check("cart merges quantities", cs.getItems().size() == 2 && cs.getItemCount() == 4);
        check("cart subtotal server-side", Math.abs(cs.getSubtotal() - (3 * 2399.20 + 419.30)) < 0.01);
        check("out-of-stock add rejected", fails(() -> cart.add(u.getUserId(), 16, 1)));
        check("over-stock add rejected", fails(() -> cart.add(u.getUserId(), 4, 9)));
        cart.update(u.getUserId(), 5, 3);
        check("change quantity recalculates", Math.abs(cart.getCart(u.getUserId()).getSubtotal() - (3 * 2399.20 + 3 * 419.30)) < 0.01);
        cart.update(u.getUserId(), 5, 0);
        check("quantity 0 removes line", cart.getCart(u.getUserId()).getItems().size() == 1);

        Address ad = new Address();
        ad.setUserId(u.getUserId()); ad.setAddressLine("1 Test Road"); ad.setCity("Jaipur"); ad.setState("Rajasthan"); ad.setPincode("302001");
        admin.addAddress(ad);
        int addrId = admin.addresses(u.getUserId()).get(0).getAddressId();
        check("bad pincode rejected", fails(() -> { Address b = new Address(); b.setUserId(1); b.setAddressLine("12345"); b.setCity("x"); b.setState("y"); b.setPincode("12"); admin.addAddress(b); }));

        check("invalid coupon", fails(() -> co.validateCoupon(u.getUserId(), "NOPE", 5000)));
        check("expired coupon", fails(() -> co.validateCoupon(u.getUserId(), "EXPIRED5", 5000)));
        check("min-order coupon", fails(() -> co.validateCoupon(u.getUserId(), "FLAT200", 100)));

        int stockBefore = new com.shopsphere.dao.ProductDAO().getStock(1);
        Order o = co.placeOrder(u.getUserId(), addrId, "welcome10", "UPI");
        double subtotal = 3 * 2399.20;
        check("order placed with coupon (10% capped at 500)", Math.abs(o.getDiscount() - Math.min(subtotal * 0.10, 500)) < 0.01);
        check("order total = subtotal - discount", Math.abs(o.getTotalAmount() - (subtotal - o.getDiscount())) < 0.01);
        check("UPI mock payment is PAID", "PAID".equals(o.getPaymentStatus()));
        check("stock reduced by 3", new com.shopsphere.dao.ProductDAO().getStock(1) == stockBefore - 3);
        check("cart emptied after order", cart.getCart(u.getUserId()).isEmpty());
        check("empty cart cannot be ordered", fails(() -> co.placeOrder(u.getUserId(), addrId, null, "COD")));

        cart.add(u.getUserId(), 3, 1);
        check("coupon cannot be reused", fails(() -> co.placeOrder(u.getUserId(), addrId, "WELCOME10", "COD")));
        check("failed checkout keeps cart", cart.getCart(u.getUserId()).getItems().size() == 1);
        check("other user's address rejected", fails(() -> co.placeOrder(u.getUserId(), 1, null, "COD")));
        Order o2 = co.placeOrder(u.getUserId(), addrId, null, "COD");
        check("COD payment is PENDING", "PENDING".equals(o2.getPaymentStatus()));

        check("order history", os.forUser(u.getUserId()).size() == 2);
        check("order details has items", os.details(o.getOrderId(), u.getUserId()).getItems().size() == 1);
        check("other customer can't view order", fails(() -> os.details(o.getOrderId(), 99999)));

        os.changeStatus(o.getOrderId(), "SHIPPED");
        check("admin updates status; customer sees it", "SHIPPED".equals(os.details(o.getOrderId(), u.getUserId()).getOrderStatus()));
        check("customer cannot cancel shipped order", fails(() -> os.cancelByCustomer(o.getOrderId(), u.getUserId())));
        int s3 = new com.shopsphere.dao.ProductDAO().getStock(3);
        os.cancelByCustomer(o2.getOrderId(), u.getUserId());
        check("cancel restores stock", new com.shopsphere.dao.ProductDAO().getStock(3) == s3 + 1);
        os.changeStatus(o.getOrderId(), "DELIVERED");
        check("delivered order cannot be changed", fails(() -> os.changeStatus(o.getOrderId(), "PLACED")));

        rs.submit(u.getUserId(), 1, 4, "Good");
        check("review after purchase updates rating", ps.getActive(1).getReviewCount() == 2);
        check("review without purchase rejected", fails(() -> rs.submit(u.getUserId(), 7, 5, "x")));

        check("dashboard numbers", os.totalOrders() >= 2 && os.revenue() > 0 && !os.topProducts().isEmpty() && admin.customerCount() >= 2);
        check("low stock list", ps.lowStock().stream().anyMatch(p -> p.getProductId() == 4));

        Product np = new Product(); np.setCategoryId(1); np.setName("Test Item"); np.setPrice(100); np.setStock(5); np.setStatus(true);
        ps.save(np);
        check("admin product insert", ps.adminList().stream().anyMatch(p -> p.getName().equals("Test Item")));
        check("invalid product rejected", fails(() -> { Product b = new Product(); b.setCategoryId(1); b.setName("Bad"); b.setPrice(-1); ps.save(b); }));

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
    static boolean isSorted(List<Product> l) { for (int i = 1; i < l.size(); i++) if (l.get(i-1).getFinalPrice() > l.get(i).getFinalPrice()) return false; return true; }
}
