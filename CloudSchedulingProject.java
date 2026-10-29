import java.util.*;

/**
 * Joint Optimization of Resource Utilization and Makespan using Genetic Algorithm
 *
 * Standalone Java implementation of the scheduling model in the project report.
 * It simulates CloudSim-style task-to-VM assignment, but does not require the
 * CloudSim library. It implements Round Robin, MCT, Min-Min, Max-Min, GA, PSO,
 * and ABC, and reports makespan, utilization, wastage, and load imbalance.
 *
 * Compile: javac CloudSchedulingProject.java
 * Run:     java CloudSchedulingProject
 */
public class CloudSchedulingProject {
    static final int VM_COUNT = 5;
    static final double[] VM_MIPS = {4000, 2000, 8000, 6000, 2000};
    static final int POPULATION = 50;
    static final int GENERATIONS = 100;
    static final double CROSSOVER_RATE = 0.80;
    static final double MUTATION_RATE = 0.05;
    static final int ELITE_COUNT = 2;
    static final double ALPHA = 0.50;
    static final Random RANDOM = new Random();

    static class Metrics {
        double makespan, utilization, wastage, imbalance;
        Metrics(double m, double u, double w, double i) {
            makespan = m; utilization = u; wastage = w; imbalance = i;
        }
    }

    static class Result {
        String name;
        double makespan, utilization, wastage, imbalance;
        Result(String name, Metrics m) {
            this.name = name;
            makespan = m.makespan;
            utilization = m.utilization;
            wastage = m.wastage;
            imbalance = m.imbalance;
        }
        void add(Metrics m) {
            makespan += m.makespan;
            utilization += m.utilization;
            wastage += m.wastage;
            imbalance += m.imbalance;
        }
        void average(int n) {
            makespan /= n; utilization /= n; wastage /= n; imbalance /= n;
        }
    }

    public static void main(String[] args) {
        int[] workloadSizes = {50, 100, 200};
        String[] names = {"Round Robin", "MCT", "Min-Min", "Max-Min",
                          "Genetic Algorithm", "PSO", "ABC"};

        System.out.println("JOINT OPTIMIZATION OF RESOURCE UTILIZATION AND MAKESPAN");
        System.out.println("VM speeds (MIPS): " + Arrays.toString(VM_MIPS));
        System.out.println("Task lengths: 5,000 to 40,000 MI; 10 runs per workload");
        System.out.println();

        for (int taskCount : workloadSizes) {
            Result[] results = new Result[names.length];
            for (int a = 0; a < names.length; a++)
                results[a] = new Result(names[a], new Metrics(0, 0, 0, 0));

            for (int run = 0; run < 10; run++) {
                // Same task list is used for every algorithm in this run.
                RANDOM.setSeed(1000L + taskCount * 100L + run);
                double[] tasks = new double[taskCount];
                for (int i = 0; i < taskCount; i++)
                    tasks[i] = 5000 + RANDOM.nextInt(35001);

                int[][] schedules = new int[names.length][];
                schedules[0] = roundRobin(taskCount);
                schedules[1] = mct(tasks);
                schedules[2] = minMin(tasks);
                schedules[3] = maxMin(tasks);
                schedules[4] = geneticAlgorithm(tasks);
                schedules[5] = pso(tasks);
                schedules[6] = abc(tasks);

                for (int a = 0; a < names.length; a++)
                    results[a].add(calculateMetrics(tasks, schedules[a]));
            }

            for (Result r : results) r.average(10);
            System.out.println("=== " + taskCount + " TASKS (average of 10 runs) ===");
            System.out.printf("%-22s %12s %13s %12s %14s%n",
                    "Algorithm", "Makespan(s)", "Utilization", "Wastage", "Imbalance");
            for (Result r : results) {
                System.out.printf("%-22s %12.3f %12.2f%% %11.2f%% %14.4f%n",
                        r.name, r.makespan, r.utilization * 100,
                        r.wastage * 100, r.imbalance);
            }
            System.out.println();
        }
    }

    static int[] roundRobin(int n) {
        int[] s = new int[n];
        for (int i = 0; i < n; i++) s[i] = i % VM_COUNT;
        return s;
    }

    // Minimum Completion Time: assign tasks in input order to the VM that
    // would finish earliest if the task were appended to its queue.
    static int[] mct(double[] tasks) {
        int[] s = new int[tasks.length];
        double[] load = new double[VM_COUNT];
        for (int i = 0; i < tasks.length; i++) {
            int best = 0;
            double bestFinish = load[0] + tasks[i] / VM_MIPS[0];
            for (int v = 1; v < VM_COUNT; v++) {
                double finish = load[v] + tasks[i] / VM_MIPS[v];
                if (finish < bestFinish) { bestFinish = finish; best = v; }
            }
            s[i] = best;
            load[best] += tasks[i] / VM_MIPS[best];
        }
        return s;
    }

