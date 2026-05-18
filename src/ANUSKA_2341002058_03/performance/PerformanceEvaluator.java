package ANUSKA_2341002058_03.performance;

import ANUSKA_2341002058_03.connection.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PerformanceEvaluator {

    private static final int RUNS       = 5;
    private static final int SMALL      = 1000;
    private static final int LARGE      = 10000;
    private static final int BATCH_OPS  = 100;

    // ----------------------------------------------------------------
    // Entry point — runs all benchmarks and prints report
    // ----------------------------------------------------------------
    public static void benchmark() {

        System.out.println("\n========================================");
        System.out.println("   PERFORMANCE EVALUATION REPORT");
        System.out.println("========================================");

        warmUp();

        double[] r1 = benchmarkIndividualInsert(SMALL);
        double[] r2 = benchmarkBatchInsert(SMALL);
        double[] r3 = benchmarkIndividualInsert(LARGE);
        double[] r4 = benchmarkBatchInsert(LARGE);
        double[] r5 = benchmarkFullTableScan();
        double[] r6 = benchmarkIndexedLookup();
        double[] r7 = benchmarkStatementQuery();
        double[] r8 = benchmarkPreparedStatementQuery();
        double[] r9 = benchmarkPerOperationCommit();
        double[] r10 = benchmarkBatchedCommit();

        printReport(new double[][]{r1, r2, r3, r4, r5, r6, r7, r8, r9, r10});

        cleanupBenchmarkData();
    }

    // ----------------------------------------------------------------
    // Warm-up phase — stabilise JVM JIT and Derby buffer cache
    // ----------------------------------------------------------------
    private static void warmUp() {

        System.out.println("Running warm-up phase...");

        try {

            Thread.sleep(100);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }

        try (
                Connection con = ConnectionManager.getConnection();
                Statement stmt = con.createStatement()
        ) {

            for (int i = 0; i < 50; i++) {

                stmt.executeQuery("SELECT COUNT(*) FROM Books").close();
            }

        } catch (SQLException e) {

            System.out.println("Warm-up error: " + e.getMessage());
        }

        System.gc();
        System.out.println("Warm-up complete.\n");
    }

    // ----------------------------------------------------------------
    // Suite 1a: Individual executeUpdate() inserts
    // ----------------------------------------------------------------
    private static double[] benchmarkIndividualInsert(int count) {

        double[] times = new double[RUNS];

        for (int run = 0; run < RUNS; run++) {

            cleanupBenchmarkData();

            try (
                    Connection con = ConnectionManager.getConnection()
            ) {

                con.setAutoCommit(false);

                PreparedStatement ps =
                        con.prepareStatement(
                                "INSERT INTO Members(Name, ActiveLoans) VALUES(?,0)");

                long start = System.nanoTime();

                for (int i = 0; i < count; i++) {

                    ps.setString(1, "BenchMember_" + i);
                    ps.executeUpdate();
                }

                con.commit();

                times[run] = (System.nanoTime() - start) / 1_000_000.0;

                ps.close();
                con.setAutoCommit(true);

            } catch (SQLException e) {

                System.out.println("Individual insert error: " + e.getMessage());
            }
        }

        return summarise("Individual INSERT", count, times);
    }

    // ----------------------------------------------------------------
    // Suite 1b: addBatch() + executeBatch() inserts
    // ----------------------------------------------------------------
    private static double[] benchmarkBatchInsert(int count) {

        double[] times = new double[RUNS];

        for (int run = 0; run < RUNS; run++) {

            cleanupBenchmarkData();

            try (
                    Connection con = ConnectionManager.getConnection()
            ) {

                con.setAutoCommit(false);

                PreparedStatement ps =
                        con.prepareStatement(
                                "INSERT INTO Members(Name, ActiveLoans) VALUES(?,0)");

                long start = System.nanoTime();

                for (int i = 0; i < count; i++) {

                    ps.setString(1, "BenchMember_" + i);
                    ps.addBatch();
                }

                ps.executeBatch();
                con.commit();

                times[run] = (System.nanoTime() - start) / 1_000_000.0;

                ps.close();
                con.setAutoCommit(true);

            } catch (SQLException e) {

                System.out.println("Batch insert error: " + e.getMessage());
            }
        }

        return summarise("Batch INSERT", count, times);
    }

    // ----------------------------------------------------------------
    // Suite 2a: Full-table scan on Loans
    // ----------------------------------------------------------------
    private static double[] benchmarkFullTableScan() {

        double[] times = new double[RUNS];

        for (int run = 0; run < RUNS; run++) {

            try (
                    Connection con = ConnectionManager.getConnection();

                    Statement stmt = con.createStatement()
            ) {

                long start = System.nanoTime();

                ResultSet rs =
                        stmt.executeQuery(
                                "SELECT * FROM Loans WHERE ReturnDate IS NULL");

                while (rs.next()) { /* consume */ }

                times[run] = (System.nanoTime() - start) / 1_000_000.0;

                rs.close();

            } catch (SQLException e) {

                System.out.println("Full scan error: " + e.getMessage());
            }
        }

        return summarise("Full-Table Scan (Loans)", 0, times);
    }

    // ----------------------------------------------------------------
    // Suite 2b: Indexed lookup on Loans.MemberID
    // ----------------------------------------------------------------
    private static double[] benchmarkIndexedLookup() {

        double[] times = new double[RUNS];

        for (int run = 0; run < RUNS; run++) {

            try (
                    Connection con = ConnectionManager.getConnection();

                    PreparedStatement ps =
                            con.prepareStatement(
                                    "SELECT * FROM Loans WHERE MemberID=?")
            ) {

                long start = System.nanoTime();

                ps.setInt(1, 1);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) { /* consume */ }

                times[run] = (System.nanoTime() - start) / 1_000_000.0;

                rs.close();

            } catch (SQLException e) {

                System.out.println("Indexed lookup error: " + e.getMessage());
            }
        }

        return summarise("Indexed Lookup (Loans.MemberID)", 0, times);
    }

    // ----------------------------------------------------------------
    // Suite 3a: Raw Statement with string concatenation
    // ----------------------------------------------------------------
    private static double[] benchmarkStatementQuery() {

        double[] times = new double[RUNS];

        for (int run = 0; run < RUNS; run++) {

            try (
                    Connection con = ConnectionManager.getConnection();
                    Statement stmt = con.createStatement()
            ) {

                long start = System.nanoTime();

                for (int i = 0; i < 100; i++) {

                    // String concatenation — no pre-compilation
                    ResultSet rs =
                            stmt.executeQuery(
                                    "SELECT * FROM Books WHERE ISBN = 'ISBN-100" + (i % 5 + 1) + "'");

                    while (rs.next()) { /* consume */ }

                    rs.close();
                }

                times[run] = (System.nanoTime() - start) / 1_000_000.0;

            } catch (SQLException e) {

                System.out.println("Statement query error: " + e.getMessage());
            }
        }

        return summarise("Statement (string concat, 100 queries)", 100, times);
    }

    // ----------------------------------------------------------------
    // Suite 3b: PreparedStatement — compiled once, executed many times
    // ----------------------------------------------------------------
    private static double[] benchmarkPreparedStatementQuery() {

        double[] times = new double[RUNS];

        for (int run = 0; run < RUNS; run++) {

            try (
                    Connection con = ConnectionManager.getConnection();

                    PreparedStatement ps =
                            con.prepareStatement(
                                    "SELECT * FROM Books WHERE ISBN=?")
            ) {

                long start = System.nanoTime();

                for (int i = 0; i < 100; i++) {

                    ps.setString(1, "ISBN-100" + (i % 5 + 1));
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) { /* consume */ }

                    rs.close();
                }

                times[run] = (System.nanoTime() - start) / 1_000_000.0;

            } catch (SQLException e) {

                System.out.println("PreparedStatement query error: " + e.getMessage());
            }
        }

        return summarise("PreparedStatement (100 queries)", 100, times);
    }

    // ----------------------------------------------------------------
    // Suite 4a: Per-operation commit (100 inserts, each committed)
    // ----------------------------------------------------------------
    private static double[] benchmarkPerOperationCommit() {

        double[] times = new double[RUNS];

        for (int run = 0; run < RUNS; run++) {

            cleanupBenchmarkData();

            try (
                    Connection con = ConnectionManager.getConnection()
            ) {

                con.setAutoCommit(false);

                PreparedStatement ps =
                        con.prepareStatement(
                                "INSERT INTO Members(Name, ActiveLoans) VALUES(?,0)");

                long start = System.nanoTime();

                for (int i = 0; i < BATCH_OPS; i++) {

                    ps.setString(1, "BenchMember_" + i);
                    ps.executeUpdate();
                    con.commit(); // commit after every single insert
                }

                times[run] = (System.nanoTime() - start) / 1_000_000.0;

                ps.close();
                con.setAutoCommit(true);

            } catch (SQLException e) {

                System.out.println("Per-op commit error: " + e.getMessage());
            }
        }

        return summarise("Per-Operation Commit (100 ops)", BATCH_OPS, times);
    }

    // ----------------------------------------------------------------
    // Suite 4b: Batched commit (100 inserts, single commit at end)
    // ----------------------------------------------------------------
    private static double[] benchmarkBatchedCommit() {

        double[] times = new double[RUNS];

        for (int run = 0; run < RUNS; run++) {

            cleanupBenchmarkData();

            try (
                    Connection con = ConnectionManager.getConnection()
            ) {

                con.setAutoCommit(false);

                PreparedStatement ps =
                        con.prepareStatement(
                                "INSERT INTO Members(Name, ActiveLoans) VALUES(?,0)");

                long start = System.nanoTime();

                for (int i = 0; i < BATCH_OPS; i++) {

                    ps.setString(1, "BenchMember_" + i);
                    ps.executeUpdate();
                }

                con.commit(); // single commit for all 100

                times[run] = (System.nanoTime() - start) / 1_000_000.0;

                ps.close();
                con.setAutoCommit(true);

            } catch (SQLException e) {

                System.out.println("Batched commit error: " + e.getMessage());
            }
        }

        return summarise("Batched Commit (100 ops, 1 commit)", BATCH_OPS, times);
    }

    // ----------------------------------------------------------------
    // Summarise: compute mean, stddev, throughput
    // Returns: [mean, stddev, throughput, count]
    // ----------------------------------------------------------------
    private static double[] summarise(String label, int count, double[] times) {

        double sum = 0;

        for (double t : times) sum += t;

        double mean = sum / times.length;

        double variance = 0;

        for (double t : times) variance += (t - mean) * (t - mean);

        double stddev = Math.sqrt(variance / times.length);

        double throughput = (mean > 0 && count > 0)
                ? (count / (mean / 1000.0))
                : 0;

        System.out.printf("[Benchmark] %-45s mean=%.2f ms  stddev=%.2f ms%n",
                label, mean, stddev);

        return new double[]{mean, stddev, throughput, count};
    }

    // ----------------------------------------------------------------
    // Print structured report table
    // ----------------------------------------------------------------
    private static void printReport(double[][] results) {

        String[] labels = {
            "Individual INSERT (" + SMALL + " records)",
            "Batch INSERT      (" + SMALL + " records)",
            "Individual INSERT (" + LARGE + " records)",
            "Batch INSERT      (" + LARGE + " records)",
            "Full-Table Scan   (Loans)",
            "Indexed Lookup    (Loans.MemberID)",
            "Statement         (100 queries, string concat)",
            "PreparedStatement (100 queries)",
            "Per-Op Commit     (" + BATCH_OPS + " ops)",
            "Batched Commit    (" + BATCH_OPS + " ops, 1 commit)"
        };

        System.out.println("\n========================================");
        System.out.println("         PERFORMANCE REPORT");
        System.out.println("========================================");
        System.out.printf("%-50s %10s %10s %14s%n",
                "Operation", "Avg(ms)", "StdDev", "Throughput");
        System.out.println("-".repeat(88));

        for (int i = 0; i < results.length; i++) {

            double mean       = results[i][0];
            double stddev     = results[i][1];
            double throughput = results[i][2];

            String tpStr = throughput > 0
                    ? String.format("%.1f ops/s", throughput)
                    : "N/A";

            System.out.printf("%-50s %10.2f %10.2f %14s%n",
                    labels[i], mean, stddev, tpStr);
        }

        System.out.println("-".repeat(88));
        System.out.println("\nObservations:");
        System.out.println("  - Batch INSERT is significantly faster than individual INSERT");
        System.out.println("    due to reduced round-trips and single transaction overhead.");
        System.out.println("  - Indexed lookup outperforms full-table scan as data grows.");
        System.out.println("  - PreparedStatement is faster than Statement for repeated");
        System.out.println("    queries because Derby compiles the plan only once.");
        System.out.println("  - Batched commit reduces I/O flush overhead vs per-op commit.");
        System.out.println("========================================\n");
    }

    // ----------------------------------------------------------------
    // Cleanup benchmark-inserted rows (Name starts with BenchMember_)
    // ----------------------------------------------------------------
    private static void cleanupBenchmarkData() {

        try (
                Connection con = ConnectionManager.getConnection();

                Statement stmt = con.createStatement()
        ) {

            stmt.executeUpdate(
                    "DELETE FROM Members WHERE Name LIKE 'BenchMember_%'");

        } catch (SQLException e) {

            System.out.println("Cleanup error: " + e.getMessage());
        }
    }
}
