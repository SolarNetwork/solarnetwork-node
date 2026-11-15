# SolarNode JDBC DatumHistorian

This plugin provide a JDBC-based implementation of the `DatumHistorian` API, that also supports the
`FilterableDatumHistorian` API that enables the SolarQuery-like API support in the SolarNode REST API.

## Postgres setup

The `net.solarnetwork.node.datumhistorian.jdbc` plugin provides Postgres DDL scripts that must
be installed prior to installing this package. Assuming all defaults and using the `solarnode`
database to host the historian tables, and aggregation support is desired, the process looks like
this:

```sh
# install the necessary support packages
apt install sn-postgresql-17-aggs-for-vecs postgresql-17-cron

# add pg_cron to shared_preload_libraries (if not already there)
sed -Ei "/^#?shared_preload_libraries\s*=/{/pg_cron/b;s/^#?(shared_preload_libraries\s*=\s*')([^']*)'/\1\2,pg_cron'/;s/',pg_cron'/'pg_cron'/}" \
/etc/postgresql/17/main/postgresql.conf

# add historian settings to own config
sudo tee /etc/postgresql/17/main/conf.d/solarnode-historian.conf <<'EOF'
max_worker_processes = 16
cron.use_background_workers = on
EOF

# create the aggs_for_vecs extension in the solarnode database
sudo -u postgres psql -d solarnode -c 'CREATE EXTENSION IF NOT EXISTS aggs_for_vecs'

# create solarhist schema
psql -h localhost -U solarnode -d solarnode -f postgres-init.sql
psql -h localhost -U solarnode -d solarnode -f postgres-init-agg.sql

# create solaruhist schema
sed 's/\bsolarhist/solaruhist/g' postgres-init.sql |psql -h localhost -U solarnode -d solarnode
sed 's/\bsolarhist/solaruhist/g' postgres-init-agg.sql |psql -h localhost -U solarnode -d solarnode

# add pg_cron extension to the postgres database
sudo -u postgres psql -c 'CREATE EXTENSION IF NOT EXISTS pg_cron'

# add maintenance for solarhist
sudo -u postgres psql -v historian_db=solarnode -f postgres-init-agg-cron.sql

# add maintenance for solaruhist
sudo -u postgres psql -v historian_db=solarnode -v historian_schema=solaruhist -f postgres-init-agg-cron.sql

# schedule a job to clean out old job run records older than 7d
sudo -u postgres psql -c "SELECT cron.schedule('cron-job-run-details-cleanup', '30 3 * * *',\$\$DELETE FROM cron.job_run_details WHERE end_time < now() - INTERVAL 'P7D'\$\$);"
```

