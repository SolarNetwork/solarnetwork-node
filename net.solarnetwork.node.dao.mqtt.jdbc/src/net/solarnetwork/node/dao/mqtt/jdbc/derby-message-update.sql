UPDATE solarnode.mqtt_message
SET destination = ?, topic = ?, retained = ?, qos = ?, payload = ?
WHERE id = ?