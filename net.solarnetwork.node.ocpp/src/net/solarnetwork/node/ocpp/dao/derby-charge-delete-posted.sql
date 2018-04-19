DELETE FROM solarnode.ocpp_charge
WHERE posted IS NOT NULL
	AND posted < ?
