SELECT auth_status, count(*)
FROM  solarnode.ocpp_auth
WHERE expires IS NULL OR expires > CURRENT_TIMESTAMP
GROUP BY auth_status
ORDER BY auth_status
