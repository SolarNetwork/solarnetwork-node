SELECT id,created,destination,topic,retained,qos,payload
FROM solarnode.mqtt_message
WHERE destination = ?