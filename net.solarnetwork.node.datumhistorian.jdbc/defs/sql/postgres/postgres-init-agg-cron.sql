/* ============================================================================
 * psql script to schedule the JDBC DatumHistorian aggregate maintenance with
 * pg_cron: a job that calls solarhist.dh_process_agg_stale() every minute in
 * the historian database. Run it after postgres-init-agg.sql, once for each
 * historian schema that has the aggregate setup.
 *
 * Nothing else keeps the aggregates up to date: without this job stored datum
 * only add to the solarhist.dh_agg_stale queue. Aggregates lag stored datum by
 * up to a minute plus the processing time.
 *
 * Variables (psql -v name=value):
 *
 *   historian_db      the database with the historian schema (default solarnode)
 *   historian_schema  the historian schema (default solarhist)
 *   schedule          the pg_cron schedule (default '* * * * *', every minute;
 *                     pg_cron also accepts '30 seconds' and the like)
 *   cron_use_socket   true to make the job connect over the Unix socket, see
 *                     below (default true); ignored when pg_cron uses
 *                     background workers
 *
 * For example:
 *
 *   psql -U postgres -d postgres -v historian_db=solarnode -f postgres-init-agg-cron.sql
 *   psql -U postgres -d postgres -v historian_db=solarnode -v historian_schema=solaruhist \
 *       -f postgres-init-agg-cron.sql
 *
 * Running it again replaces the schema's job.
 *
 *
 * Requirements and notes
 * ----------------------
 *
 * 1. pg_cron must be in shared_preload_libraries (a server restart), and the
 *    extension created in its cron.database_name database, which is postgres
 *    by default:
 *
 *        shared_preload_libraries = 'pg_cron'    # postgresql.conf
 *        CREATE EXTENSION pg_cron;               -- in the postgres database
 *
 * 2. pg_cron keeps all its jobs in that one database, so run this script
 *    there, not in the historian database. The job is scheduled into the
 *    historian database with cron.schedule_in_database().
 *
 * 3. Run it as a superuser: pg_cron lets only superusers call
 *    cron.schedule_in_database(), and setting the job's nodename (step 4) is a
 *    direct update of cron.job. The job runs as the role that runs this script;
 *    the yearly solarhist.dh_agg_hourly and solarhist.dh_agg_15m partitions it
 *    creates are still given their table's owner.
 *
 * 4. Job connections. The simplest setup is to have pg_cron run jobs in
 *    background workers, with a restart:
 *
 *        cron.use_background_workers = on        # postgresql.conf
 *
 *    A job then makes no client connection, so pg_hba.conf plays no part and
 *    this script leaves the job's nodename alone. Each running job takes one of
 *    the max_worker_processes slots; in this mode cron.max_running_jobs
 *    defaults to 5 and can be at most max_worker_processes - 1.
 *
 *    Otherwise pg_cron runs each job over an ordinary client connection to
 *    cron.host, which is localhost. On a stock Debian cluster pg_hba.conf asks
 *    for a password on both loopback addresses, so every run fails with
 *    "connection failed" and an error in cron.job_run_details. With
 *    cron_use_socket (the default) this script then sets the job's nodename to
 *    an empty string, which makes pg_cron connect over the Unix socket in
 *    libpq's default directory, where Debian's "local all postgres peer" line
 *    lets the postgres role in. Setting cron.host to the socket directory (for
 *    example /var/run/postgresql), with a restart, does the same for jobs
 *    scheduled after that, as pg_cron copies cron.host into each new job's
 *    nodename. A job run as another role needs a pg_hba.conf "local" line that
 *    lets the server's OS user connect as that role.
 *
 * 5. A job does not overlap itself: if a run takes longer than the schedule
 *    interval, the next starts when it ends. solarhist.dh_process_agg_stale()
 *    also refuses to run beside another call of itself.
 *
 * 6. Every run adds a row to cron.job_run_details, 1,440 a day for this job.
 *    Either set cron.log_run = off, or schedule a cleanup, for example:
 *
 *        SELECT cron.schedule('cron-job-run-details-cleanup', '17 3 * * *',
 *            $$DELETE FROM cron.job_run_details WHERE end_time < now() - INTERVAL 'P7D'$$);
 *
 * 7. Set jit = off for the historian database or role, as JIT compilation
 *    costs far more than it saves in these functions:
 *
 *        ALTER DATABASE solarnode SET jit = off;
 *
 * To check on the job:
 *
 *   SELECT * FROM cron.job;
 *   SELECT * FROM cron.job_run_details ORDER BY start_time DESC LIMIT 10;
 *
 * To remove it:
 *
 *   SELECT cron.unschedule('solarnode-agg-solarhist');
 * ============================================================================
 */

\set ON_ERROR_STOP on

\if :{?historian_db}
\else
	\set historian_db solarnode
\endif
\if :{?historian_schema}
\else
	\set historian_schema solarhist
\endif
\if :{?schedule}
\else
	\set schedule '* * * * *'
\endif
\if :{?cron_use_socket}
\else
	\set cron_use_socket true
\endif

\set job_name solarnode-agg- :historian_schema

DO $$
BEGIN
	IF current_setting('cron.database_name', TRUE) IS NULL THEN
		RAISE EXCEPTION 'pg_cron is not loaded.'
		USING HINT = 'Add pg_cron to shared_preload_libraries and restart the server.';
	ELSIF current_setting('cron.database_name') <> current_database() THEN
		RAISE EXCEPTION 'Run this script in the % database, the cron.database_name setting.',
			current_setting('cron.database_name');
	END IF;
END
$$;

-- replace any existing job for the schema
SELECT count(cron.unschedule(jobid)) AS removed_jobs
FROM cron.job
WHERE jobname = :'job_name';

SELECT cron.schedule_in_database(
	  :'job_name'
	, :'schedule'
	, format('CALL %I.dh_process_agg_stale()', :'historian_schema')
	, :'historian_db'
) AS jobid \gset

SELECT COALESCE(current_setting('cron.use_background_workers', TRUE), 'off') AS cron_background_workers \gset

\if :cron_background_workers
-- a job run in a background worker makes no connection, so its nodename is not used
\elif :cron_use_socket
-- connect over the Unix socket rather than to cron.host
UPDATE cron.job SET nodename = '' WHERE jobid = :jobid;
\endif

SELECT jobid, jobname, schedule, command, database, username, nodename
FROM cron.job
WHERE jobid = :jobid;
