# Syllabus mapping - where each topic is in ShopSphere

| Topic | Where to look |
|---|---|
| **JDBC**: driver, `Connection`, `PreparedStatement`, `ResultSet`, transactions | `common/.../util/DBConnection`, `dao/*DAO`, `service/CheckoutService` (commit/rollback), `OrderService.changeStatus` |
| **Servlets**: lifecycle, `ServletConfig`, `ServletContext` | `web/.../LifecycleDemoServlet` + `web.xml` (init-param, context-param), page `/demo/lifecycle` |
| Servlet request/response, `doGet`/`doPost`, forward vs redirect | every servlet in `controller/`; `BaseServlet.render()` (forward) and `redirect()` |
| **Session tracking**: `HttpSession`, cookies | `LoginServlet`, `SessionListener`, `CookieUtil`, `ProductServlet.rememberViewed`, `/demo/session-cookie.jsp` |
| **Filters** | `filter/EncodingFilter, LocaleFilter, CsrfFilter, AuthenticationFilter, AdminFilter` + mappings in `web.xml` |
| **Listeners** | `AppListener` (`ServletContextListener`), `SessionListener` (`HttpSessionListener`) |
| **JSP** elements: directives, declaration, scriptlet, expression, comment, implicit objects | `/demo/jsp-elements.jsp`, `fragments/*.jsp` |
| JSP include (static & dynamic), `jsp:forward`, `jsp:param`, `jsp:useBean` | `<%@ include %>` header/footer, `<jsp:include>` navbar/sidebar, `index.jsp` (`jsp:forward`), `demo/i18n.jsp` |
| **Expression Language** | all JSPs (`${...}`) |
| **JSTL core** (`c:`) | `c:forEach`, `c:if`, `c:choose`, `c:set`, `c:url`, `c:redirect`, `c:out`, `c:catch` in almost every page |
| **JSTL formatting** (`fmt:`) | `fmt:message`, `fmt:formatNumber`, `fmt:formatDate`, `fmt:setLocale`, `fmt:setBundle` |
| **JSTL functions** (`fn:`) | `/demo/jstl-functions.jsp`, `products.jsp` |
| **JSTL XML** (`x:`) | `/demo/jstl-xml.jsp` |
| **JSTL SQL** (`sql:`) | `/demo/jstl-sql.jsp` (with explanation why DAO is preferred) |
| **Custom tags**: tag files | `WEB-INF/tags/productCard.tag`, `rating.tag` |
| **Custom tags**: Java tag handlers + TLD | `tags/FormatPriceTag`, `StatusBadgeTag`, `WEB-INF/shopsphere.tld` (`<sp:price>`, `<sp:statusBadge>`) |
| **JNDI** | `JndiDemoServlet`, `<env-entry>` and `<resource-ref>` in `web.xml`, `META-INF/context.xml` |
| **Internationalization** | `messages*.properties`, `LocaleFilter`, `LanguageServlet`, `util/I18nDemo`, `/demo/i18n.jsp` |
| **MVC pattern / N-tier** | web: servlet-JSP-service-DAO; Swing: `swing-admin/view`, `controller`, `model` |
| **AWT / Swing**: containers, layout managers, components, events, `JTable`, `TableModel`, look and feel | `swing-admin` (`GridBagLayout`, `BorderLayout`, `GridLayout`, `FlowLayout`, `JTable`, `JComboBox`, `JRadioButton`, `JCheckBox`, `JOptionPane`, `JMenuBar`, `UIManager`) |
| **Applets** | `docs/applet-demo/ProductBannerApplet.java` (init/start/stop/destroy/paint) |
| **java.net**: `Socket`, `ServerSocket`, multi-threaded server | `network/server/InventoryServer`, `ClientHandler`, `client/InventoryClient` |
| **Object serialization** | `Serializable` models, `ObjectOutputStream/ObjectInputStream`, `ObjectInputFilter` |
| **URL, URLConnection, protocol & content handlers** | `network/url/UrlDemo` |
| **RMI** | `rmi/remote/InventoryService`, `server/RmiServer`, `InventoryServiceImpl`, `client/RmiClient` |
| Threads / concurrency | executor pool in `InventoryServer`; Swing EDT in `SwingAdminApp` |
| Security practices | `docs/ARCHITECTURE.md` section 7 |
