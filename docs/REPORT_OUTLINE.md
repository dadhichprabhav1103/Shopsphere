# Project report outline (suggested chapters)

1. **Title page, certificate, acknowledgement, abstract** (abstract: 150 words - what ShopSphere is, technologies, result).
2. **Introduction** - problem (small shops need online ordering + admin tools), objectives, scope, limitations (mock payment, no image upload, single currency).
3. **Technologies** - Java 21, JDBC/MySQL, Servlets/JSP/JSTL (Jakarta EE 11, Tomcat 11), Swing, java.net, RMI, Maven. One paragraph each with *why chosen*.
4. **System analysis** - functional requirements (customer, admin), non-functional (security, usability, maintainability), use-case diagram (`ARCHITECTURE.md` section 4).
5. **System design** - N-tier architecture (section 1), MVC flow (section 2), ER diagram (section 3), table descriptions (`database/shopsphere.sql`), class diagram (section 6), checkout transaction (section 5), module diagram showing web / Swing / socket / RMI around one database.
6. **Implementation** - one subsection per module with 2-3 important code listings:
   * `CheckoutService.placeOrder` (transaction), `ProductDAO.search` (dynamic SQL with PreparedStatement), `CsrfFilter`, `PasswordUtil`,
   * `productCard.tag`, `FormatPriceTag`, i18n bundles,
   * Swing `ProductController`, `GenericTableModel`,
   * `InventoryServer`/`ClientHandler`, `RmiServer`/`InventoryServiceImpl`, `UrlDemo`.
7. **Screenshots** - home, listing with filters, product details, cart, checkout, order tracking, admin dashboard, admin orders, Hindi UI, Swing windows (`docs/screenshots`), socket/RMI console output. (Take the web screenshots in your own browser.)
8. **Security** - table in `ARCHITECTURE.md` section 7.
9. **Testing** - strategy (service tests, HTTP tests, manual), table from `TEST_CASES.md`, results.
10. **Conclusion and future work** - real payment gateway, image upload, email notifications, REST API/Spring migration, pagination, connection pool for all modules, role management, Docker.
11. **Syllabus mapping** (`SYLLABUS_MAPPING.md`) and **References** (Oracle Java tutorials, Jakarta EE specs, Tomcat docs, MySQL docs).
12. **Appendices** - SQL script, setup steps, user manual (login accounts).
