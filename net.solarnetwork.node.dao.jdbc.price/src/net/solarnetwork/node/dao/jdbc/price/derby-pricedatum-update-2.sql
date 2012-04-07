DROP INDEX price_datum_upload_created_idx;
DROP INDEX price_datum_upload_track_id_idx;

UPDATE solarnode.sn_settings SET svalue = '2' WHERE skey = 'solarnode.sn_price_datum.version'; 
