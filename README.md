# Library Loan Management System

## End-to-End JDBC Application with Transaction Management & Performance Evaluation (Apache Derby)

---

## Student Details

| Field        | Details                      |
|--------------|------------------------------|
| Name         | Anuska Basantia              |
| Regd. No.    | 2341002058                   |
| Section      | 2341-2C3                     |
| Sl. No.      | 03                           |
| Package Name | ANUSKA_2341002058_03         |

---

## Project Description

A console-driven Library Loan Management System built using Java JDBC and Apache Derby (embedded mode).
The system enforces ACID transactions, demonstrates savepoint-based partial rollback, validates
constraint violations, and includes a full performance benchmarking framework comparing multiple
JDBC access strategies.

---

## Technologies Used

| Technology   | Details                        |
|--------------|--------------------------------|
| Language     | Java 11+                       |
| JDBC Driver  | Apache Derby (embedded)        |
| Database     | Apache Derby — libraryDB       |
| IDE          | Eclipse IDE                    |
| Build        | Manual classpath / Eclipse     |

---

## Project Structure

```
src/
└── ANUSKA_2341002058_03/
    ├── connection/
    │   ├── ConnectionManager.java   — DB URL, getConnection(), shutdown()
    │   ├── DatabaseSetup.java       — Table creation, index creation
    │   └── SeedData.java            — Baseline seed data insertion
    ├── service/
    │   └── TransactionService.java  — All business operations & transactions
    ├── performance/
    │   └── PerformanceEvaluator.java — Benchmark suites & report generator
    └── ui/
        └── MainApp.java             — CLI menu, workflow orchestration
```

---

## Database Schema

### Members
| Column      | Type         | Constraint              |
|-------------|--------------|-------------------------|
| MemberID    | INT          | PK, GENERATED IDENTITY  |
| Name        | VARCHAR(100) | NOT NULL                |
| ActiveLoans | INT          | DEFAULT 0               |

### Books
| Column    | Type         | Constraint              |
|-----------|--------------|-------------------------|
| BookID    | INT          | PK, GENERATED IDENTITY  |
| Title     | VARCHAR(100) | NOT NULL                |
| ISBN      | VARCHAR(30)  | UNIQUE, NOT NULL        |
| Available | BOOLEAN      | DEFAULT true            |

### Loans
| Column     | Type | Constraint                    |
|------------|------|-------------------------------|
| LoanID     | INT  | PK, GENERATED IDENTITY        |
| BookID     | INT  | FK → Books(BookID)            |
| MemberID   | INT  | FK → Members(MemberID)        |
| LoanDate   | DATE |                               |
| ReturnDate | DATE | NULL until returned           |

### Indexes Created
| Index Name            | Table | Column      | Purpose                    |
|-----------------------|-------|-------------|----------------------------|
| idx_books_isbn        | Books | ISBN        | Fast ISBN lookup           |
| idx_loans_memberid    | Loans | MemberID    | Fast member loan queries   |
| idx_loans_returndate  | Loans | ReturnDate  | Fast overdue book queries  |

---

## Features Implemented

### Phase 1 — Database Initialization & Schema
- Embedded Derby DB with `jdbc:derby:libraryDB;create=true`
- Three normalized tables with PKs, FKs, and constraints
- Three indexes on frequently queried columns
- Seed data with duplicate-insert guard

### Phase 2 — Transaction Management
- Auto-commit disabled for all data-modifying operations
- `processLoan()` — 5-step transaction:
  1. Verify book exists and is available
  2. Verify member exists
  3. Update book availability → `Available = false`
  4. **Savepoint** set after book update (`AfterBookUpdate`)
  5. Insert loan record + update member active loan count
- On loan insert failure → **partial rollback to savepoint** (book status reverted, loan not inserted)
- On any other failure → **full rollback**
- `processReturn()` — updates ReturnDate, restores book availability, decrements member count
- `demonstrateConstraintViolation()` — intentionally triggers UNIQUE constraint on ISBN, catches SQLException, rolls back, verifies data integrity post-rollback

### Phase 3 — Core Application Workflow
- Full menu-driven CLI with 9 options
- All DB interactions use `PreparedStatement`
- `try-with-resources` used throughout for Connection, Statement, ResultSet
- Graceful shutdown with Derby SQLState check (`08006` / `XJ015`)

### Phase 4 — Performance Evaluation
Four benchmark suites, each run 5 times with mean ± stddev and throughput (ops/sec):

| Suite | Test A | Test B |
|-------|--------|--------|
| Insert Strategy | Individual `executeUpdate()` — 1,000 & 10,000 records | `addBatch()` + `executeBatch()` — 1,000 & 10,000 records |
| Query Strategy | Full-table scan on Loans | Indexed lookup on `Loans.MemberID` |
| Statement Type | Raw `Statement` (string concat, 100 queries) | `PreparedStatement` (100 queries) |
| Transaction Granularity | Per-operation commit (100 ops) | Batched commit (100 ops, 1 commit) |

- Warm-up phase: 100ms sleep + 50 dummy queries + `System.gc()` before timing
- Structured report table printed to console with observations

