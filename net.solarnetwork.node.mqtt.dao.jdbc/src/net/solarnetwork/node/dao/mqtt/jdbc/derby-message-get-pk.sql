SELECT id,created,topic,retained,qos,payload
FROM solarnode.mqtt_message
WHERE id = ?