# MoeSQL

MoeSQL is a many-objective approach of test database generation problem.   
We developed this framework based on [EvoSQL](https://github.com/SERG-Delft/evosql).  

## Procedure

Import project by Gradle, install Lombok plugin and enable annotation processing.  
The program entrance is located in [Runner.java](./evaluation/src/main/java/nl/tudelft/serg/evosql/evaluation/Runner.java).   
Parameters are dataset(erpnext/espocrm/suitecrm) and algorithm type(evosql/baseline/both/moesql).  

## Experiment data

We provide experimental data of each algorithm running five times in three datasets, which can be found in [scenarios](./evaluation/scenarios).  