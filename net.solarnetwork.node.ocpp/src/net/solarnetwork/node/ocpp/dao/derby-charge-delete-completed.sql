DELETE FROM solarnode.ocpp_charge
WHERE ended IS NOT NULL
	AND ended < ?