    static int[] minMin(double[] tasks) { return minMax(tasks, false); }
    static int[] maxMin(double[] tasks) { return minMax(tasks, true); }

    static int[] minMax(double[] tasks, boolean chooseMaximum) {
        int n = tasks.length;
        int[] schedule = new int[n];
        Arrays.fill(schedule, -1);
        boolean[] done = new boolean[n];
        double[] load = new double[VM_COUNT];
        for (int step = 0; step < n; step++) {
            int chosenTask = -1, chosenVm = 0;
            double chosenFinish = chooseMaximum ? -1 : Double.POSITIVE_INFINITY;
            for (int i = 0; i < n; i++) {
                if (done[i]) continue;
                int bestVm = 0;
                double earliest = load[0] + tasks[i] / VM_MIPS[0];
                for (int v = 1; v < VM_COUNT; v++) {
                    double finish = load[v] + tasks[i] / VM_MIPS[v];
                    if (finish < earliest) { earliest = finish; bestVm = v; }
                }
                if ((!chooseMaximum && earliest < chosenFinish) ||
                    (chooseMaximum && earliest > chosenFinish)) {
                    chosenFinish = earliest;
                    chosenTask = i;
                    chosenVm = bestVm;
                }
            }
            schedule[chosenTask] = chosenVm;
            done[chosenTask] = true;
            load[chosenVm] += tasks[chosenTask] / VM_MIPS[chosenVm];
        }
        return schedule;
    }

    static int[] geneticAlgorithm(double[] tasks) {
        int n = tasks.length;
        int[][] population = new int[POPULATION][n];
        for (int p = 0; p < POPULATION; p++)
            for (int i = 0; i < n; i++) population[p][i] = RANDOM.nextInt(VM_COUNT);

        for (int gen = 0; gen < GENERATIONS; gen++) {
            double[] fitness = new double[POPULATION];
            for (int p = 0; p < POPULATION; p++)
                fitness[p] = fitness(tasks, population[p]);

            Integer[] order = new Integer[POPULATION];
            for (int p = 0; p < POPULATION; p++) order[p] = p;
            Arrays.sort(order, Comparator.comparingDouble(p -> fitness[p]));

            int[][] next = new int[POPULATION][n];
            for (int e = 0; e < ELITE_COUNT; e++)
                next[e] = population[order[e]].clone();

            int pos = ELITE_COUNT;
            while (pos < POPULATION) {
                int[] parent1 = tournament(population, fitness);
                int[] parent2 = tournament(population, fitness);
                int[] child1 = parent1.clone(), child2 = parent2.clone();

                if (RANDOM.nextDouble() < CROSSOVER_RATE && n > 2) {
                    int cut1 = RANDOM.nextInt(n);
                    int cut2 = RANDOM.nextInt(n);
                    if (cut1 > cut2) { int t = cut1; cut1 = cut2; cut2 = t; }
                    for (int i = cut1; i <= cut2; i++) {
                        child1[i] = parent2[i];
                        child2[i] = parent1[i];
                    }
                }
                mutate(child1);
                mutate(child2);
                next[pos++] = child1;
                if (pos < POPULATION) next[pos++] = child2;
            }
            population = next;
        }

        int best = 0;
        double bestFit = fitness(tasks, population[0]);
        for (int p = 1; p < POPULATION; p++) {
            double f = fitness(tasks, population[p]);
            if (f < bestFit) { bestFit = f; best = p; }
        }
        return population[best].clone();
    }

    static int[] tournament(int[][] pop, double[] fit) {
        int best = RANDOM.nextInt(pop.length);
        for (int k = 1; k < 3; k++) {
            int candidate = RANDOM.nextInt(pop.length);
            if (fit[candidate] < fit[best]) best = candidate;
        }
        return pop[best].clone();
    }

    static void mutate(int[] chromosome) {
        for (int i = 0; i < chromosome.length; i++)
            if (RANDOM.nextDouble() < MUTATION_RATE)
                chromosome[i] = RANDOM.nextInt(VM_COUNT);
    }

    static double fitness(double[] tasks, int[] schedule) {
        Metrics m = calculateMetrics(tasks, schedule);
        double totalLength = 0;
        for (double task : tasks) totalLength += task;
        double lowerBound = totalLength / Arrays.stream(VM_MIPS).sum();
        return ALPHA * (m.makespan / lowerBound) + (1 - ALPHA) * m.wastage;
    }

