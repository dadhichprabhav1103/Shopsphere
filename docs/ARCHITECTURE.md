# ShopSphere - Architecture and Design

(Diagrams are written in Mermaid. They render on GitHub, in VS Code (Markdown Preview Mermaid) and at https://mermaid.live.)

## 1. N-tier architecture

```mermaid
flowchart TB
    subgraph Clients
        B[Web browser<br/>customer / admin]
        S[Swing admin app]
        SC[Socket client]
        RC[RMI client]
    end
    subgraph Web["web module (Tomcat)"]
        F[Filters: Encoding, Locale, CSRF,<br/>Authentication, Admin]
        C[Servlets = Controllers]
        J[JSP + JSTL + tag files = Views]
    end
    subgraph Common["common module"]
        SV[Service layer<br/>validation + business rules]
        D[DAO layer<br/>JDBC PreparedStatement]
        M[Model classes]
    end
    SS[Socket server<br/>network module]
    RS[RMI server<br/>rmi module]
    DB[(MySQL shopsphere)]

    B --> F --> C --> SV
    C -->|forward| J
    S -->|Swing controllers| SV
    SC <-->|TCP + serialization| SS --> D
    RC <-->|RMI| RS --> D
    SV --> D --> DB
    D -.uses.- M
```

Rules: a JSP never touches the database; a servlet never contains SQL; a DAO contains no business rules.

## 2. MVC in the web module

```mermaid
sequenceDiagram
    participant Br as Browser
    participant Fl as Filters
    participant Sv as Servlet (Controller)
    participant Se as Service
    participant Da as DAO
    participant Jp as JSP (View)
    Br->>Fl: POST /cart (csrf, productId, qty)
    Fl->>Fl: Encoding, Locale, CSRF check, Authentication
    Fl->>Sv: doPost()
    Sv->>Se: CartService.add()
    Se->>Da: ProductDAO.findById(), CartDAO.addOrIncrease()
    Da-->>Se: data
    Se-->>Sv: ok / ServiceException
    Sv->>Br: redirect (flash message in session)
    Br->>Sv: GET /cart
    Sv->>Jp: forward cart.jsp (request attribute "cart")
    Jp-->>Br: HTML
```

## 3. Entity-relationship diagram

```mermaid
erDiagram
    USERS ||--o{ ADDRESSES : has
    USERS ||--o{ CART : owns
    USERS ||--o{ WISHLIST : saves
    USERS ||--o{ ORDERS : places
    USERS ||--o{ REVIEWS : writes
    CATEGORIES ||--o{ PRODUCTS : contains
    PRODUCTS ||--o{ CART : in
    PRODUCTS ||--o{ WISHLIST : in
    PRODUCTS ||--o{ ORDER_ITEMS : sold_as
    PRODUCTS ||--o{ REVIEWS : receives
    ORDERS ||--|{ ORDER_ITEMS : contains
    ORDERS ||--o{ PAYMENTS : paid_by
    ORDERS }o--|| ADDRESSES : ships_to
    COUPONS ||--o{ COUPON_USAGE : used_in
    ORDERS ||--o| COUPON_USAGE : applies
    USERS ||--o{ COUPON_USAGE : redeems
    USERS { int user_id PK
            string email UK
            string password "PBKDF2 hash"
            string role "CUSTOMER or ADMIN" }
    PRODUCTS { int product_id PK
               decimal price
               decimal discount
               int stock
               bool status }
    ORDERS { int order_id PK
             decimal total_amount
             string order_status
             string payment_status }
```

## 4. Use cases

```mermaid
flowchart LR
    V([Visitor]) --- UC1[Browse / search / filter products]
    V --- UC2[Register]
    V --- UC3[Login]
    Cu([Customer]) --- UC4[Manage cart and wishlist]
    Cu --- UC5[Checkout: address, coupon, payment]
    Cu --- UC6[Track / cancel orders]
    Cu --- UC7[Review purchased products]
    Cu --- UC8[Manage profile and addresses]
    A([Administrator]) --- UC9[Manage products and categories]
    A --- UC10[Manage inventory]
    A --- UC11[Process orders]
    A --- UC12[Block / unblock customers]
    A --- UC13[Manage coupons]
    A --- UC14[View dashboard / sales]
```

## 5. Checkout transaction

```mermaid
sequenceDiagram
    participant Co as CheckoutService
    participant DB as MySQL (one Connection, autoCommit=false)
    Co->>DB: read cart + current product prices
    Co->>Co: validate stock, coupon; calculate subtotal, discount, total
    Co->>DB: INSERT orders
    loop each cart line
        Co->>DB: UPDATE products SET stock = stock - ? WHERE stock >= ?
        Co->>DB: INSERT order_items (price at purchase time)
    end
    Co->>DB: INSERT payments, INSERT coupon_usage, DELETE cart
    alt every step succeeded
        Co->>DB: COMMIT
    else any failure
        Co->>DB: ROLLBACK (no half-created orders, stock restored)
    end
```

## 6. Key classes

```mermaid
classDiagram
    class BaseServlet { +currentUser() +render() +redirect() +flash() +safeNext() }
    BaseServlet <|-- ProductServlet
    BaseServlet <|-- CartServlet
    BaseServlet <|-- CheckoutServlet
    BaseServlet <|-- AdminProductServlet
    class CheckoutService { +placeOrder() +validateCoupon() }
    class OrderService { +changeStatus() +cancelByCustomer() }
    class ProductDAO { +search() +decrementStock() }
    class Product { <<Serializable>> +getFinalPrice() }
    CheckoutServlet --> CheckoutService
    CheckoutService --> ProductDAO
    CheckoutService --> OrderDAO
    ProductDAO --> Product
    class InventoryService { <<interface Remote>> +getProduct() +getStock() +updateStock() }
    class InventoryServiceImpl
    InventoryService <|.. InventoryServiceImpl
    InventoryServiceImpl --> ProductDAO
```

## 7. Security design summary

| Threat | Defence |
|---|---|
| SQL injection | `PreparedStatement` only; sort order chosen from a whitelist |
| XSS | all user data printed with `<c:out>`; `HttpOnly` cookies |
| CSRF | per-session token checked by `CsrfFilter` on every POST |
| Session fixation | `changeSessionId()` after login |
| Password theft | PBKDF2-HMAC-SHA256, random salt, 65 536 iterations, constant-time compare |
| Broken access control | `AuthenticationFilter`, `AdminFilter`, ownership checks in `OrderService` / `AddressDAO` |
| Price tampering | totals always recalculated on the server |
| Overselling / double-spend | guarded stock update inside a transaction |
| Open redirect | `safeNext()` allows only same-site paths |
| Unsafe deserialization | `ObjectInputFilter` allow-list in the socket client |
