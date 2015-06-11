SELECT created, sessid_hi, sessid_lo, idtag, socketid, auth_status, xid, ended, posted
FROM  solarnode.ocpp_charge
WHERE xid IS NULL OR (ended IS NOT NULL AND posted IS NULL)
ORDER BY created