---

## How to Run

### Prerequisites
- Java 11 or higher
- `derby.jar` (Apache Derby embedded driver)

### Steps in Eclipse
1. Open Eclipse → `File` → `Import` → `Existing Projects into Workspace`
2. Select the `LibararyLoanSystem` folder
3. Right-click project → `Build Path` → `Add External JARs` → select `derby.jar`
4. Run `ANUSKA_2341002058_03.ui.MainApp` as Java Application

### Steps via Command Line
```bash
# Compile
javac -cp .:derby.jar -d bin src/ANUSKA_2341002058_03/**/*.java src/module-info.java

# Run
java -cp .:derby.jar:bin ANUSKA_2341002058_03.ui.MainApp
```
> On Windows replace `:` with `;` in the classpath.

---

## Sample CLI Session

```
========================================
   LIBRARY LOAN MANAGEMENT SYSTEM
========================================
 1. Register Member
 2. Add Book
 3. Process Loan
 4. Process Return
 5. View Active Loans by Member
 6. View Overdue Books
 7. Demonstrate Constraint Violation
 8. Run Performance Benchmark
 9. Exit
========================================
Enter choice: 3
Enter Book ID: 1
Enter Member ID: 1
Loan processed successfully.

Enter choice: 5
Enter Member ID: 1

--- Active Loans for Member ID 1 ---
LoanID     Title                          ISBN            LoanDate
----------------------------------------------------------------------
1          Java Programming               ISBN-1001       2025-05-15

Enter choice: 7

--- Demonstrating Constraint Violation & Rollback ---
Constraint violation caught: The statement was aborted because it would have caused a duplicate key value...
Transaction rolled back. Data integrity preserved.

Verifying data consistency post-rollback:
Books with ISBN-1001: 1 (should be 1 — no duplicate inserted)

Enter choice: 8
Running warm-up phase...
Warm-up complete.

========================================
         PERFORMANCE REPORT
========================================
Operation                                          Avg(ms)    StdDev    Throughput
----------------------------------------------------------------------------------------
Individual INSERT (1000 records)                    320.45     12.30    3121.4 ops/s
Batch INSERT      (1000 records)                     48.12      3.10   20780.5 ops/s
Individual INSERT (10000 records)                  3180.22     45.60    3144.2 ops/s
Batch INSERT      (10000 records)                   410.88     18.40   24338.1 ops/s
Full-Table Scan   (Loans)                             2.10      0.30          N/A
Indexed Lookup    (Loans.MemberID)                    0.45      0.08          N/A
Statement         (100 queries, string concat)       18.30      1.20     5464.5 ops/s
PreparedStatement (100 queries)                       9.80      0.70    10204.1 ops/s
Per-Op Commit     (100 ops)                          95.40      5.60    1047.7 ops/s
Batched Commit    (100 ops, 1 commit)                 8.20      0.90   12195.1 ops/s
----------------------------------------------------------------------------------------

Observations:
  - Batch INSERT is significantly faster than individual INSERT
    due to reduced round-trips and single transaction overhead.
  - Indexed lookup outperforms full-table scan as data grows.
  - PreparedStatement is faster than Statement for repeated
    queries because Derby compiles the plan only once.
  - Batched commit reduces I/O flush overhead vs per-op commit.
```

---

## Performance Analysis

### Transaction Boundaries & Data Integrity
Disabling auto-commit and wrapping multi-step operations in explicit transactions ensures
that either all steps succeed (commit) or none take effect (rollback). The savepoint in
`processLoan()` demonstrates partial rollback — if the loan record insert fails, the book
availability update is also undone, leaving the database in a consistent state.

### Why Batch Inserts Outperform Individual Inserts
Each individual `executeUpdate()` call involves a full round-trip to Derby's storage engine
and a separate transaction flush. `executeBatch()` groups all statements into a single
engine call, dramatically reducing overhead — typically 5–8x faster at 10,000 records.

### Why PreparedStatement Outperforms Statement
Derby parses and compiles a query plan on every `Statement.executeQuery()` call.
`PreparedStatement` compiles the plan once and reuses it, making repeated parameterized
queries significantly faster, especially in loops.

### Why Batched Commit Outperforms Per-Operation Commit
Every `commit()` forces Derby to flush the transaction log to disk (WAL — Write-Ahead Log).
Committing 100 times means 100 disk flushes. A single batched commit means one flush,
reducing I/O by ~99x for the same number of records.

### Trade-offs
| Approach | Speed | Safety |
|----------|-------|--------|
| Per-op commit | Slow | High (each op durable immediately) |
| Batched commit | Fast | Medium (all-or-nothing for the batch) |
| PreparedStatement | Fast | High (prevents SQL injection) |
| Raw Statement | Slow | Low (SQL injection risk) |

---

## Dependencies

| File       | Source                                      |
|------------|---------------------------------------------|
| derby.jar  | Apache Derby 10.x — https://db.apache.org/derby/ |

---

## Author

**Anuska**
Regd. No.: 2341002058 | Section: 2341-2C3 | Sl. No.: 03
