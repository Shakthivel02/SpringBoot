# Store Module Analysis & Spring Boot Migration Guide

This document breaks down the existing Node.js/Mongoose "Store Module" to help you rebuild it as a new, standalone project using **Java Spring Boot, Spring Data JPA, and Spring WebFlux**.

maintain all the best practices and code structure
---

## 1. Domain Entities & Database Models
The current system uses MongoDB (Mongoose). To migrate this to a relational database postgresql (via JPA) or Spring Data MongoDB Reactive, we must establish the core entities. 

### A. Product (`Product.js`)
Represents an item available for purchase in the store.
*   **Key Fields**: `readableId`, `productTitle`, `description`, `brandName`, `imageUrl`, `images` (array), `unitCount`, `totalStock`, `status` (`active`/`inactive`), `instituteId`.
*   **Pricing Embedded Object**: Includes `mrp`, `salePrice`, `saleStartDate`, `saleEndDate`, `minimumPrice`, `maximumPrice`.
*   **Relationships**: 
    *   Many-to-Many to `Category` (`productCategories`).
    *   Many-to-One to `User` (`createdBy`).
    *   Many-to-One to `Institute`.
*   **JPA Note**: Arrays like `genericKeywords`, `bulletPoints`, and `itemsIncluded` can be modeled using `@ElementCollection` or separate child tables. Pricing can be an `@Embedded` class.

### B. Store Order (`StoreOrder.js`)
Tracks the lifecycle of a student's purchase request.
*   **Key Fields**: `costCoins`, `quantity` (legacy), `status` (Enum: `pending_parent`, `pending_admin`, `approved`, `rejected`, `delivered`), `deliveryAddress` (embedded object).
*   **Approval Tracking**: `parentApprovedBy`, `parentApprovedAt`, `adminApprovedBy`, `adminApprovedAt`, `parentRejectionReason`, etc.
*   **Items List**: An array of items purchased, storing a snapshot (`productId`, `productTitle`, `quantity`, `costCoins`).
*   **Relationships**: Many-to-One to `User` (Student), `Institute`.
*   **JPA Note**: Use `@OneToMany(cascade = CascadeType.ALL)` for tracking the `OrderItems`. Use `@Embedded` for the `DeliveryAddress`. Use an `Enum` for the order status.

### C. Ledger Transaction (`Transaction.js`)
Acts as the financial ledger/audit log for the in-game economy (Coins, Stars, XP).
*   **Key Fields**: `transactionType` (e.g., `store_purchase`, `refund`, `reward_earned`), `status` (`pending`, `completed`, `refunded`), `coinsChange`, `previousCoins`, `newCoins`.
*   **Relationships**: `studentId`, `batchId`, `relatedEntityId` (+ `relatedEntityType` polymorphic link to the order).
*   **JPA Note**: You will use Entity listeners (`@PrePersist`, `@PreUpdate`) to calculate `newCoins = previousCoins + coinsChange` just like the Mongoose `pre("save")` hook.

### D. Cart & Wishlist (`Cart.js`, `Wishlist.js`)
Simple collections linking a `StudentStats` profile to a list of `Product` IDs.
*   **Key Fields**: `studentStatsId` (unique mapping), `items` list containing `productId` and `addedAt` (and `quantity` for Cart).

---

## 2. Core Business Logic & Workflows (`store.js`)

The `store.js` file handles administration and the checkout lifecycle. If migrating to Spring Boot, these would become your `@Service` layer classes.

### A. Product Management (Admin/Teacher)
*   **Creation & Image Upload**: Validates image formats and uploads them to S3. Rejects negative prices or scenarios where `salePrice > mrp`. Ensures `saleEndDate` is strictly after `saleStartDate`.
*   **Soft Deletion**: Instead of permanently deleting a `Product`, it sets `status = inactive`, deletes S3 images to save space, and clears the image arrays. It prevents deletion if there are any orders in `pending_parent` or `pending_admin` status.

### B. Store Front (Student View)
*   **Endpoint**: `getAvailableStoreItems`
*   **Logic**: 
    1. Fetches all active products with `totalStock > 0`.
    2. Retrieves the student's current coin balance (`totalCoinsEarned`) from `StudentStats`.
    3. Calculates the `activePrice` (accounting for active sales using `saleStartDate` and `saleEndDate`).
    4. **Smart Filtering**: Automatically hides products that cost more coins than the student currently owns. (1 MRP = 1 Coin).

### C. The Checkout & Approval Workflow
This is the most critical logic sequence to port over correctly to Java:

1.  **Request Purchase (`requestStorePurchase`)**:
    *   A Student or Parent initiates a purchase.
    *   **Validation**: Checks if product exists, is active, and `totalStock > 0`. Checks if student has enough `totalCoinsEarned`.
    *   **Coins Deduction**: *Coins are deducted immediately upfront* from `StudentStats`. 
    *   **Order Creation**: An order is created with status `pending_parent` (if requested by student) or `pending_admin` (if parent requested).
    *   **Ledger**: A `Transaction` is logged with type `store_purchase`, status `pending` and `coinsChange` (positive number representing spending).
