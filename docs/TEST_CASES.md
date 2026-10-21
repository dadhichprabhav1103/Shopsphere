# Test cases

"Auto" = covered by `tests/e2e_http_test.py` (HTTP) or `tests/ServiceIT.java` (service/database). All automated checks passed on the final code.

| # | Area | Steps | Expected | Auto |
|---|---|---|---|---|
| 1 | Register | Fill form with valid data | Account created, redirected to login with message | yes |
| 2 | Register | Existing email | "already exists" | yes |
| 3 | Register | Weak password / bad mobile | Validation message | yes |
| 4 | Login | Valid credentials | Home page showing user name | yes |
| 5 | Login | Wrong password | "Invalid email or password" (same text as unknown email) | yes |
| 6 | Login | 5 wrong attempts in one session | Further attempts refused | manual |
| 7 | Logout | Click Logout, press Back | Login page; protected pages need login again | yes |
| 8 | Search | `/products?q=java` | Only matching products | yes |
| 9 | Filter / sort | Category, min/max price, sort by price | Correct subset and order | yes |
| 10 | Injection | Search for `' OR '1'='1` | No results, no error | yes |
| 11 | XSS | Review text `<b>x</b>` | Displayed as text, not HTML | yes |
| 12 | Cart | Add same product twice | One line, quantity added | yes |
| 13 | Cart | Add more than stock / out-of-stock item | Rejected with message | yes |
| 14 | Cart | Change quantity, set 0 | Totals recalculated, line removed at 0 | yes |
| 15 | Wishlist | Add, move to cart, remove | Works, item leaves wishlist on move | yes |
| 16 | Checkout | No address | Place order disabled; add address works | yes |
| 17 | Coupon | `WELCOME10` | 10 % off capped at Rs. 500 | yes |
| 18 | Coupon | `EXPIRED5`, unknown code, below minimum, second use | Specific error each time | yes |
| 19 | Payment | UPI without `@`, card not 16 digits | Rejected | yes |
| 20 | Order | Place order (UPI) | Success page, PAID, stock reduced, cart emptied | yes |
| 21 | Order | Place order (COD) | PENDING until delivered | yes |
| 22 | Transaction | Force failure mid-checkout | Nothing saved (rollback) | design |
| 23 | Orders | Open another customer's order id | "Order not found" | yes |
| 24 | Orders | Cancel PLACED order | Cancelled, stock returned, refund status | yes |
| 25 | Orders | Cancel SHIPPED order | Refused | yes |
| 26 | Review | Review without purchase / after purchase | Refused / saved, average updated | yes |
| 27 | Profile | Update name, change password, add/delete address | Saved with message | yes |
| 28 | Access | Customer opens `/admin/dashboard` | 403 "Access denied" | yes |
| 29 | Access | Not logged in opens `/cart`, `/cart.jsp` | Redirect to login | yes |
| 30 | CSRF | POST without token | 403 | yes |
| 31 | Admin | Login as customer on admin login | Refused | yes |
| 32 | Admin | Create/edit product, invalid price | Saved / validation error | yes |
| 33 | Admin | Category, coupon create | Saved and listed | yes |
| 34 | Admin | Update order status | Customer sees new status | yes |
| 35 | Admin | Set stock, low-stock view | Updated; low items highlighted | yes |
| 36 | Admin | Block customer | Customer cannot log in | manual |
| 37 | i18n | Click language link | UI switches to Hindi and back | yes |
| 38 | Errors | `/nonexistent` | Custom 404 page | yes |
| 39 | Socket | `GET_PRODUCT`, `GET_STOCK`, `SEARCH_PRODUCT`, bad input | Correct objects / error strings | run manually |
| 40 | RMI | Client lookup, get/update stock | Values returned, DB updated | run manually |
| 41 | Swing | Login, open Products/Orders/Inventory | Windows show live data | run manually |

Run the automated tests:

```bash
mysql -u root -p < database/shopsphere.sql                  # fresh data
python3 tests/e2e_http_test.py                              # needs the WAR running on :8080
# service tests (adjust classpath to your jars):
javac -cp common/target/classes -d /tmp/it tests/ServiceIT.java
java  -cp /tmp/it:common/target/classes:mysql-connector-j.jar ServiceIT
```
