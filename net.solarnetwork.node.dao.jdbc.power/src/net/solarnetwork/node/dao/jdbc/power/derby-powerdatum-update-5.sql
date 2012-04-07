DROP INDEX power_datum_upload_track_id_idx;
DROP INDEX power_datum_upload_created_idx;

UPDATE solarnode.sn_settings SET svalue = '5' WHERE skey = 'solarnode.sn_power_datum.version'; 
