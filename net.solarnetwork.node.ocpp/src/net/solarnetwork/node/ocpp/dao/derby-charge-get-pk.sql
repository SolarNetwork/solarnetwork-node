SELECT created, sessid_hi, sessid_lo, idtag, socketid, xid, ended
FROM  solarnode.ocpp_charge
WHERE sessid_hi = ? AND sessid_lo = ?
