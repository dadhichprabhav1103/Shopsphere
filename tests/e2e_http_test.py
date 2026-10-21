"""End-to-end HTTP test of a RUNNING ShopSphere (Tomcat + MySQL with the demo seed data).
Usage:  python3 tests/e2e_http_test.py          (env SHOPSPHERE_URL=http://localhost:8080/shopsphere)
Re-create the database first (database/shopsphere.sql): the test registers users/products and expects seed data."""
import re, urllib.request, urllib.parse, http.cookiejar, sys
import os
BASE=os.environ.get("SHOPSPHERE_URL","http://localhost:8080/shopsphere")
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,*a,**k): return None
def client(follow=True):
    cj=http.cookiejar.CookieJar()
    h=[urllib.request.HTTPCookieProcessor(cj)]
    if not follow: h.append(NoRedirect())
    return urllib.request.build_opener(*h), cj
passed=failed=0
def check(name, ok, extra=""):
    global passed, failed
    print(("PASS  " if ok else "FAIL  ")+name+(("  -> "+str(extra)[:200]) if (not ok and extra) else ""))
    if ok: passed+=1
    else: failed+=1
def get(op,path):
    try:
        r=op.open(BASE+path); return r.status, r.read().decode("utf8","replace"), r.geturl()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf8","replace"), e.geturl()
def post(op,path,data):
    try:
        r=op.open(BASE+path, urllib.parse.urlencode(data).encode()); return r.status, r.read().decode("utf8","replace"), r.geturl()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf8","replace"), e.geturl()
def csrf(html):
    m=re.search(r'name="csrf" value="([^"]+)"',html); return m.group(1) if m else None

op,cj=client()
s,h,_=get(op,"/")
check("home renders with featured products", s==200 and "Featured deals" in h and "Wireless Earbuds Pro" in h, s)
check("rupee price formatted", "₹" in h)
tok=csrf(h)
check("csrf token present in forms", tok is not None)
s,h,_=get(op,"/products?q=java&sort=price_asc"); check("search product", s==200 and "Advanced Java Programming" in h and "Yoga" not in h)
s,h,_=get(op,"/products?category=3"); check("category filter", "Clean Code" in h and "Yoga" not in h)
s,h,_=get(op,"/products?min=500&max=700&sort=price_desc"); check("price filter", s==200 and "Yoga Mat" in h)
s,h,_=get(op,"/product?id=1"); check("product details + reviews", s==200 and "Great sound" in h and "Customer reviews" in h)
check("XSS-safe output of search term", "&lt;script&gt;" in get(op,"/products?q=%3Cscript%3E")[1] or "<script>alert" not in get(op,"/products?q=%3Cscript%3Ealert(1)%3C/script%3E")[1])

# unauthenticated
op2,_=client(follow=False)
try: op2.open(BASE+"/cart"); loc=None
except urllib.error.HTTPError as e: loc=e.headers.get("Location")
check("unauthenticated cart -> redirect to login", loc and "/login" in loc, loc)
s,h,_=get(client()[0],"/admin/dashboard"); check("admin area without login -> admin login page", "Admin login" in h, s)
s,h,_=post(op,"/login",{"email":"demo@shopsphere.com","password":"Demo@123"})
check("POST without csrf rejected (403)", s==403, s)

# register
s,h,_=get(op,"/register"); tok=csrf(h)
email="e2e%d@test.com"%__import__("time").time()
s,h,_=post(op,"/register",{"csrf":tok,"name":"E2E User","email":email,"mobile":"9876543210","password":"Passw0rd1","confirm":"Passw0rd1"})
check("valid registration -> login page with message", s==200 and "Account created" in h, h[:200])
s,h,_=get(op,"/register"); tok=csrf(h)
s,h,_=post(op,"/register",{"csrf":tok,"name":"E2E User","email":email,"mobile":"9876543210","password":"Passw0rd1","confirm":"Passw0rd1"})
check("duplicate email -> error", "already exists" in h)
s,h,_=get(op,"/login"); tok=csrf(h)
s,h,_=post(op,"/login",{"csrf":tok,"email":email,"password":"bad"}); check("invalid login -> error", "Invalid email or password" in h)
s,h,u=post(op,"/login",{"csrf":tok,"email":email,"password":"Passw0rd1"})
check("valid login -> home with user name", s==200 and "E2E User" in h and u.endswith("/home"), u)
tok=csrf(h)

