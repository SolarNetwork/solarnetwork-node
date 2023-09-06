SELECT id,created,'' AS tok_sec,disp_name,description
FROM solarnode.sn_sectok
ORDER BY LOWER(id),created