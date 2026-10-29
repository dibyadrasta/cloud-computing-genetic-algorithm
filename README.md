
# Cloud Computing Project: Genetic Algorithm Scheduling

## Overview
A Java implementation of cloud task scheduling that aims to optimize makespan and resource utilization across heterogeneous virtual machines.

## Algorithms
- Round Robin
- Minimum Completion Time (MCT)
- Min-Min
- Max-Min
- Genetic Algorithm (GA)
- Particle Swarm Optimization (PSO)
- Artificial Bee Colony (ABC)

## Configuration
- Number of VMs: 5
- VM speeds: 4000, 2000, 8000, 6000, 2000 MIPS
- Workload sizes: 50, 100, 200 tasks
- Task lengths: 5000–40000 MI

## Technologies
- Java
- Cloud Computing
- Task Scheduling
- Genetic Algorithm

## How to Run
Compile:

```bash
javac CloudSchedulingProject.java
```

Run:

```bash
java CloudSchedulingProject
```

## Performance Metrics
- Makespan
- Resource utilization
- Resource wastage
- Load imbalance

## Note
The current Java implementation is a standalone scheduling simulation, not yet integrated with actual CloudSim entities.
