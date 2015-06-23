SELECT created, idtag, parent_idtag, auth_status, expires
FROM  solarnode.ocpp_auth
WHERE idtag = ?
