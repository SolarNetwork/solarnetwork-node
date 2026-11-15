SELECT to_regclass('solarhist.dh_agg_hourly') IS NOT NULL
	AND to_regclass('solarhist.dh_agg_daily') IS NOT NULL
	AND to_regclass('solarhist.dh_agg_monthly') IS NOT NULL AS agg
