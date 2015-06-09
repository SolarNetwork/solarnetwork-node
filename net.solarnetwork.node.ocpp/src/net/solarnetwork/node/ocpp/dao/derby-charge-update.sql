UPDATE solarnode.ocpp_charge
SET xid = ?, ended = ?
WHERE sessid_hi = ? AND sessid_lo = ?
