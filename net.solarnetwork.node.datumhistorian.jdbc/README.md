# SolarNode JDBC DatumHistorian

This plugin provides a JDBC-based implementation of the `DatumHistorian` API, that also supports the
`FilterableDatumHistorian` API that enables the SolarQuery-like API support in the SolarNode REST API.

## Aggregates

With the optional `postgres-init-agg.sql` script, the historian also stores `FifteenMinute`,
`Hour`, `Day`, and `Month` aggregates of every stream, computed by the `pg_cron` job of
`postgres-init-agg-cron.sql` from a queue of the hours that stored datum have changed. Aggregate
support is chosen when the schema is set up: run the script then or not at all.

- The hourly table, `dh_agg_hourly`, holds one row for each stream and UTC hour that has datum.
- The 15 minute table, `dh_agg_15m`, holds one to four rows for each row of `dh_agg_hourly`: one for
  each quarter of that UTC hour that holds a datum or is given part of the hour's accumulation or
  readings. The job computes them with their hour, from the same datum, so they add up to the hour.
  They are stored, changed, and deleted only with their hour.
- Both tables are partitioned by UTC year and manage their own partitions: the job creates the
  partition of a year when it stores the first hour in it, which for a late datum can be a past
  year. The raw `dh_datum` table is partitioned by UTC month in the same way, as datum are stored.
