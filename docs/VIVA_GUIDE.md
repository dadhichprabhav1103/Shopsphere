# Viva guide - questions you should be able to answer

## Project overview
**Q. What is ShopSphere?** An e-commerce system: a Servlet/JSP storefront and admin area, a Swing desktop admin, a socket inventory server and an RMI inventory service, all on one MySQL database and one shared service layer.

**Q. Why a `common` module?** So Servlets, Swing, the socket server and the RMI server reuse the same DAOs and business rules. Fixing a rule once fixes it everywhere.

**Q. Explain the layers.** JSP (view) -> Servlet (controller) -> Service (rules) -> DAO (SQL) -> MySQL. A JSP has no SQL; a DAO has no business rules.

## JDBC
**Q. Why `PreparedStatement`?** Prevents SQL injection (values are bound, never concatenated), and the statement can be precompiled. Even the sort order is chosen from a whitelist (`ProductDAO.SORTS`).

**Q. Where do you use a transaction and why?** `CheckoutService.placeOrder`: `setAutoCommit(false)`, then order, items, stock, payment, coupon usage and cart clearing; `commit()` on success, `rollback()` on any error. Without it a failure could leave an order with no items or reduced stock without an order.

**Q. How do you stop two customers buying the last item?** `UPDATE products SET stock = stock - ? WHERE product_id = ? AND stock >= ?` - if 0 rows change, the purchase fails and the transaction rolls back.

**Q. Why `try-with-resources`?** Closes `Connection`, `Statement`, `ResultSet` automatically, even on exceptions (no leaks).

## Servlets, sessions, filters
**Q. Servlet life cycle?** Container loads class, calls `init()` once, `service()` per request (dispatches to `doGet/doPost`), `destroy()` once. Demo: `/demo/lifecycle`.

**Q. `ServletConfig` vs `ServletContext`?** Config = one servlet's init-params. Context = whole application (context-params, shared attributes).

**Q. Forward vs redirect?** Forward: same request, server side, URL unchanged (used to show JSPs). Redirect: new request, URL changes (used after POST - the Post/Redirect/Get pattern avoids duplicate form submission).

**Q. Session vs cookie?** Session data is stored on the server; the browser only holds the session id cookie (`JSESSIONID`, HttpOnly). Cookies in this project store only non-sensitive data (`lang`, `recentlyViewed`).

**Q. What does a Filter do here?** Encoding (UTF-8), locale selection, CSRF check, authentication for customer pages, role check for `/admin/*`. Order is defined in `web.xml`.

**Q. What do the listeners do?** `AppListener` logs startup and checks the database; `SessionListener` counts sessions and creates the CSRF token.

**Q. How is the password stored?** PBKDF2-HMAC-SHA256 with a random salt and 65 536 iterations (`PasswordUtil`), never plain text, compared in constant time.

**Q. What is CSRF and how do you stop it?** A malicious site makes the logged-in browser send a request. Every form carries a secret per-session token which `CsrfFilter` checks on POST.

## JSP / JSTL / tags / i18n
**Q. JSP elements?** Directive `<%@ %>`, declaration `<%! %>`, scriptlet `<% %>`, expression `<%= %>`, comment `<%-- --%>`, actions `jsp:include/forward/param/useBean`. Demo: `/demo/jsp-elements.jsp`. Real pages use EL + JSTL instead of scriptlets.

**Q. `<%@ include %>` vs `<jsp:include>`?** Static: file is merged at translation time (header/footer). Dynamic: separate JSP executed at request time (navbar, sidebar with parameters).

**Q. Why `<c:out>`?** Escapes HTML to stop XSS. Plain `${}` does not escape.

**Q. Tag file vs tag handler?** Tag file (`.tag`) is JSP syntax for reusable fragments (`productCard`). A tag handler is a Java class extending `SimpleTagSupport` described in a TLD (`<sp:price>`).

**Q. How does i18n work?** `messages_en/hi.properties` are `ResourceBundle`s; `fmt:setLocale` + `fmt:message` choose the text; the language link calls `LanguageServlet`, which stores the choice in session and cookie.

**Q. Why are JSTL SQL tags only a demo?** They put SQL in the view and break the layered design. The real code uses DAO + JDBC.

**Q. What is JNDI used for?** A name-based lookup of container resources (`java:comp/env`): an env-entry and a pooled `DataSource` declared in `context.xml` - the app does not hard-code the connection.

## Swing
**Q. MVC in the Swing app?** View = frames (`view/`), controller = listeners and logic (`controller/`), model = `GenericTableModel` and entity classes.

**Q. Why `SwingUtilities.invokeLater`?** Swing is single-threaded; UI must be created on the Event Dispatch Thread.

**Q. Layout managers used?** `BorderLayout`, `GridLayout`, `FlowLayout`, `GridBagLayout`.

## Network, RMI, Applet
**Q. How does the socket server handle many clients?** `ServerSocket.accept()` loop; each client is handled by a `ClientHandler` task in a fixed thread pool.

**Q. Why create `ObjectOutputStream` before `ObjectInputStream`?** Both sides write a stream header first; creating the input stream first on both sides blocks forever (deadlock).

**Q. Why `Serializable` and `ObjectInputFilter`?** Objects must be serializable to travel over sockets/RMI. The filter allow-lists classes so untrusted data cannot instantiate dangerous classes.

**Q. RMI steps?** Remote interface (extends `Remote`, methods throw `RemoteException`) -> implementation (extends `UnicastRemoteObject`) -> server creates registry and `rebind` -> client `lookup` and calls the stub. Arguments/results are serialized; Java 5+ generates stubs dynamically (no `rmic`).

**Q. Sockets vs RMI?** Sockets: you design the protocol and parse messages. RMI: remote method calls look like local calls; Java handles marshalling.

**Q. Why is the Applet outside the build?** Applets are obsolete (no browser support, API removed in newer JDKs). The file shows the lifecycle (`init, start, stop, destroy, paint`) for the syllabus only.

**Q. What is a protocol/content handler?** `URLStreamHandler` defines how to open a URL scheme (`shopsphere://`); `ContentHandler` converts the stream to an object (`url.getContent()`). See `UrlDemo`.

## Design decisions worth mentioning
* Soft delete of products keeps order history valid.
* Order items store the price at purchase time, so later price changes do not alter old orders.
* Totals are never trusted from the browser.
* Mock payment: card/UPI details are validated and discarded, never stored.
