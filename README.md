# ShopSphere - Advanced Java E-Commerce Platform

A complete mini e-commerce system built for the **Advanced Java (RTU)** syllabus. One shared MySQL database and one shared service layer are used by:

| Module | What it is | Syllabus topics |
|---|---|---|
| `common` | Models, JDBC DAOs, service layer (business rules), `DBConnection`, password hashing | JDBC, PreparedStatement, transactions, N-tier design |
| `web` | Customer storefront + admin area (Servlets, JSP, JSTL, filters, listeners, tag files, tag handlers, i18n, cookies, sessions, JNDI) | Servlet, JSP, JSTL, MVC, Filters, Listeners |
| `swing-admin` | Desktop administration app (MVC) | AWT/Swing, layout managers, JTable, events, look and feel |
| `network` | Multi-threaded TCP inventory server + client, serialization, URL / protocol / content handler demo | java.net, Sockets, URL, URLConnection, Serialization |
| `rmi` | Remote Inventory Service (registry, stub, remote interface) | RMI |
| `docs/applet-demo` | Applet life-cycle example (obsolete technology, kept out of the build) | Applets |

Demo logins (created by `database/shopsphere.sql`):

| Role | Email | Password |
|---|---|---|
| Admin | `admin@shopsphere.com` | `Admin@123` |
| Customer | `demo@shopsphere.com` | `Demo@123` |

Coupons in the seed data: `WELCOME10` (10 %, min Rs. 500, max Rs. 500 off), `FLAT200` (Rs. 200 off orders above Rs. 1500), `EXPIRED5` (expired, for testing).

---

## 1. Prerequisites

* JDK 21 (or newer LTS)
* Apache Maven 3.9+
* MySQL 8+ (running on `localhost:3306`)
* Apache Tomcat **11** (Jakarta EE 11: Servlet 6.1 / JSP 4.0). Tomcat 10.1 also works.
* Internet for the first Maven build and for the Bootstrap CSS/JS loaded from a CDN in the browser.

## 2. Quick start

```bash
# 1. create database, tables and demo data
mysql -u root -p < database/shopsphere.sql          # or scripts/setup-database.sh | .bat

# 2. set your MySQL password (default in the project is root/root)
#    common/src/main/resources/db.properties           <- Swing, RMI, socket server, web app
#    web/src/main/webapp/META-INF/context.xml          <- only for the JNDI demo

# 3. build everything
mvn clean package

# 4. deploy the web app
cp web/target/shopsphere.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh                         # startup.bat on Windows
# open  http://localhost:8080/shopsphere/
```

> For the **JNDI demo** (`/demo/jndi`, `/demo/jstl-sql.jsp`) copy `mysql-connector-j-9.x.jar` into `$CATALINA_HOME/lib` once and restart Tomcat. The rest of the application does not need this.

### Desktop, socket and RMI programs

`mvn package` creates runnable jars in `network/target`, `rmi/target` and `swing-admin/target`. Convenience scripts (`.sh` and `.bat`) are in `scripts/`:

| Script | Starts |
|---|---|
| `run-swing-admin` | Swing admin application (login with the admin account) |
| `run-socket-server` / `run-socket-client` | TCP inventory server on port 5050 / interactive client |
| `run-rmi-server` / `run-rmi-client` | RMI registry + `InventoryService` on port 1099 / client |
| `run-url-demo` | URL, `HttpURLConnection`, custom protocol & content handler demo |

Database settings can be overridden without editing files: `JAVA_OPTS="-Ddb.url=... -Ddb.user=... -Ddb.password=..."` or environment variables `DB_URL`, `DB_USER`, `DB_PASSWORD`.

Socket client commands: `GET_PRODUCT 1`, `GET_STOCK 1`, `SEARCH_PRODUCT java`, `PING`, `QUIT`.

## 3. Project structure

```
ShopSphere/
├── pom.xml                      parent (modules: common, network, rmi, swing-admin, web)
├── database/shopsphere.sql      schema + seed data
├── common/                      model/ dao/ service/ util/  (+ db.properties)
├── web/
│   ├── src/main/java/com/shopsphere/{controller,filter,listener,tags,util}
│   ├── src/main/resources/messages*.properties           (i18n: English, Hindi)
│   └── src/main/webapp/
│       ├── *.jsp  admin/*.jsp  demo/*.jsp  fragments/  css/  js/  images/
│       └── WEB-INF/{web.xml, shopsphere.tld, tags/*.tag}  META-INF/context.xml
├── swing-admin/                 view/ controller/ model/  (MVC)
├── network/                     server/ client/ model/ url/
├── rmi/                         remote/ server/ client/
├── scripts/                     setup + run scripts
├── tests/                       ServiceIT.java, e2e_http_test.py
└── docs/                        architecture, syllabus mapping, test cases, viva guide, report outline, screenshots
```

