SELECT to_regclass('solarhist.dh_agg_15m') IS NOT NULL
	AND to_regclass('solarhist.dh_agg_hourly') IS NOT NULL
	AND to_regclass('solarhist.dh_agg_daily') IS NOT NULL
	AND to_regclass('solarhist.dh_agg_monthly') IS NOT NULL AS agg
	, to_regprocedure('solarhist.dh_rollup_datum_for_time_span_slots(integer,timestamp with time zone,timestamp with time zone,integer,interval)') IS NOT NULL
	AND to_regprocedure('solarhist.dh_count_datum_time_span_slots(integer,timestamp with time zone,timestamp with time zone,integer)') IS NOT NULL AS agg_slots