2.  **Parent Approval (`updateParentApproval`)**:
    *   Action: `approve`. Checks stock again. Updates status to `pending_admin`.
    *   Action: `reject`. Order status becomes `rejected_by_parent`. **Coins are refunded** to the student's `StudentStats`, and the ledger `Transaction` is marked as `refunded`.
3.  **Admin Approval**:
    *   Action: `approve`. Final stock check *and stock decrement* (`totalStock -= quantity`). Status -> `approved` or `delivered`.
    *   Action: `reject`. Status -> `rejected`. **Coins are refunded** and transaction marked `refunded`.

---

## 3. Spring Boot WebFlux + JPA Migration Strategy

When building this in Spring Boot with WebFlux and JPA, you must navigate a specific architectural challenge: **WebFlux is reactive (non-blocking), while standard JPA (Hibernate) is blocking.**

### Tech Stack Recommendation
1.  **Option A (Pure Reactive)**: `Spring WebFlux` + `Spring Data R2DBC` (for Postgres/MySQL) or `Spring Data MongoDB Reactive`. This remains fully non-blocking end-to-end.
2.  **Option B (Hybrid)**: `Spring WebFlux` + `Spring Data JPA` (Hibernate). You must wrap all repository calls in `Mono.fromCallable(() -> repository.save(entity)).subscribeOn(Schedulers.boundedElastic())` to prevent blocking the Netty event loop threads.

### Suggested Java Project Structure
```text

src/main/java/com/app/store
├── config/
│   └── S3Config.java            # AWS SDK V2 Reactive configuration
├── controller/
│   ├── ProductController.java   # Admin catalog management routes
│   └── OrderController.java     # Checkout and approval workflows
├── model/
│   ├── entity/                  # JPA @Entity classes (Product, StoreOrder, Transaction)
│   └── enums/                   # Enums (OrderStatus, TransactionType)
├── repository/
│   ├── ProductRepository.java   # Extends R2dbcRepository or JpaRepository
│   └── StoreOrderRepository.java
├── service/
│   ├── ProductService.java      # Pricing logic, S3 uploads
│   └── CheckoutService.java     # The transactional purchase/approval workflow
└── dto/
    ├── request/                 # PurchaseRequest, ProductCreateRequest
    └── response/                # ProductResponse (calculates activePrice)
```

### Handling Transactions in WebFlux
In Node.js Mongoose, `session.startTransaction()` is used to ensure the order creation, transaction log, and coin deduction succeed or fail together.
In Spring WebFlux, if using R2DBC or MongoDB Reactive, use the `@Transactional` annotation on your service method:

```java
@Service
public class CheckoutService {
    
    @Transactional
    public Mono<StoreOrderResponse> requestStorePurchase(PurchaseRequest request, String userId) {
        // 1. Fetch Product and User Stats (Reactive chain)
        return productRepo.findById(request.getProductId())
            .filter(product -> product.getTotalStock() > 0)
            .switchIfEmpty(Mono.error(new OutOfStockException()))
            .zipWith(studentStatsRepo.findByStudentId(userId))
            .flatMap(tuple -> {
                Product product = tuple.getT1();
                StudentStats stats = tuple.getT2();
                
                // 2. Validate Coins
                int cost = calculateActivePrice(product);
                if (stats.getTotalCoinsEarned() < cost) return Mono.error(new InsufficientCoinsException());
                
                // 3. Deduct Coins, Save Order, Save Transaction log
                stats.setTotalCoinsEarned(stats.getTotalCoinsEarned() - cost);
                StoreOrder newOrder = createPendingOrder(product, userId);
                Transaction ledger = createPendingTransaction(product, cost, userId);
                
                // Save all 3 reactively
                return studentStatsRepo.save(stats)
                    .then(storeOrderRepo.save(newOrder))
                    .then(transactionRepo.save(ledger))
                    .map(savedTransaction -> convertToResponse(newOrder));
            });
    }
}
```

### Key Considerations for Your Rewrite:
1.  **Id Generation**: Mongoose uses `ObjectId`. In JPA, you'll likely use `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` for auto-incrementing integers, or `GenerationType.UUID`.
2.  **Polymorphism**: The `Transaction` schema uses `relatedEntityId` and `relatedEntityType`. In JPA, this pattern is generally discouraged in favor of explicit Foreign Keys or JPA Table Inheritance. Consider creating a base `Transaction` class and subclasses (`StoreTransaction`, `RewardTransaction`), or keep it as a weak string reference.
3.  **Validation**: Replace Mongoose schema validations with Java Bean Validation annotations on your DTOs (`@NotNull`, `@Min`, `@Max`, `@FutureOrPresent`).