    // Discrete PSO: each particle position is a real value per task, mapped
    // to a VM index. Fitness uses the same objective as the GA.
    static int[] pso(double[] tasks) {
        int particles = 50, iterations = 100, n = tasks.length;
        double w = 0.7, c1 = 1.5, c2 = 1.5;
        double[][] pos = new double[particles][n], vel = new double[particles][n];
        double[][] bestPos = new double[particles][n];
        double[] bestFit = new double[particles];
        double[] globalPos = new double[n];
        double globalFit = Double.POSITIVE_INFINITY;

        for (int p = 0; p < particles; p++) {
            for (int i = 0; i < n; i++) {
                pos[p][i] = RANDOM.nextInt(VM_COUNT);
                bestPos[p][i] = pos[p][i];
            }
            bestFit[p] = fitness(tasks, toSchedule(pos[p]));
            if (bestFit[p] < globalFit) {
                globalFit = bestFit[p]; globalPos = pos[p].clone();
            }
        }

        for (int it = 0; it < iterations; it++) {
            for (int p = 0; p < particles; p++) {
                for (int i = 0; i < n; i++) {
                    vel[p][i] = w * vel[p][i]
                            + c1 * RANDOM.nextDouble() * (bestPos[p][i] - pos[p][i])
                            + c2 * RANDOM.nextDouble() * (globalPos[i] - pos[p][i]);
                    pos[p][i] += vel[p][i];
                    pos[p][i] = Math.max(0, Math.min(VM_COUNT - 1, pos[p][i]));
                }
                double f = fitness(tasks, toSchedule(pos[p]));
                if (f < bestFit[p]) {
                    bestFit[p] = f; bestPos[p] = pos[p].clone();
                }
                if (f < globalFit) {
                    globalFit = f; globalPos = pos[p].clone();
                }
            }
        }
        return toSchedule(globalPos);
    }

    static int[] toSchedule(double[] position) {
        int[] s = new int[position.length];
        for (int i = 0; i < s.length; i++)
            s[i] = Math.max(0, Math.min(VM_COUNT - 1, (int) Math.floor(position[i])));
        return s;
    }

    // Basic Artificial Bee Colony: a neighbour changes one task's VM.
    static int[] abc(double[] tasks) {
        int foodCount = 25, cycles = 100, limit = 20, n = tasks.length;
        int[][] food = new int[foodCount][n];
        double[] fit = new double[foodCount];
        int[] trials = new int[foodCount];
        for (int f = 0; f < foodCount; f++) {
            for (int i = 0; i < n; i++) food[f][i] = RANDOM.nextInt(VM_COUNT);
            fit[f] = fitness(tasks, food[f]);
        }

        for (int cycle = 0; cycle < cycles; cycle++) {
            // Employed bees
            for (int f = 0; f < foodCount; f++)
                abcNeighbour(tasks, food, fit, trials, f);
            // Onlooker bees, selected by inverse-fitness roulette.
            double total = 0;
            for (double x : fit) total += 1.0 / (1e-9 + x);
            for (int b = 0; b < foodCount; b++) {
                double pick = RANDOM.nextDouble() * total, sum = 0;
                int chosen = foodCount - 1;
                for (int f = 0; f < foodCount; f++) {
                    sum += 1.0 / (1e-9 + fit[f]);
                    if (sum >= pick) { chosen = f; break; }
                }
                abcNeighbour(tasks, food, fit, trials, chosen);
            }
            // Scout bees replace sources that have not improved for too long.
            for (int f = 0; f < foodCount; f++) {
                if (trials[f] >= limit) {
                    for (int i = 0; i < n; i++) food[f][i] = RANDOM.nextInt(VM_COUNT);
                    fit[f] = fitness(tasks, food[f]);
                    trials[f] = 0;
                }
            }
        }
        int best = 0;
        for (int f = 1; f < foodCount; f++) if (fit[f] < fit[best]) best = f;
        return food[best].clone();
    }

    static void abcNeighbour(double[] tasks, int[][] food, double[] fit,
                             int[] trials, int source) {
        int[] candidate = food[source].clone();
        int task = RANDOM.nextInt(candidate.length);
        int newVm = RANDOM.nextInt(VM_COUNT - 1);
        if (newVm >= candidate[task]) newVm++;
        candidate[task] = newVm;
        double newFit = fitness(tasks, candidate);
        if (newFit < fit[source]) {
            food[source] = candidate;
            fit[source] = newFit;
            trials[source] = 0;
        } else {
            trials[source]++;
        }
    }

    static Metrics calculateMetrics(double[] tasks, int[] schedule) {
        double[] busy = new double[VM_COUNT];
        for (int i = 0; i < tasks.length; i++) {
            int vm = schedule[i];
            busy[vm] += tasks[i] / VM_MIPS[vm];
        }
        double makespan = 0, totalBusy = 0, mean = 0;
        for (double t : busy) {
            makespan = Math.max(makespan, t);
            totalBusy += t;
            mean += t;
        }
        mean /= VM_COUNT;
        double utilization = makespan == 0 ? 0 : totalBusy / (VM_COUNT * makespan);
        double variance = 0;
        for (double t : busy) variance += (t - mean) * (t - mean);
        variance /= VM_COUNT;
        double sd = Math.sqrt(variance);
        double imbalance = mean == 0 ? 0 : sd / mean;
        return new Metrics(makespan, utilization, 1 - utilization, imbalance);
    }
}