# cart
s,h,_=post(op,"/cart",{"csrf":tok,"action":"add","productId":"1","quantity":"2","back":"/products"}); check("add to cart (flash + badge)", "Added to your cart" in h)
s,h,_=get(op,"/cart"); check("cart page shows item and subtotal", "Wireless Earbuds Pro" in h and "4,798.4" in h.replace(",","," ), re.findall(r'Subtotal.{0,120}',h,re.S)[:1])
s,h,_=post(op,"/cart",{"csrf":tok,"action":"update","productId":"1","quantity":"3"}); check("change quantity recalculates", "7,197.6" in h, re.findall(r'7,\d+\.\d+',h))
s,h,_=post(op,"/cart",{"csrf":tok,"action":"add","productId":"16","quantity":"1"}); check("out-of-stock add rejected", "out of stock" in h)

# wishlist
s,h,_=post(op,"/wishlist",{"csrf":tok,"action":"add","productId":"8","back":"/products"}); check("wishlist add", s==200)
s,h,_=get(op,"/wishlist"); check("wishlist page", "Advanced Java Programming" in h)

# checkout
s,h,_=get(op,"/checkout"); check("checkout renders", s==200 and "Delivery address" in h and "Add a delivery address" in h)
s,h,_=post(op,"/checkout",{"csrf":tok,"action":"addAddress","addressLine":"5 Test Street","city":"Jaipur","state":"Rajasthan","pincode":"302001","addressType":"HOME"})
check("add address in checkout", "5 Test Street" in h)
aid=re.search(r'name="addressId" id="addr(\d+)"',h).group(1)
s,h,_=post(op,"/checkout",{"csrf":tok,"action":"applyCoupon","code":"welcome10"}); check("coupon applied", "WELCOME10" in h and "Discount" in h)
s,h,_=post(op,"/checkout",{"csrf":tok,"action":"applyCoupon","code":"EXPIRED5"}); check("expired coupon rejected", "expired" in h)
s,h,_=post(op,"/checkout",{"csrf":tok,"action":"place","addressId":aid,"paymentMethod":"UPI","upiId":"bad"}); check("bad UPI id rejected", "valid UPI" in h)
s,h,u=post(op,"/checkout",{"csrf":tok,"action":"place","addressId":aid,"paymentMethod":"UPI","upiId":"user@okbank"})
check("place order -> success page", s==200 and "Thank you" in h and "order-success" in u, u)
oid=re.search(r"<strong>#(\d+)</strong>",h).group(1)
s,h,_=get(op,"/orders"); check("order history lists order", "#"+oid in h)
s,h,_=get(op,"/order?id="+oid); check("order details with tracking", "Placed" in h and "Cancel order" in h)
s,h,_=get(op,"/cart"); check("cart empty after order", "Your cart is empty" in h)
s,h,_=post(op,"/review",{"csrf":tok,"productId":"1","rating":"5","reviewText":"Loved <b>it</b>"}); check("review saved", "Your review has been saved" in h and "&lt;b&gt;it&lt;/b&gt;" in h)
s,h,_=get(op,"/profile"); check("profile page", s==200 and "E2E User" in h and "Saved addresses" in h)
s,h,_=post(op,"/profile",{"csrf":tok,"action":"update","name":"E2E Renamed","mobile":"9876543211"}); check("profile update", "Profile updated" in h and "E2E Renamed" in h)

# customer cannot enter admin
s,h,_=get(op,"/admin/dashboard"); check("customer opening /admin -> 403 access denied", s==403 and "Access denied" in h, s)
s,h,_=get(op,"/admin/products"); check("customer /admin/products -> 403", s==403)

# language
s,h,_=get(op,"/lang?to=hi&back=/products"); check("switch to Hindi", "उत्पाद" in h and "कार्ट" in h)
s,h,_=get(op,"/lang?to=en&back=http://evil.com"); check("open-redirect blocked", "evil.com" not in _ and s==200)

# logout
s,h,u=post(op,"/logout",{"csrf":tok}); check("logout -> login page", "logged out" in h)
s,h,_=get(op,"/cart"); check("after logout cart requires login", "Login" in h and "Password" in h)

