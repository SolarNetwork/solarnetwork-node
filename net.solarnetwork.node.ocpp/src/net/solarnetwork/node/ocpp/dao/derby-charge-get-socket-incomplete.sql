SELECT created, sessid_hi, sessid_lo, idtag, socketid, auth_status, xid, ended
FROM  solarnode.ocpp_charge
WHERE socketid = ? AND ended IS NULL
ORDER BY created DESC
