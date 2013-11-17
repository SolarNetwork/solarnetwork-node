INSERT INTO solarnode.sn_power_datum
			(created,source_id, price_loc_id, watts, bat_volts, bat_amp_hrs, dc_out_volts,
			dc_out_amps, ac_out_volts, ac_out_amps, watt_hours, amp_hours)
VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