# admin
ad,_=client()
s,h,_=get(ad,"/admin-login"); t=csrf(h)
s,h,u=post(ad,"/admin-login",{"csrf":t,"email":"demo@shopsphere.com","password":"Demo@123"}); check("customer cannot use admin login", "does not have administrator" in h)
s,h,_=get(ad,"/admin-login"); t=csrf(h)
s,h,u=post(ad,"/admin-login",{"csrf":t,"email":"admin@shopsphere.com","password":"Admin@123"})
check("admin login -> dashboard", s==200 and "Dashboard" in h and "Total orders" in h, u); t=csrf(h)
for page,needle in [("/admin/products","New product"),("/admin/categories","Categories"),("/admin/orders","Orders"),("/admin/users","Customers"),("/admin/coupons","WELCOME10"),("/admin/inventory","Inventory")]:
    s,h,_=get(ad,page); check("admin page "+page, s==200 and needle in h, s)
s,h,_=get(ad,"/admin/products?edit=new"); check("admin new product form", "New product" in h and 'name="price"' in h)
s,h,_=post(ad,"/admin/products",{"csrf":t,"action":"save","productId":"0","name":"E2E Gadget","brand":"X","categoryId":"1","description":"d","price":"999","discount":"10","stock":"7","imageUrl":"","status":"on"})
check("admin creates product", "Product saved" in h and "E2E Gadget" in h)
s,h,_=post(ad,"/admin/products",{"csrf":t,"action":"save","productId":"0","name":"Bad","categoryId":"1","price":"-5","stock":"1"}); check("admin product validation", "Price must be greater" in h)
s,h,_=post(ad,"/admin/categories",{"csrf":t,"action":"save","categoryId":"0","name":"E2E Cat","description":"x","status":"on"}); check("admin creates category", "Category saved" in h and "E2E Cat" in h)
s,h,_=post(ad,"/admin/coupons",{"csrf":t,"action":"add","code":"E2E15","discountType":"PERCENT","discountValue":"15","minimumOrder":"100","maximumDiscount":"0","expiryDate":"2031-01-01"}); check("admin creates coupon", "Coupon created" in h and "E2E15" in h)
s,h,_=post(ad,"/admin/orders",{"csrf":t,"orderId":oid,"status":"SHIPPED"}); check("admin updates order status", "updated" in h and "SHIPPED" in h)
s,h,_=post(ad,"/admin/inventory",{"csrf":t,"productId":"4","stock":"50","view":""}); check("admin updates stock", "Stock updated" in h)
s,h,_=get(ad,"/admin/inventory?view=low"); check("low stock view", "low stock only" in h and "LED Desk Lamp" in h)
s,h,_=get(ad,"/admin/dashboard"); check("dashboard shows sales + top products", "Top selling products" in h and "Wireless Earbuds Pro" in h)

# customer sees new status
c2,_=client(); s,h,_=get(c2,"/login"); t2=csrf(h)
post(c2,"/login",{"csrf":t2,"email":email,"password":"Passw0rd1"})
s,h,_=get(c2,"/order?id="+oid); check("customer sees admin's new status (SHIPPED)", "SHIPPED" in h)

# demos
for page,needle in [("/demo/index.jsp","Syllabus"),("/demo/jsp-elements.jsp","Declaration"),("/demo/jstl-functions.jsp","Grade B"),("/demo/jstl-xml.jsp","Wireless Earbuds Pro"),("/demo/jstl-sql.jsp","Rows returned"),("/demo/i18n.jsp","Currency"),("/demo/session-cookie.jsp","demoVisits"),("/demo/request-info.jsp?name=S","Method"),("/demo/lifecycle","ServletConfig init-param"),("/demo/jndi","DataSource")]:
    s,h,_=get(op,page); check("demo "+page, s==200 and needle in h, (s, h[:300] if s!=200 else ""))
s,h,_=get(op,"/demo/jndi"); check("JNDI lookups succeed", "OK  java:comp/env/storeName" in h.replace("&nbsp;"," ") or "OK" in h and "storeName" in h and "FAIL" not in h, re.findall(r'<li[^>]*>(.*?)</li>',h))
s,h,_=get(op,"/nonexistent"); check("404 custom page", s==404 and "Page not found" in h)
s,h,_=get(op,"/cart.jsp"); check("direct JSP access still protected", "Password" in h)
print("\n%d passed, %d failed"%(passed,failed)); sys.exit(1 if failed else 0)
