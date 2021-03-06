# Inclusive start index
$env:COVERAGE_EXPERIMENT_START = 0;
# Step size
$env:COVERAGE_EXPERIMENT_STEP = 1;
# Exclusive end index (uncomment to enable)
# $env:COVERAGE_EXPERIMENT_STOP = 10;

# Run the experiment
docker-compose up --build --abort-on-container-exit --force-recreate;

# Copy results
docker cp "$(docker-compose ps -q app):/experiment-output.tar.gz" experiment-output.tar.gz
