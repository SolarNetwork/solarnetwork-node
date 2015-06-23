SELECT created, sessid_hi, sessid_lo, idtag, socketid, auth_status, xid, ended, posted
FROM  solarnode.ocpp_charge
WHERE xid = ? AND ended IS NULL
ORDER BY created DESC