Note: the guide suggested separate `model`/`dao` folders in every module. They are in one shared `common` module instead, so the web app, Swing app, socket server and RMI server all use exactly the same classes and rules (no duplicated code).

## 4. Features

**Customer:** register / login / logout, browse, search, filter (category, price), sort, product details with reviews and related products, cart (stock-checked), wishlist, checkout with address, coupon and mock payment (COD / UPI / card), order history, order tracking timeline, cancel order, profile + password change + address book, English / Hindi UI, recently viewed products (cookie).

**Admin (web and Swing):** dashboard (orders, revenue, top products, low stock), product / category CRUD, inventory with low-stock highlighting, order status workflow, customer block / unblock, coupons (web).

**Business rules (enforced on the server):** prices and totals are recalculated from database prices; stock can never go negative (guarded `UPDATE ... WHERE stock >= ?`); checkout is one JDBC transaction (order + items + stock + payment + coupon usage + cart clearing, rolled back on any failure); coupons are validated for expiry, minimum order, one use per customer; cancelling returns stock; delivered / cancelled orders are final.

**Security:** PBKDF2 salted password hashes, `PreparedStatement` everywhere, output escaping with `<c:out>`, CSRF token on every POST, session fixation protection (`changeSessionId`), HttpOnly session cookie, role-based filters (`/admin/*`), open-redirect protection, login attempt limit, restricted deserialization (`ObjectInputFilter`) on the socket client, soft delete of products to keep order history.

## 5. What was verified

Everything below was executed on the final code in a Linux test environment:

* `tests/ServiceIT.java`: **48 / 48** checks pass against a real database (registration, login, search, cart, coupons, transactional checkout, stock, cancellation, reviews, admin).
* `tests/e2e_http_test.py`: **70 / 70** HTTP checks pass against the deployed WAR on **Tomcat 10.1.55 + Jakarta JSTL 3.0.2 + MariaDB 10.11** (all customer pages, all admin pages, all demo pages, filters, CSRF, 403/404 pages, Hindi switch, JNDI lookups).
* Socket server + client, RMI server + client and the URL / protocol-handler demo were run end-to-end.
* The Swing application was started on a virtual display, logged in with the real admin account, and the dashboard, product and order windows were opened (screenshots in `docs/screenshots`).

Not verified in that environment (be aware):

* The **Maven build itself** (`mvn clean package`): Maven Central was not reachable from the sandbox, so sources were compiled with `javac` against the same library versions instead. The POMs are well-formed and use published versions; if Maven reports a version problem, adjust the version numbers in `pom.xml` / `web/pom.xml`.
* **Tomcat 11 and MySQL 8 specifically** (Tomcat 10.1 and MariaDB were used; the code only uses APIs common to both and the same `jakarta.*` namespace).
* The pages in a real **browser** (Bootstrap is loaded from a CDN, so the styling could not be viewed here).

## 6. Troubleshooting

| Problem | Fix |
|---|---|
| `Communications link failure` / `Access denied` | MySQL not running, or wrong user/password in `db.properties` |
| Web app shows "Could not load products" | Database not created, or credentials wrong; check Tomcat log (`logs/catalina.*.log`) for `Database is NOT reachable` |
| `No suitable driver` in a standalone program | Run the shaded jar from `target/` (it contains the MySQL driver) |
| `/demo/jndi` shows FAIL | Copy the MySQL driver jar into Tomcat `lib` and fix `META-INF/context.xml` |
| Page shows raw `${...}` | You deployed to an old Tomcat (needs 10.1+ because of `jakarta.*`) |
| Port 8080 / 5050 / 1099 in use | Stop the other program or change the port |

More documents: `docs/ARCHITECTURE.md`, `docs/SYLLABUS_MAPPING.md`, `docs/TEST_CASES.md`, `docs/VIVA_GUIDE.md`, `docs/REPORT_OUTLINE.md`.
