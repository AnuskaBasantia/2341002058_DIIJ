package performance;

public class PerformanceEvaluator {

    public static void benchmark() {

        long start =
                System.nanoTime();

        for(int i = 0; i < 100000; i++) {

            int x = i * i;
        }

        long end =
                System.nanoTime();

        double time =
                (end - start) / 1000000.0;

        System.out.println(
                "Execution Time: "
                        + time + " ms");
    }
}