- A new partition is given its table's owner, `solarnode` in the setup below, although the job that
  creates it runs as `postgres`, and is created in its table's tablespace (see
  [Tablespace](#tablespace)).
- No partition is ever dropped or detached automatically, so the historian never prunes the hourly
  or 15 minute aggregates. The daily and monthly tables are small enough not to be partitioned.

The aggregations of the SolarQuery-like API are provided from these tables:

| Aggregation | Provided by | Reading statistics | Date range |
|:------------|:------------|:-------------------|:-----------|
| `FifteenMinute`, `Hour`, `Day`, `Month` | Reading the stored aggregates | Yes | Optional, any span |
| `ThirtyMinute` | Combining the two 15 minute rows of each UTC half hour when queried | Yes | Optional, any span |
| `FiveMinute`, `TenMinute` | Computing the slots from raw datum when queried | No | Required, at most `maxMinuteAggregationHours` (840 by default) |

- The 15 and 30 minute aggregates start on the quarter and half hours of UTC time. The 5 and 10
  minute slots are UTC-aligned too, and follow the hourly rollup's rules: nothing is counted across a
  gap longer than an hour, a difference across an hour is split at the hour, and a slot is returned
  when it starts within the date range, computed from all its datum.
- A `ThirtyMinute` query finds and sorts the half hours of its whole date range, and then combines
  only those of the page asked for. On a development machine, a page of 1,000 half hours from a year
  of 20 streams took about 0.25 seconds, and the whole year about 3.5 seconds, with at most 41 MB of
  temporary files.
- The most recent aggregates can be queried for `FifteenMinute`, `Hour`, `Day`, and `Month`, but not
  for `ThirtyMinute`, `FiveMinute`, or `TenMinute`.
- `Difference` reading queries accept `FifteenMinute`, `Hour`, `Day`, and `Month`, and refuse the
  other sub-hour aggregations.
- Without the aggregate setup, all aggregations are treated as no aggregation in list queries, and
  the sub-hour aggregations are refused in reading queries.

## Postgres setup

This plugin needs the Postgres tables and functions created by the SQL scripts in the
[`defs/sql/postgres`](./defs/sql/postgres) directory, and they must be run before installing the plugin.
The scripts are not included in the plugin itself, so copy them to the node first: the commands below
run them from the current directory.

The commands assume you are using SolarNodeOS and Postgres 17 is already installed, with the
`solarnode` role and database that SolarNode connects to with a
`/etc/solarnode/services/net.solarnetwork.jdbc.pool.hikari-solarnode-postgres.cfg` file.

Assuming all defaults and using the `solarnode` database to host the historian tables, and
aggregation support is desired, the process looks like this:

```sh
# install the necessary support packages
sudo apt install sn-postgresql-17-aggs-for-vecs postgresql-17-cron

# add pg_cron to shared_preload_libraries (if not already there)
sudo sed -Ei "/^#?shared_preload_libraries\s*=/{/pg_cron/b;s/^#?(shared_preload_libraries\s*=\s*')([^']*)'/\1\2,pg_cron'/;s/',pg_cron'/'pg_cron'/}" \
/etc/postgresql/17/main/postgresql.conf

# add historian settings to own config
sudo tee /etc/postgresql/17/main/conf.d/solarnode-historian.conf <<'EOF'
max_worker_processes = 16
cron.use_background_workers = on
EOF

# restart Postgres, to load pg_cron and the settings above
sudo systemctl restart postgresql@17-main

# create the aggs_for_vecs extension in the solarnode database, and turn off JIT compilation
sudo -u postgres psql -d solarnode -c 'CREATE EXTENSION IF NOT EXISTS aggs_for_vecs'
sudo -u postgres psql -d solarnode -c 'ALTER DATABASE solarnode SET jit = off'

# create solarhist schema; ON_ERROR_STOP and -1 (a single transaction) roll back everything if
# any statement fails, so a script can be run again after fixing the problem
psql -h localhost -U solarnode -d solarnode -v ON_ERROR_STOP=1 -1 -f postgres-init.sql
psql -h localhost -U solarnode -d solarnode -v ON_ERROR_STOP=1 -1 -f postgres-init-agg.sql

# create solaruhist schema
sed 's/\bsolarhist/solaruhist/g' postgres-init.sql |psql -h localhost -U solarnode -d solarnode -v ON_ERROR_STOP=1 -1
sed 's/\bsolarhist/solaruhist/g' postgres-init-agg.sql |psql -h localhost -U solarnode -d solarnode -v ON_ERROR_STOP=1 -1

# add pg_cron extension to the postgres database
sudo -u postgres psql -c 'CREATE EXTENSION IF NOT EXISTS pg_cron'

# add maintenance for solarhist; the script is read from standard input, as the postgres user
# might not be allowed to read it from the current directory
sudo -u postgres psql -v historian_db=solarnode <postgres-init-agg-cron.sql

# add maintenance for solaruhist
sudo -u postgres psql -v historian_db=solarnode -v historian_schema=solaruhist <postgres-init-agg-cron.sql

# schedule a job to clean out old job run records older than 7d
sudo -u postgres psql -c "SELECT cron.schedule('cron-job-run-details-cleanup', '30 3 * * *',\$\$DELETE FROM cron.job_run_details WHERE end_time < now() - INTERVAL 'P7D'\$\$);"

# let the historian use the SolarNode Postgres connection pool, by adding node-datum-history to
# its db service property (if not already there)
sudo sed -Ei '/^serviceProperty\.db\s*=/{/node-datum-history/b;s/$/, node-datum-history/}' \
/etc/solarnode/services/net.solarnetwork.jdbc.pool.hikari-solarnode-postgres.cfg
```

### Tablespace

The history schemas can be kept in a tablespace of their own, for example on an attached drive.

Create the tablespace first, let the `solarnode` role create tables in it, and run the init
scripts with `default_tablespace` set to it. New partitions are then created in the same
tablespace, as each partitioned table has it.

### Recreate schemas

If you ever would like to drop/recreate the historian schemas, you can do the following,
assuming you run as a database superuser:

```sh
{ echo 'DROP SCHEMA solarhist CASCADE; DROP SCHEMA solaruhist CASCADE; SET ROLE solarnode;'; cat postgres-init.sql postgres-init-agg.sql; sed 's/\bsolarhist/solaruhist/g' postgres-init.sql postgres-init-agg.sql; } | psql -U postgres -d solarnode -X -q -v ON_ERROR_STOP=1 -1 -f -
```

