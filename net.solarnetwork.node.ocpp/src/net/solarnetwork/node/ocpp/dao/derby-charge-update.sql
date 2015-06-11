UPDATE solarnode.ocpp_charge
SET auth_status = ?, xid = ?, ended = ?, posted = ?
WHERE sessid_hi = ? AND sessid_lo = ?
