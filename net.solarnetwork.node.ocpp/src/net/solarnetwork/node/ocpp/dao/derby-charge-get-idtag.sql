SELECT created, sessid_hi, sessid_lo, idtag, socketid, xid, ended
FROM  solarnode.ocpp_charge
WHERE idtag = ?
ORDER BY created DESC
