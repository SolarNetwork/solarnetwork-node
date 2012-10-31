DROP INDEX consum_datum_upload_created_idx;
DROP INDEX consum_datum_upload_track_id_idx;

UPDATE solarnode.sn_settings SET svalue = '4' WHERE skey = 'solarnode.sn_consum_datum.version'; 
