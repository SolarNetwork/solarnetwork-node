UPDATE solarnode.mqtt_message
SET topic = ?, retained = ?, qos = ?, payload = ?
WHERE id = ?