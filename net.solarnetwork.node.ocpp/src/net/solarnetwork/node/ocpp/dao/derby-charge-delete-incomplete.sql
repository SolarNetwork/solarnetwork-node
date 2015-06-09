DELETE FROM solarnode.ocpp_charge
WHERE ended IS NULL
	AND created < ?
